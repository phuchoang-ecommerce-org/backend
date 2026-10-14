package org.phuchoang.ecp.promotion.internal.application.generatevouchers.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionVoucherDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Authorizes and records the voucher-generation use case. */
@Service
@RequiredArgsConstructor
public class GeneratePromotionVouchersService {

    private final PromotionVoucherDomainService vouchers;
    private final IdentityAuthorization authorization;
    private final AuditTrail audit;
    private final GeneratePromotionVouchersMapper mapper;
    private final Clock clock;

    @Transactional
    public List<GeneratedVoucher> generate(GeneratePromotionVouchersCommand command) {
        authorization.assertAuthorized(command.caller(), "generatePromotionVouchers");
        List<GeneratedVoucher> generated = vouchers.generate(mapper.domainRequest(command)).stream()
            .map(mapper::generatedVoucher)
            .toList();
        audit.record(new AuditRecord(UUID.randomUUID(), command.caller().accountId(), firstRole(command),
            "promotionVouchersGenerated", "promotion", command.promotionId(), "promotion", Map.of(),
            Map.of("count", command.count(), "singleUse", command.singleUse()), null, command.correlationId(),
            clock.instant(), null, false));
        return generated;
    }

    private static String firstRole(GeneratePromotionVouchersCommand command) {
        return command.caller().roles().stream().sorted().findFirst().orElse(null);
    }
}
