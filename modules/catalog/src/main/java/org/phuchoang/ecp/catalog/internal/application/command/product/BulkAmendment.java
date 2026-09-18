package org.phuchoang.ecp.catalog.internal.application.command.product;

import java.util.UUID;

/** One independently applied product amendment in a bulk request. */
public record BulkAmendment(UUID productId, ProductChange change) {
}
