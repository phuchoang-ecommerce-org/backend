package org.phuchoang.ecp.web.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.phuchoang.ecp.payment.api.PaymentFacade;
import org.phuchoang.ecp.payment.api.PaymentMethod;
import org.phuchoang.ecp.payment.api.PaymentOutcome;
import org.phuchoang.ecp.ordering.api.CheckoutFacade;
import org.phuchoang.ecp.ordering.api.CheckoutView;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.pagination.Pagination;
import org.phuchoang.ecp.web.common.request.QueryParams;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** HTTP-only adapter; card instruments are not part of any request record here. */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class PaymentController {
    private final PaymentFacade payments;
    private final CheckoutFacade checkouts;
    private final RequestContextResolver context;

    @GetMapping("/api/v1/orders/{orderId}/payment-methods")
    List<PaymentMethodView> eligible(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return List.of(PaymentMethod.CARD, PaymentMethod.BANK_TRANSFER, PaymentMethod.WALLET).stream()
            .map(method -> new PaymentMethodView(method, display(method), true, null)).toList();
    }

    @PutMapping("/api/v1/checkouts/{checkoutId}/payment-method")
    ResponseEntity<CheckoutView> select(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId, @Valid @RequestBody PaymentMethodWrite write) {
        var caller = context.resolve(jwt).caller();
        payments.selectCheckoutPaymentMethod(caller, checkoutId, write.method());
        return ResponseEntity.ok(checkouts.getCurrentCheckout(caller));
    }

    @PostMapping("/api/v1/payments")
    ResponseEntity<InitiationView> initiate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody InitiationWrite write,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var request = context.resolve(jwt);
        var result = payments.initiatePayment(request.caller(), write.orderId(), write.method(), idempotencyKey, write.returnUrl(), request.correlationId());
        URI location = URI.create("/api/v1/payments/" + result.payment().id());
        return ResponseEntity.status(result.accepted() ? HttpStatus.ACCEPTED : HttpStatus.CREATED).location(location).body(view(result));
    }

    @GetMapping("/api/v1/orders/{orderId}/payment")
    PaymentView payment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        try {
            return view(payments.getOrderPayment(context.resolve(jwt).caller(), orderId));
        } catch (org.phuchoang.ecp.payment.internal.application.get.query.GetOrderPaymentService.PaymentNotFound missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/api/v1/unmatched-payments")
    PageEnvelope<UnmatchedPaymentView> unmatched(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
            @RequestParam(required = false) Boolean resolved, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        QueryParams.rejectUnknown(request, Set.of("resolved", "cursor", "size"));
        var page = payments.listUnmatchedPayments(context.resolve(jwt).caller(), resolved, cursor, Pagination.clampSize(size));
        var items = page.items().stream().map(item -> new UnmatchedPaymentView(item.id(), item.providerReference(),
            item.amount() == null ? null : new MoneyView(item.amount().toPlainString(), item.currency()), item.outcome(),
            item.receivedAt(), item.resolvedAt())).toList();
        return new PageEnvelope<>(items, new Page(items.size(), page.nextCursor(), null));
    }

    @PostMapping("/api/v1/payment-provider-notifications")
    ResponseEntity<Void> receive(@RequestBody ProviderNotificationWrite write, @RequestHeader("X-Provider-Signature") String signature) {
        try {
            payments.receivePaymentProviderNotification(write.providerReference(), write.outcome(), write.amount(), write.currency(), write.providerEventId(),
                write.occurredAt(), write.providerPayload() == null ? Map.of() : write.providerPayload(), signature,
                context.resolve(null).correlationId());
            return ResponseEntity.accepted().build();
        } catch (SecurityException rejected) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Provider signature could not be verified."); }
    }

    private static String display(PaymentMethod method) { return method.name().replace('_', ' '); }
    private static InitiationView view(PaymentFacade.Initiation value) {
        var a = value.attempt();
        return new InitiationView(view(value.payment()), new AttemptView(a.id(), a.paymentId(), a.attemptNumber(),
            money(a.amount(), a.currency()), a.outcome(), a.providerReference(), a.failureReason(), a.requestedAt(),
            a.resolvedAt()), value.redirectUrl());
    }
    private static PaymentView view(PaymentFacade.PaymentDetails p) {
        return new PaymentView(p.id(), p.orderId(), p.customerId(), p.method(), money(p.authorisedAmount(), p.currency()),
            money(p.capturedAmount(), p.currency()), money(p.refundedAmount(), p.currency()), p.currency(), p.status(), p.createdAt());
    }
    private static MoneyView money(BigDecimal amount, String currency) {
        return amount == null ? null : new MoneyView(amount.toPlainString(), currency);
    }
    record PaymentMethodView(PaymentMethod type, String displayName, boolean eligible, String ineligibleReason) { }
    record PaymentMethodWrite(@NotNull PaymentMethod method) { }
    record InitiationWrite(@NotNull UUID orderId, @NotNull PaymentMethod method, URI returnUrl) { }
    record ProviderNotificationWrite(@NotBlank String providerReference, @NotNull PaymentOutcome outcome, @NotNull BigDecimal amount,
        @NotBlank String currency, String providerEventId, Instant occurredAt, Map<String,Object> providerPayload) { }
    record InitiationView(PaymentView payment, AttemptView attempt, URI redirectUrl) { }
    record PaymentView(UUID id, UUID orderId, UUID customerId, PaymentMethod method, MoneyView authorisedAmount,
        MoneyView capturedAmount, MoneyView refundedAmount, String currency, Object status, Instant createdAt) { }
    record AttemptView(UUID id, UUID paymentId, int attemptNumber, MoneyView amount, PaymentOutcome outcome,
        String providerReference, String failureReason, Instant requestedAt, Instant resolvedAt) { }
    record MoneyView(String amount, String currency) { }
    record UnmatchedPaymentView(UUID id, String providerReference, MoneyView amount, String outcome,
        Instant receivedAt, Instant resolvedAt) { }
}
