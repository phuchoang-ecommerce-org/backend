package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal persistence gateway for Account-owned address entities (`identity_address`, `UC-CUS-09`).
 * It is deliberately not a DDD repository: {@code AccountRepository} is Identity's only repository.
 */
public interface AddressStore {

    CustomerAddress save(CustomerAddress address);

    Optional<CustomerAddress> findById(UUID id);

    /**
     * Reverse-chronological (by {@code created_at}, ties broken by {@code id}), one page of
     * {@code size} rows. {@code after} is the last row's cursor from the previous page, or
     * {@code null} for the first page.
     */
    List<CustomerAddress> findByAccountId(UUID accountId, Cursor after, int size);

    /** The cursor of {@code address}, for building the next page's {@code next} value. */
    Cursor cursorOf(CustomerAddress address);

    /** Clears the account's current default shipping address, if any, in one update (`BR-CUS-05`). */
    void clearDefaultShipping(UUID accountId);

    /** Clears the account's current default billing address, if any, in one update. */
    void clearDefaultBilling(UUID accountId);

    void delete(UUID id);

    boolean existsAnyForAccount(UUID accountId);

    record Cursor(Instant createdAt, UUID id) {
    }
}
