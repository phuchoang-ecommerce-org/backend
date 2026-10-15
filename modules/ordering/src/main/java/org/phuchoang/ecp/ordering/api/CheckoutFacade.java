package org.phuchoang.ecp.ordering.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

/** Public Checkout API; its adapter maps this contract to focused application use cases. */
public interface CheckoutFacade {
    CheckoutOutcome initiateCheckout(IdentityActor caller);
    CheckoutView getCurrentCheckout(IdentityActor caller);
    CheckoutView setCheckoutShippingAddress(IdentityActor caller, UUID id, UUID addressId, CheckoutAddressRequest address, boolean saveToAddressBook);
    ShippingQuoteView getShippingQuotes(IdentityActor caller, UUID id);
    CheckoutView setCheckoutBillingInformation(IdentityActor caller, UUID id, UUID addressId, CheckoutAddressRequest address, boolean sameAsShipping);
    CheckoutView selectCheckoutShippingOption(IdentityActor caller, UUID id, String optionId);
    CheckoutView applyCheckoutVoucher(IdentityActor caller, UUID id, String code);
    void removeCheckoutVoucher(IdentityActor caller, UUID id);
    CheckoutSummaryView getOrderSummary(IdentityActor caller, UUID id);
}
