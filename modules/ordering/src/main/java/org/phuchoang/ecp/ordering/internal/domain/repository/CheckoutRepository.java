package org.phuchoang.ecp.ordering.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;

import java.util.Optional;
import java.util.UUID;

/** Aggregate persistence port for an in-progress Draft checkout. */
@Repository
public interface CheckoutRepository {
    Optional<Checkout> findById(UUID id);
    Optional<Checkout> findDraftByCustomerId(UUID customerId);
    Checkout save(Checkout checkout);
}
