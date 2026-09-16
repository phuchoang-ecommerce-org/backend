package org.phuchoang.ecp.identity.domain;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.sharedkernel.api.address.Address;

import java.util.Objects;
import java.util.UUID;

/**
 * One entry in a customer's address book (`UC-CUS-09`, Database.md §4.1's {@code identity_address}).
 * A separate aggregate from {@link Account} — the address book changes far more often than the
 * account record itself, and nothing about {@code Account} needs to load with it.
 *
 * <p>{@code BR-CUS-05} ("exactly one default") is enforced by the application service, which
 * clears the previous default before saving a new one, backed by the database's own partial
 * unique index (`ux_identity_address_default_shipping`/`_billing`) as the final guarantee — the
 * same layering `Account`'s email uniqueness uses.
 */
@AggregateRoot
public final class CustomerAddress {

    @Identity
    private final UUID id;
    private final UUID accountId;
    private Address address;
    private boolean defaultShipping;
    private boolean defaultBilling;

    private CustomerAddress(UUID id, UUID accountId, Address address, boolean defaultShipping,
            boolean defaultBilling) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.address = Objects.requireNonNull(address);
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
    }

    public static CustomerAddress add(UUID id, UUID accountId, Address address, boolean defaultShipping,
            boolean defaultBilling) {
        return new CustomerAddress(id, accountId, address, defaultShipping, defaultBilling);
    }

    public static CustomerAddress reconstitute(UUID id, UUID accountId, Address address, boolean defaultShipping,
            boolean defaultBilling) {
        return new CustomerAddress(id, accountId, address, defaultShipping, defaultBilling);
    }

    /** `UC-CUS-09` A1: whole replacement, per `replaceOwnAddress`'s idempotent semantics. */
    public void replace(Address newAddress, boolean defaultShipping, boolean defaultBilling) {
        this.address = Objects.requireNonNull(newAddress);
        this.defaultShipping = defaultShipping;
        this.defaultBilling = defaultBilling;
    }

    public void clearDefaultShipping() {
        this.defaultShipping = false;
    }

    public void clearDefaultBilling() {
        this.defaultBilling = false;
    }

    public boolean isOwnedBy(UUID candidateAccountId) {
        return accountId.equals(candidateAccountId);
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public Address address() {
        return address;
    }

    public boolean defaultShipping() {
        return defaultShipping;
    }

    public boolean defaultBilling() {
        return defaultBilling;
    }
}
