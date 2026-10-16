package org.phuchoang.ecp.ordering.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/** Authoritative persistence port for the placed Order aggregate. */
@Repository
public interface OrderRepository {
    Optional<Order> findById(UUID id);
    Order save(Order order);
    Order saveTransition(Order order);
}
