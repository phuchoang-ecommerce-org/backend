package org.phuchoang.ecp.catalog.internal.application.command.product;

import java.util.UUID;

/** The result of one bulk item; {@code code}/{@code detail} are set only when {@code applied} is false. */
public record BulkAmendmentOutcome(UUID productId, boolean applied, String code, String detail) {

    public static BulkAmendmentOutcome applied(UUID productId) {
        return new BulkAmendmentOutcome(productId, true, null, null);
    }

    public static BulkAmendmentOutcome failed(UUID productId, String code, String detail) {
        return new BulkAmendmentOutcome(productId, false, code, detail);
    }
}
