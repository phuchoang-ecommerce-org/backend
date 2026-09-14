package org.phuchoang.ecp.identity.application.port;

import org.phuchoang.ecp.identity.domain.CustomerAddress;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The persistence port for {@link CustomerAddress} (`identity_address`, `UC-CUS-09`). */
public interface AddressRepository {

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

    /** All of the account's addresses, unpaged — used to find and clear the previous default (`BR-CUS-05`). */
    List<CustomerAddress> findAllByAccountId(UUID accountId);

    void delete(UUID id);

    boolean existsAnyForAccount(UUID accountId);

    record Cursor(Instant createdAt, UUID id) {
    }
}
