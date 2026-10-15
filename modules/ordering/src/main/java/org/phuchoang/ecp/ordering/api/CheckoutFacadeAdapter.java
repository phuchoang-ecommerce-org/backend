package org.phuchoang.ecp.ordering.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.api.error.DomainException;
import org.phuchoang.ecp.ordering.api.error.GenErrorCode;
import org.phuchoang.ecp.ordering.api.OrderErrorCode;
import org.phuchoang.ecp.ordering.internal.application.checkout.billing.command.SetCheckoutBillingInformationService;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandFailures;
import org.phuchoang.ecp.ordering.internal.application.checkout.current.query.GetCurrentCheckoutService;
import org.phuchoang.ecp.ordering.internal.application.checkout.initiate.command.InitiateCheckoutService;
import org.phuchoang.ecp.ordering.internal.application.checkout.review.command.ReviewCheckoutSummaryService;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.command.SelectCheckoutShippingOptionService;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.command.SetCheckoutShippingAddressService;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query.GetShippingQuotesService;
import org.phuchoang.ecp.ordering.internal.application.checkout.voucher.command.ApplyCheckoutVoucherService;
import org.phuchoang.ecp.ordering.internal.application.checkout.voucher.command.RemoveCheckoutVoucherService;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutFailure;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutFailures;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class CheckoutFacadeAdapter implements CheckoutFacade {
  private final InitiateCheckoutService initiate;
  private final GetCurrentCheckoutService current;
  private final SetCheckoutShippingAddressService shippingAddress;
  private final GetShippingQuotesService shippingQuotes;
  private final SetCheckoutBillingInformationService billing;
  private final SelectCheckoutShippingOptionService shippingOption;
  private final ApplyCheckoutVoucherService voucher;
  private final RemoveCheckoutVoucherService removeVoucher;
  private final ReviewCheckoutSummaryService review;
  private final CheckoutApiMapper mapper;

  @Override
  public CheckoutOutcome initiateCheckout(IdentityActor caller) {
    return translate(() -> {
      var result = initiate.initiate(caller);
      return new CheckoutOutcome(mapper.checkoutView(result), result.resumed());
    });
  }

  @Override
  public CheckoutView getCurrentCheckout(IdentityActor caller) {
    return translate(() -> mapper.checkoutView(current.get(caller)));
  }

  @Override
  public CheckoutView setCheckoutShippingAddress(IdentityActor caller, UUID id, UUID addressId,
      CheckoutAddressRequest address, boolean save) {
    return translate(
        () -> mapper.checkoutView(shippingAddress.set(caller, id, addressId, mapper.command(address), save)));
  }

  @Override
  public ShippingQuoteView getShippingQuotes(IdentityActor caller, UUID id) {
    return translate(() -> mapper.shippingQuoteView(shippingQuotes.get(caller, id)));
  }

  @Override
  public CheckoutView setCheckoutBillingInformation(IdentityActor caller, UUID id, UUID addressId,
      CheckoutAddressRequest address, boolean same) {
    return translate(() -> mapper.checkoutView(billing.set(caller, id, addressId, mapper.command(address), same)));
  }

  @Override
  public CheckoutView selectCheckoutShippingOption(IdentityActor caller, UUID id, String optionId) {
    return translate(() -> mapper.checkoutView(shippingOption.select(caller, id, optionId)));
  }

  @Override
  public CheckoutView applyCheckoutVoucher(IdentityActor caller, UUID id, String code) {
    return translate(() -> mapper.checkoutView(voucher.apply(caller, id, code)));
  }

  @Override
  public void removeCheckoutVoucher(IdentityActor caller, UUID id) {
    translate(() -> {
      removeVoucher.remove(caller, id);
      return null;
    });
  }

  @Override
  public CheckoutSummaryView getOrderSummary(IdentityActor caller, UUID id) {
    return translate(() -> mapper.summaryView(review.review(caller, id)));
  }

  private static <T> T translate(java.util.function.Supplier<T> action) {
    try {
      return action.get();
    } catch (RuntimeException exception) {
      CheckoutFailure failure = CheckoutFailures.map(exception);
      if (failure == null)
        throw exception;
      throw switch (failure.kind()) {
        case NOT_FOUND -> new DomainException(GenErrorCode.NOT_FOUND, failure.message());
        case NOT_PURCHASABLE -> new DomainException(OrderErrorCode.CART_NOT_PURCHASABLE, failure.message());
        case VOUCHER_NOT_APPLICABLE -> new DomainException(OrderErrorCode.VOUCHER_NOT_APPLICABLE, failure.message());
        case DISCOUNT_EXCEEDS_SUBTOTAL -> new DomainException(OrderErrorCode.DISCOUNT_EXCEEDS_ORDER, failure.message());
        case UNVERIFIED_ACCOUNT -> new DomainException(GenErrorCode.FORBIDDEN, failure.message());
      };
    }
  }
}
