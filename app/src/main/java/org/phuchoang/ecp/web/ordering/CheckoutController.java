package org.phuchoang.ecp.web.ordering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.phuchoang.ecp.ordering.api.CheckoutAddressRequest;
import org.phuchoang.ecp.ordering.api.CheckoutFacade;
import org.phuchoang.ecp.ordering.api.CheckoutSummaryView;
import org.phuchoang.ecp.ordering.api.CheckoutView;
import org.phuchoang.ecp.ordering.api.ShippingQuoteView;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** HTTP adapter for Sprint 17's draft-order checkout funnel. */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class CheckoutController {
    private final CheckoutFacade checkouts;
    private final RequestContextResolver requestContext;

    @PostMapping("/api/v1/checkouts")
    ResponseEntity<CheckoutView> initiate(@AuthenticationPrincipal Jwt jwt) {
        var outcome = checkouts.initiateCheckout(context(jwt).caller());
        if (outcome.resumed()) return ResponseEntity.ok(outcome.checkout());
        return ResponseEntity.created(URI.create("/api/v1/checkouts/" + outcome.checkout().order().id())).body(outcome.checkout());
    }

    @GetMapping("/api/v1/checkouts/current")
    CheckoutView current(@AuthenticationPrincipal Jwt jwt) {
        return checkouts.getCurrentCheckout(context(jwt).caller());
    }

    @PutMapping("/api/v1/checkouts/{checkoutId}/shipping-address")
    CheckoutView setShippingAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId,
            @Valid @RequestBody ShippingAddressSelection request) {
        return checkouts.setCheckoutShippingAddress(context(jwt).caller(), checkoutId, request.addressId(), address(request.address()),
            request.shouldSaveToAddressBook());
    }

    /** Provisional ownership is documented in Sprint 17; Shipping takes this computation in Sprint 20. */
    @GetMapping("/api/v1/shipping-quotes")
    ShippingQuoteView shippingQuotes(@AuthenticationPrincipal Jwt jwt, @org.springframework.web.bind.annotation.RequestParam UUID checkoutId) {
        return checkouts.getShippingQuotes(context(jwt).caller(), checkoutId);
    }

    @PutMapping("/api/v1/checkouts/{checkoutId}/billing-information")
    CheckoutView setBillingInformation(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId,
            @Valid @RequestBody BillingInformation request) {
        return checkouts.setCheckoutBillingInformation(context(jwt).caller(), checkoutId, request.addressId(), address(request.address()),
            request.sameAsShipping());
    }

    @PutMapping("/api/v1/checkouts/{checkoutId}/shipping-option")
    CheckoutView selectShippingOption(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId,
            @Valid @RequestBody ShippingOptionSelection request) {
        return checkouts.selectCheckoutShippingOption(context(jwt).caller(), checkoutId, request.shippingOptionId());
    }

    @PutMapping("/api/v1/checkouts/{checkoutId}/voucher")
    CheckoutView applyVoucher(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId,
            @Valid @RequestBody VoucherApplication request) {
        return checkouts.applyCheckoutVoucher(context(jwt).caller(), checkoutId, request.code());
    }

    @DeleteMapping("/api/v1/checkouts/{checkoutId}/voucher")
    ResponseEntity<Void> removeVoucher(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId) {
        checkouts.removeCheckoutVoucher(context(jwt).caller(), checkoutId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/checkouts/{checkoutId}/summary")
    CheckoutSummaryView summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID checkoutId) {
        return checkouts.getOrderSummary(context(jwt).caller(), checkoutId);
    }

    private RequestContext context(Jwt jwt) { return requestContext.resolve(jwt); }

    private static CheckoutAddressRequest address(AddressWrite value) {
        return value == null ? null : new CheckoutAddressRequest(value.label(), value.recipientName(), value.line1(), value.line2(),
            value.city(), value.region(), value.postalCode(), value.countryCode(), value.phone());
    }

    record ShippingAddressSelection(UUID addressId, @Valid AddressWrite address, Boolean saveToAddressBook) {
        boolean shouldSaveToAddressBook() { return saveToAddressBook == null || saveToAddressBook; }
    }
    record BillingInformation(UUID addressId, @Valid AddressWrite address, boolean sameAsShipping) { }
    record ShippingOptionSelection(@NotBlank String shippingOptionId) { }
    record VoucherApplication(@NotBlank String code) { }
    record AddressWrite(String label, @NotBlank String recipientName, @NotBlank String line1, String line2,
            @NotBlank String city, String region, @NotBlank String postalCode, @NotBlank String countryCode, String phone) { }
}
