package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.inventory.internal.domain.service.ReservationRetryPolicy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/** Command-side owner of UC-INV-04. Audit and aggregate state share this local transaction. */
@Service
public class AdjustStockService {
    private static final String ADJUST_STOCK = "adjustStock";
    private final IdentityAuthorization authorization;
    private final AdjustStockAttempt attempt;
    private final AuditTrail audit;
    private final Clock clock;
    private final int maxAttempts;

    public AdjustStockService(IdentityAuthorization authorization, AdjustStockAttempt attempt,
            AuditTrail audit, Clock clock, ReservationRetryPolicy retryPolicy) {
        this.authorization = authorization; this.attempt = attempt; this.audit = audit; this.clock = clock;
        this.maxAttempts = retryPolicy.maxAttempts();
    }

    public AdjustStockResult adjust(AdjustStockCommand command) {
        try {
            authorization.assertAuthorized(command.caller(), ADJUST_STOCK);
        } catch (RuntimeException denied) {
            audit.record(new AuditRecord(UUID.randomUUID(), command.caller().accountId(), role(command.caller()), "inventoryAdjustmentDenied",
                "stockItem", command.stockItemId(), "inventory", Map.of(), Map.of("delta", command.delta()), command.reason(),
                command.correlationId(), clock.instant(), command.caller().accountId() == null ? "http" : null,
                command.caller().accountId() == null));
            throw denied;
        }
        OptimisticLockingFailureException last = null;
        for (int number = 1; number <= maxAttempts; number++) {
            try {
                return attempt.apply(command);
            } catch (OptimisticLockingFailureException race) {
                last = race;
            }
        }
        throw last;
    }

    private static String role(IdentityActor actor) { return actor.roles().stream().sorted().findFirst().orElse(null); }
}
