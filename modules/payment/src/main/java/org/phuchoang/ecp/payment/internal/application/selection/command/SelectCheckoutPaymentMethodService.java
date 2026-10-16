package org.phuchoang.ecp.payment.internal.application.selection.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelectCheckoutPaymentMethodService {
  private final IdentityAuthorization authorization;
  private final CheckoutPaymentSelectionPort selections;

  @Transactional
  public void select(IdentityActor caller, UUID checkoutId, String methodName) {
    PaymentMethod method = PaymentMethod.valueOf(methodName);
    authorization.assertAuthorized(caller, "selectCheckoutPaymentMethod");
    if (method == PaymentMethod.CASH_ON_DELIVERY)
      throw new MethodNotEligible("Cash on delivery is not configured.");
    selections.save(checkoutId, caller.accountId(), method);
  }

  public static final class MethodNotEligible extends RuntimeException {
    public MethodNotEligible(String message) {
      super(message);
    }
  }
}
