package org.phuchoang.ecp.payment.internal.application.reconciliation.query;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUnmatchedPaymentsService {
  private final IdentityAuthorization authorization;
  private final UnmatchedPaymentsQuery query;

  @Transactional(readOnly = true)
  public UnmatchedPaymentPage list(IdentityActor caller, Boolean resolved, String cursor, int size) {
    authorization.assertAuthorized(caller, "listUnmatchedPayments");
    if (size < 1 || size > 100)
      throw new IllegalArgumentException("Invalid page size.");
    return query.list(resolved, cursor, size);
  }
}
