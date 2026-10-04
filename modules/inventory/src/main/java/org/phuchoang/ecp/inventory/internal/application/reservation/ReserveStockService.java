package org.phuchoang.ecp.inventory.internal.application.reservation;

import org.phuchoang.ecp.inventory.api.reservation.InsufficientStockException;
import org.phuchoang.ecp.inventory.api.reservation.ReservationSet;
import org.phuchoang.ecp.inventory.api.reservation.ReserveStockCommand;
import org.phuchoang.ecp.inventory.api.reservation.StockShortfall;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Bounded optimistic-lock retries around a complete, independently transactional reservation attempt. */
@Service
public class ReserveStockService {

    private final ReserveStockAttempt attempt;
    private final int maxAttempts;

    public ReserveStockService(ReserveStockAttempt attempt,
            @Value("${ecp.inventory.reservation.max-attempts:8}") int maxAttempts) {
        this.attempt = attempt;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public ReservationSet reserve(ReserveStockCommand command) {
        for (int number = 1; number <= maxAttempts; number++) {
            try {
                return attempt.reserve(command);
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException race) {
                // A concurrent duplicate resolves to its existing reservation on the next attempt;
                // a stock race is re-evaluated against current availability, never treated as a 500.
            }
        }
        throw exhausted(command);
    }

    private static InsufficientStockException exhausted(ReserveStockCommand command) {
        List<StockShortfall> shortfalls = command.lines().stream()
            .map(line -> new StockShortfall(line.orderLineId(), line.sku(), line.quantity(), 0)).toList();
        return new InsufficientStockException(shortfalls);
    }
}
