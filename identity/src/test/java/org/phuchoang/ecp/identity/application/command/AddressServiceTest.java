package org.phuchoang.ecp.identity.application.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.application.port.AddressRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.domain.CustomerAddress;
import org.phuchoang.ecp.identity.domain.RoleCode;
import org.phuchoang.ecp.sharedkernel.api.Address;
import org.phuchoang.ecp.sharedkernel.api.DomainException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — `UC-CUS-09` (`US-CUS-09`): `BR-CUS-05` (exactly one default) and ownership → `404`. */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private final UUID accountId = UUID.randomUUID();

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private AuthorizationService authorizationService;

    private AddressService service() {
        return new AddressService(addressRepository, authorizationService);
    }

    private static Address anAddress() {
        return new Address(null, "Jane Doe", "1 Main St", null, "Springfield", null, "12345", "US", null);
    }

    private CallerContext caller() {
        return new CallerContext(accountId, Set.of(RoleCode.CUSTOMER));
    }

    @Test
    void theFirstAddressAddedBecomesTheDefaultShippingAddress_BR_CUS_05() {
        when(addressRepository.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = service().addOwnAddress(caller(), new AddressCommand(anAddress(), false, false));

        assertThat(summary.isDefaultShipping()).isTrue();
    }

    @Test
    void theFirstAddressDoesNotAutoBecomeTheDefaultBillingAddress() {
        // BR-CUS-05 governs default *shipping* only — nothing in the spec extends the
        // first-address auto-default to billing, which has no documented business rule.
        when(addressRepository.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = service().addOwnAddress(caller(), new AddressCommand(anAddress(), false, false));

        assertThat(summary.isDefaultBilling()).isFalse();
    }

    @Test
    void isDefaultBillingIsStoredExactlyAsRequestedEvenForTheFirstAddress() {
        when(addressRepository.existsAnyForAccount(accountId)).thenReturn(false);
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressSummary summary = service().addOwnAddress(caller(), new AddressCommand(anAddress(), false, true));

        assertThat(summary.isDefaultBilling()).isTrue();
    }

    @Test
    void nominatingANewDefaultClearsThePreviousOne_BR_CUS_05() {
        when(addressRepository.existsAnyForAccount(accountId)).thenReturn(true);
        CustomerAddress existingDefault = CustomerAddress.add(UUID.randomUUID(), accountId, anAddress(), true, false);
        when(addressRepository.findAllByAccountId(accountId)).thenReturn(List.of(existingDefault));
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().addOwnAddress(caller(), new AddressCommand(anAddress(), true, false));

        assertThat(existingDefault.defaultShipping()).isFalse();
        verify(addressRepository).save(existingDefault);
    }

    @Test
    void gettingAnotherCustomersAddressIsNotFoundNeverForbidden_Integration_Contract_2_1() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress othersAddress = CustomerAddress.add(addressId, UUID.randomUUID(), anAddress(), false, false);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(othersAddress));

        Throwable thrown = catchThrowable(() -> service().getOwnAddress(caller(), addressId));

        assertThat(thrown).isInstanceOf(DomainException.class);
    }

    @Test
    void removingAnAlreadyAbsentAddressIsIdempotent() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        service().removeOwnAddress(caller(), addressId);

        verify(addressRepository, never()).delete(any());
    }

    @Test
    void removingAnotherCustomersAddressDoesNothing() {
        UUID addressId = UUID.randomUUID();
        CustomerAddress othersAddress = CustomerAddress.add(addressId, UUID.randomUUID(), anAddress(), false, false);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(othersAddress));

        service().removeOwnAddress(caller(), addressId);

        verify(addressRepository, never()).delete(any());
    }
}
