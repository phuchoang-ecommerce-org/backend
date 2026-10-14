package org.phuchoang.ecp.promotion.api;

/** A cryptographically random voucher generated for an existing promotion. */
public record Voucher(String code, boolean singleUse) { }
