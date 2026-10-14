package org.phuchoang.ecp.promotion.internal.application.list.query;

import java.util.List;

/** Query port for the operator-oriented promotion projection. */
public interface PromotionListQuery {
    List<PromotionListItem> list(String status, int limit);
}
