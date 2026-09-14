package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.application.port.AddressRepository;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.query.AddressPageResult;
import org.phuchoang.ecp.identity.domain.CustomerAddress;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.InvalidCursorException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * `UC-CUS-09` — Manage Shipping Addresses (`US-CUS-09`): {@code listOwnAddresses},
 * {@code addOwnAddress}, {@code getOwnAddress}, {@code replaceOwnAddress}, {@code removeOwnAddress}.
 *
 * <p>Ownership mismatch is reported as {@code NOT_FOUND}, thrown directly here — never a second
 * call to {@code AuthorizationService}, and never {@code FORBIDDEN} — per that service's own
 * contract (`identity.api.AuthorizationService`'s Javadoc, `Integration Contract.md` §2.1).
 */
@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final AuthorizationService authorizationService;
    private final CursorCodec cursorCodec;

    public AddressService(AddressRepository addressRepository, AuthorizationService authorizationService,
            CursorCodec cursorCodec) {
        this.addressRepository = addressRepository;
        this.authorizationService = authorizationService;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    public AddressPageResult listOwnAddresses(CallerContext caller, String cursor, int size) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.LIST_OWN_ADDRESSES);

        AddressRepository.Cursor after = decodeCursor(caller.accountId(), cursor);
        List<CustomerAddress> rows = addressRepository.findByAccountId(caller.accountId(), after, size + 1);
        boolean hasMore = rows.size() > size;
        List<CustomerAddress> page = hasMore ? rows.subList(0, size) : rows;
        String nextCursor = hasMore ? encodeCursor(caller.accountId(), addressRepository.cursorOf(page.getLast())) : null;
        return new AddressPageResult(page.stream().map(AddressSummary::of).toList(), nextCursor);
    }

    @Transactional
    public AddressSummary addOwnAddress(CallerContext caller, AddressCommand command) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.ADD_OWN_ADDRESS);

        // BR-CUS-05 ("at most one default shipping address", SRS §BR-CUS-05) governs *shipping*
        // only — the first address in the book becomes the default shipping address (main
        // scenario step 4). Nothing in the spec extends this to isDefaultBilling, which has no
        // documented business rule at all, so it is stored exactly as requested (default false).
        boolean isFirst = !addressRepository.existsAnyForAccount(caller.accountId());
        boolean defaultShipping = command.isDefaultShipping() || isFirst;
        boolean defaultBilling = command.isDefaultBilling();

        if (defaultShipping) {
            clearPreviousDefaultShipping(caller.accountId());
        }
        if (defaultBilling) {
            clearPreviousDefaultBilling(caller.accountId());
        }

        CustomerAddress address = CustomerAddress.add(UUID.randomUUID(), caller.accountId(), command.address(),
            defaultShipping, defaultBilling);
        return AddressSummary.of(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public AddressSummary getOwnAddress(CallerContext caller, UUID addressId) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.GET_OWN_ADDRESS);
        return AddressSummary.of(ownedAddressOrNotFound(caller.accountId(), addressId));
    }

    @Transactional
    public AddressSummary replaceOwnAddress(CallerContext caller, UUID addressId, AddressCommand command) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.REPLACE_OWN_ADDRESS);
        CustomerAddress address = ownedAddressOrNotFound(caller.accountId(), addressId);

        if (command.isDefaultShipping() && !address.defaultShipping()) {
            clearPreviousDefaultShipping(caller.accountId());
        }
        if (command.isDefaultBilling() && !address.defaultBilling()) {
            clearPreviousDefaultBilling(caller.accountId());
        }

        address.replace(command.address(), command.isDefaultShipping(), command.isDefaultBilling());
        return AddressSummary.of(addressRepository.save(address));
    }

    @Transactional
    public void removeOwnAddress(CallerContext caller, UUID addressId) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.REMOVE_OWN_ADDRESS);
        // Idempotent — removing an address already gone (or another customer's) is 204, not 404.
        addressRepository.findById(addressId)
            .filter(a -> a.isOwnedBy(caller.accountId()))
            .ifPresent(a -> addressRepository.delete(a.id()));
    }

    private CustomerAddress ownedAddressOrNotFound(UUID accountId, UUID addressId) {
        return addressRepository.findById(addressId)
            .filter(a -> a.isOwnedBy(accountId))
            .orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Address not found."));
    }

    private AddressRepository.Cursor decodeCursor(UUID accountId, String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        CursorPosition position = cursorCodec.decode(cursor, cursorContext(accountId));
        if (position.sortValues().size() != 1 || position.sortValues().getFirst().type() != CursorValue.Type.INSTANT) {
            throw new InvalidCursorException();
        }
        return new AddressRepository.Cursor(position.sortValues().getFirst().instantValue(), position.tieBreaker());
    }

    private String encodeCursor(UUID accountId, AddressRepository.Cursor cursor) {
        return cursorCodec.encode(cursorContext(accountId), List.of(CursorValue.instant(cursor.createdAt())), cursor.id());
    }

    private static CursorContext cursorContext(UUID accountId) {
        return new CursorContext("identity.own-addresses", accountId.toString(), "createdAt:desc", java.util.Map.of());
    }

    private void clearPreviousDefaultShipping(UUID accountId) {
        addressRepository.findAllByAccountId(accountId).stream()
            .filter(CustomerAddress::defaultShipping)
            .forEach(a -> {
                a.clearDefaultShipping();
                addressRepository.save(a);
            });
    }

    private void clearPreviousDefaultBilling(UUID accountId) {
        addressRepository.findAllByAccountId(accountId).stream()
            .filter(CustomerAddress::defaultBilling)
            .forEach(a -> {
                a.clearDefaultBilling();
                addressRepository.save(a);
            });
    }
}
