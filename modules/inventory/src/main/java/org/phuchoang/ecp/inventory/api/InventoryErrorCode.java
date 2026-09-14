package org.phuchoang.ecp.inventory.api;

import org.phuchoang.ecp.sharedkernel.api.ErrorCode;

/**
 * The {@code INV} domain (Error Codes.md §3.5). {@code ECP-INV-4091} is the visible surface of the
 * optimistic-locking oversell guarantee (ADR-0011) — expected under peak load, not exceptional.
 */
public enum InventoryErrorCode implements ErrorCode {

    INSUFFICIENT_STOCK("ECP-INV-4091", 409, "Insufficient available stock");

    private final String code;
    private final int httpStatus;
    private final String title;

    InventoryErrorCode(String code, int httpStatus, String title) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.title = title;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String title() {
        return title;
    }
}
