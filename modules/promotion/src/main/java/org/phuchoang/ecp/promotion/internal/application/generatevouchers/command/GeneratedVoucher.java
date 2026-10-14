package org.phuchoang.ecp.promotion.internal.application.generatevouchers.command;

/** Command-owned representation of a generated voucher. */
public record GeneratedVoucher(String code, boolean singleUse) {
}
