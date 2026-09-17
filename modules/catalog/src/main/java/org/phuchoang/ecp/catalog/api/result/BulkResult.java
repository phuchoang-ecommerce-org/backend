package org.phuchoang.ecp.catalog.api.result;
import java.util.List;

/** Ordered per-item outcome for a bulk catalog amendment. */
public record BulkResult(List<BulkItemResult> results) { }
