package org.phuchoang.ecp.catalog.api.request;
import java.util.UUID;

/** One independently reportable product amendment in an administrator bulk request. */
public record BulkItem(UUID productId, ProductWrite amendment) { }
