package org.phuchoang.ecp.catalog.api.administration.product;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;

/** Requested replacement list price and its required administration audit reason. */
public record PriceWrite(MoneyView listPrice, String reason) { }
