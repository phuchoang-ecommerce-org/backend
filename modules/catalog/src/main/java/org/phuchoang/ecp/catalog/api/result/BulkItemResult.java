package org.phuchoang.ecp.catalog.api.result;
import java.util.UUID;

/** Outcome of one item in a bulk catalog amendment; a failed item does not hide other outcomes. */
public record BulkItemResult(UUID productId, boolean applied, String code, String detail) { }
