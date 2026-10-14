package org.phuchoang.ecp.promotion.internal.application.list.query;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Authorizes and executes the observational operator list query. */
@Service
@RequiredArgsConstructor
public class ListPromotionsService {

    private final PromotionListQuery promotions;
    private final IdentityAuthorization authorization;

    @Transactional(readOnly = true)
    public List<PromotionListItem> list(IdentityActor caller, String status, int limit) {
        authorization.assertAuthorized(caller, "listPromotions");
        return promotions.list(status, limit);
    }
}
