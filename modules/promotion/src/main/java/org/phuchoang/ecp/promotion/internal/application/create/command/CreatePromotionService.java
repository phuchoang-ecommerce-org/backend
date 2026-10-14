package org.phuchoang.ecp.promotion.internal.application.create.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionCreationDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Authorizes, audits, and invokes the authoritative promotion-creation domain operation. */
@Service
@RequiredArgsConstructor
public class CreatePromotionService {

    private static final String CREATE_PROMOTION = "createPromotion";

    private final PromotionCreationDomainService promotions;
    private final IdentityAuthorization authorization;
    private final AuditTrail audit;
    private final CreatePromotionMapper mapper;
    private final Clock clock;

    @Transactional
    public CreatedPromotion create(CreatePromotionCommand command) {
        authorizeOrAuditDenial(command);
        Instant now = clock.instant();
        Promotion promotion = promotions.create(mapper.domainRequest(command, now));
        audit.record(new AuditRecord(UUID.randomUUID(), command.caller().accountId(), firstRole(command), "promotionCreated",
            "promotion", promotion.id(), "promotion", Map.of(), Map.of("name", promotion.name(), "usageLimit", promotion.usageLimit()),
            null, command.correlationId(), now, null, false));
        return mapper.createdPromotion(promotion);
    }

    private void authorizeOrAuditDenial(CreatePromotionCommand command) {
        try {
            authorization.assertAuthorized(command.caller(), CREATE_PROMOTION);
        } catch (RuntimeException denied) {
            audit.record(new AuditRecord(UUID.randomUUID(), command.caller().accountId(), firstRole(command), "promotionCreationDenied",
                "promotion", null, "promotion", Map.of(), Map.of("name", command.name()), null, command.correlationId(),
                clock.instant(), null, false));
            throw denied;
        }
    }

    private static String firstRole(CreatePromotionCommand command) {
        return command.caller().roles().stream().sorted().findFirst().orElse(null);
    }
}
