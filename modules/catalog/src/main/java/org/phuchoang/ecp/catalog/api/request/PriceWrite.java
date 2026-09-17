package org.phuchoang.ecp.catalog.api.request;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;

/** Requested replacement list price and its required administration audit reason. */
public record PriceWrite(MoneyView listPrice, String reason) { }
