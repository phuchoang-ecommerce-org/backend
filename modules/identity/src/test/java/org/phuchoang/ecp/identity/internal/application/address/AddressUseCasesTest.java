package org.phuchoang.ecp.identity.internal.application.address;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.port.AddressStore;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.sharedkernel.api.address.Address;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.HmacCursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — `UC-CUS-09` (`US-CUS-09`): `BR-CUS-05` (exactly one default) and ownership → `404`. */
@ExtendWith(MockitoExtension.class)
class AddressUseCasesTest {

    private final UUID accountId = UUID.randomUUID();

    @Mock
    private AddressStore addressStore;
    @Mock
    private PermissionChecker permissions;

    private AddressUseCases useCases() {
        return new AddressUseCases(addressStore, permissions, codec());
    }

    private static Address anAddress() {
        return new Address(null, "Jane Doe", "1 Main St", null, "Springfield", null, "12345", "US", null);
    }

    private CallerContext caller() {
        return new CallerContext(accountId, Set.of(RoleCode.CUSTOMER));
    }

    @Test
    void theFirstAddressAddedBecomesTheDefaultShippingAddress_BR_CUS_05() {
        when(addressStore.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = useCases().addOwnAddress(caller(), new AddressCommand(anAddress(), false, false));

        assertThat(summary.isDefaultShipping()).isTrue();
    }

    @Test
    void theFirstAddressDoesNotAutoBecomeTheDefaultBillingAddress() {
        // BR-CUS-05 governs default *shipping* only — nothing in the spec extends the
        // first-address auto-default to billing, which has no documented business rule.
        when(addressStore.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = useCases().addOwnAddress(caller(), new AddressCommand(anAddress(), false, false));

        assertThat(summary.isDefaultBilling()).isFalse();
        verify(addressStore, never()).clearDefaultBilling(any());
    }

    @Test
    void isDefaultBillingIsStoredExactlyAsRequestedEvenForTheFirstAddress() {
        when(addressStore.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = useCases().addOwnAddress(caller(), new AddressCommand(anAddress(), false, true));

        assertThat(summary.isDefaultBilling()).isTrue();
        verify(addressStore).clearDefaultBilling(accountId);
    }

    @Test
    void nominatingANewDefaultClearsThePreviousOneInOneUpdate_BR_CUS_05() {
        when(addressStore.existsAnyForAccount(accountId)).thenReturn(true);
        when(addressStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCases().addOwnAddress(caller(), new AddressCommand(anAddress(), true, false));

        var order = org.mockito.Mockito.inOrder(addressStore);
        order.verify(addressStore).clearDefaultShipping(accountId);
        ArgumentCaptor<CustomerAddress> saved = ArgumentCaptor.forClass(CustomerAddress.class);
        order.verify(addressStore).save(saved.capture());
        assertThat(saved.getValue().defaultShipping()).isTrue();
    }

    @Test
    void replacingAnAddressThatIsAlreadyTheDefaultDoesNotClearItself() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress current = CustomerAddress.add(addressId, accountId, anAddress(), true, false);
        when(addressStore.findById(addressId)).thenReturn(Optional.of(current));
        when(addressStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = useCases().replaceOwnAddress(caller(), addressId, new AddressCommand(anAddress(), true, true));

        verify(addressStore, never()).clearDefaultShipping(any());
        verify(addressStore).clearDefaultBilling(accountId);
        assertThat(summary.isDefaultShipping()).isTrue();
        assertThat(summary.isDefaultBilling()).isTrue();
    }

    @Test
    void gettingAnotherCustomersAddressIsNotFoundNeverForbidden_Integration_Contract_2_1() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress othersAddress = CustomerAddress.add(addressId, UUID.randomUUID(), anAddress(), false, false);
        when(addressStore.findById(addressId)).thenReturn(Optional.of(othersAddress));

        Throwable thrown = catchThrowable(() -> useCases().getOwnAddress(caller(), addressId));

        assertThat(thrown).isInstanceOf(DomainException.class)
            .satisfies(e -> assertThat(((DomainException) e).errorCode().code()).isEqualTo("ECP-GEN-4040"));
    }

    @Test
    void removingAnAlreadyAbsentAddressIsIdempotent() {
        UUID addressId = UUID.randomUUID();
        when(addressStore.findById(addressId)).thenReturn(Optional.empty());

        useCases().removeOwnAddress(caller(), addressId);

        verify(addressStore, never()).delete(any());
    }

    @Test
    void removingAnotherCustomersAddressDoesNothing() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress othersAddress = CustomerAddress.add(addressId, UUID.randomUUID(), anAddress(), false, false);
        when(addressStore.findById(addressId)).thenReturn(Optional.of(othersAddress));

        useCases().removeOwnAddress(caller(), addressId);

        verify(addressStore, never()).delete(any());
    }

    @Test
    void addressListingUsesSignedAccountBoundLookAheadCursor() {
        CustomerAddress first = CustomerAddress.add(UUID.randomUUID(), accountId, anAddress(), true, false);
        CustomerAddress second = CustomerAddress.add(UUID.randomUUID(), accountId, anAddress(), false, false);
        CustomerAddress lookAhead = CustomerAddress.add(UUID.randomUUID(), accountId, anAddress(), false, false);
        AddressStore.Cursor secondCursor = new AddressStore.Cursor(Instant.parse("2026-09-14T10:00:00Z"), second.id());
        when(addressStore.findByAccountId(accountId, null, 3)).thenReturn(List.of(first, second, lookAhead));
        when(addressStore.cursorOf(second)).thenReturn(secondCursor);

        var page = useCases().listOwnAddresses(caller(), null, 2);

        assertThat(page.items()).hasSize(2);
        assertThat(page.nextCursor()).isNotBlank();
        assertThat(codec().decode(page.nextCursor(), context(accountId)).sortValues())
            .containsExactly(CursorValue.instant(secondCursor.createdAt()));
        verify(addressStore).findByAccountId(accountId, null, 3);
    }

    @Test
    void addressCursorCannotBeReplayedAgainstAnotherAccount() {
        UUID otherAccount = UUID.randomUUID();
        String cursor = codec().encode(context(accountId),
            List.of(CursorValue.instant(Instant.parse("2026-09-14T10:00:00Z"))), UUID.randomUUID());

        assertThat(catchThrowable(() -> useCases().listOwnAddresses(
            new CallerContext(otherAccount, Set.of(RoleCode.CUSTOMER)), cursor, 2)))
            .isInstanceOf(InvalidCursorException.class);
        verify(addressStore, never()).findByAccountId(eq(otherAccount), any(), anyInt());
    }

    private static CursorCodec codec() {
        return new HmacCursorCodec(CursorSigningKey.utf8("test-key", "01234567890123456789012345678901"), null);
    }

    private static CursorContext context(UUID accountId) {
        return new CursorContext("identity.own-addresses", accountId.toString(), "createdAt:desc", Map.of());
    }
}
