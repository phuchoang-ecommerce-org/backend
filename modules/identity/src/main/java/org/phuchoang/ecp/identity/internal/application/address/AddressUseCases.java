package org.phuchoang.ecp.identity.internal.application.address;

import org.phuchoang.ecp.identity.internal.application.IdentityErrors;
import org.phuchoang.ecp.identity.internal.application.port.AddressStore;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.phuchoang.ecp.identity.internal.application.security.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * `UC-CUS-09` — Manage Shipping Addresses (`US-CUS-09`): {@code listOwnAddresses},
 * {@code addOwnAddress}, {@code getOwnAddress}, {@code replaceOwnAddress}, {@code removeOwnAddress}.
 *
 * <p>Ownership mismatch is reported as {@code NOT_FOUND}, thrown directly here — never a second
 * call to the permission checker, and never {@code FORBIDDEN} — per
 * {@code identity.api.IdentityAuthorization}'s contract (`Integration Contract.md` §2.1).
 *
 * <p>Default-address transitions (`BR-CUS-05`) are one bulk update per flag on {@link AddressStore}
 * inside the same transaction as the save; the partial unique indexes
 * {@code ux_identity_address_default_shipping/billing} enforce the invariant under concurrency.
 */
@Service
public class AddressUseCases {

    private final AddressStore addresses;
    private final PermissionChecker permissions;
    private final CursorCodec cursorCodec;

    public AddressUseCases(AddressStore addresses, PermissionChecker permissions, CursorCodec cursorCodec) {
        this.addresses = addresses;
        this.permissions = permissions;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public AddressPageResult listOwnAddresses(CallerContext caller, String cursor, int size) {
        permissions.require(caller, PermissionMatrix.LIST_OWN_ADDRESSES);

        AddressStore.Cursor after = decodeCursor(caller.accountId(), cursor);
        List<CustomerAddress> rows = addresses.findByAccountId(caller.accountId(), after, size + 1);
        boolean hasMore = rows.size() > size;
        List<CustomerAddress> page = hasMore ? rows.subList(0, size) : rows;
        String nextCursor = hasMore ? encodeCursor(caller.accountId(), addresses.cursorOf(page.getLast())) : null;
        return new AddressPageResult(page.stream().map(AddressSummary::of).toList(), nextCursor);
    }

    @Transactional
    public AddressSummary addOwnAddress(CallerContext caller, AddressCommand command) {
        permissions.require(caller, PermissionMatrix.ADD_OWN_ADDRESS);

        // BR-CUS-05 ("at most one default shipping address") governs *shipping* only — the first
        // address in the book becomes the default shipping address (main scenario step 4). Nothing
        // in the spec extends this to isDefaultBilling, so it is stored exactly as requested.
        boolean isFirst = !addresses.existsAnyForAccount(caller.accountId());
        boolean defaultShipping = command.isDefaultShipping() || isFirst;
        boolean defaultBilling = command.isDefaultBilling();

        if (defaultShipping) {
            addresses.clearDefaultShipping(caller.accountId());
        }
        if (defaultBilling) {
            addresses.clearDefaultBilling(caller.accountId());
        }

        CustomerAddress address = CustomerAddress.add(UUID.randomUUID(), caller.accountId(), command.address(),
            defaultShipping, defaultBilling);
        return AddressSummary.of(addresses.save(address));
    }

    @Transactional(readOnly = true)
    public AddressSummary getOwnAddress(CallerContext caller, UUID addressId) {
        permissions.require(caller, PermissionMatrix.GET_OWN_ADDRESS);
        return AddressSummary.of(ownedAddressOrNotFound(caller.accountId(), addressId));
    }

    @Transactional
    public AddressSummary replaceOwnAddress(CallerContext caller, UUID addressId, AddressCommand command) {
        permissions.require(caller, PermissionMatrix.REPLACE_OWN_ADDRESS);
        CustomerAddress address = ownedAddressOrNotFound(caller.accountId(), addressId);

        if (command.isDefaultShipping() && !address.defaultShipping()) {
            addresses.clearDefaultShipping(caller.accountId());
        }
        if (command.isDefaultBilling() && !address.defaultBilling()) {
            addresses.clearDefaultBilling(caller.accountId());
        }

        address.replace(command.address(), command.isDefaultShipping(), command.isDefaultBilling());
        return AddressSummary.of(addresses.save(address));
    }

    /** Idempotent — removing an address already gone (or another customer's) is 204, not 404. */
    @Transactional
    public void removeOwnAddress(CallerContext caller, UUID addressId) {
        permissions.require(caller, PermissionMatrix.REMOVE_OWN_ADDRESS);
        addresses.findById(addressId)
            .filter(address -> address.isOwnedBy(caller.accountId()))
            .ifPresent(address -> addresses.delete(address.id()));
    }

    private CustomerAddress ownedAddressOrNotFound(UUID accountId, UUID addressId) {
        return addresses.findById(addressId)
            .filter(address -> address.isOwnedBy(accountId))
            .orElseThrow(IdentityErrors::addressNotFound);
    }

    private AddressStore.Cursor decodeCursor(UUID accountId, String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        CursorPosition position = cursorCodec.decode(cursor, cursorContext(accountId));
        if (position.sortValues().size() != 1 || position.sortValues().getFirst().type() != CursorValue.Type.INSTANT) {
            throw new InvalidCursorException();
        }
        return new AddressStore.Cursor(position.sortValues().getFirst().instantValue(), position.tieBreaker());
    }

    private String encodeCursor(UUID accountId, AddressStore.Cursor cursor) {
        return cursorCodec.encode(cursorContext(accountId), List.of(CursorValue.instant(cursor.createdAt())), cursor.id());
    }

    private static CursorContext cursorContext(UUID accountId) {
        return new CursorContext("identity.own-addresses", accountId.toString(), "createdAt:desc", Map.of());
    }
}
