package org.phuchoang.ecp.web.promotion;

import org.phuchoang.ecp.promotion.api.PromotionFacade;
import org.phuchoang.ecp.promotion.api.PromotionView;
import org.phuchoang.ecp.promotion.api.PromotionWrite;
import org.phuchoang.ecp.promotion.api.Voucher;
import org.phuchoang.ecp.promotion.api.VoucherValidationResult;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Thin HTTP boundary for the Sprint-15 management and non-binding validation operations. */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class PromotionController {
    private final PromotionFacade promotions;
    private final RequestContextResolver requestContext;

    @PostMapping("/api/v1/promotions")
    ResponseEntity<PromotionView> create(@AuthenticationPrincipal Jwt jwt, @RequestBody PromotionWrite write) {
        RequestContext context = requestContext.resolve(jwt);
        PromotionView created = promotions.createPromotion(context.caller(), context.correlationId(), write);
        return ResponseEntity.created(URI.create("/api/v1/promotions/" + created.id())).body(created);
    }

    @GetMapping("/api/v1/promotions")
    PageEnvelope<PromotionView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int size) {
        List<PromotionView> items = promotions.listPromotions(requestContext.resolve(jwt).caller(), status, size);
        return new PageEnvelope<>(items, new Page(items.size(), null, null));
    }

    @PostMapping("/api/v1/promotions/{promotionId}/vouchers")
    ResponseEntity<List<Voucher>> generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID promotionId,
            @RequestBody VoucherBatchWrite write) {
        RequestContext context = requestContext.resolve(jwt);
        List<Voucher> vouchers = promotions.generatePromotionVouchers(context.caller(), context.correlationId(), promotionId,
            write.count(), write.singleUse(), write.prefix());
        return ResponseEntity.created(URI.create("/api/v1/promotions/" + promotionId + "/vouchers")).body(vouchers);
    }

    @PostMapping("/api/v1/voucher-validations")
    VoucherValidationResult validate(@AuthenticationPrincipal Jwt jwt, @RequestBody VoucherValidationWrite write) {
        RequestContext context = requestContext.resolve(jwt);
        if (context.caller() == null || context.caller().accountId() == null) return VoucherValidationResult.notValid();
        return promotions.validateVoucher(context.caller().accountId(), write.code(), write.discountableAmount());
    }

    record VoucherBatchWrite(int count, boolean singleUse, String prefix) { }
    record VoucherValidationWrite(String code, BigDecimal discountableAmount) { }
}
