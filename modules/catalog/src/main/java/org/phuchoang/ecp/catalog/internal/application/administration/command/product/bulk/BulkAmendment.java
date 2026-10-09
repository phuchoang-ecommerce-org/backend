package org.phuchoang.ecp.catalog.internal.application.administration.command.product.bulk;

import org.phuchoang.ecp.catalog.internal.application.administration.command.product.update.ProductChange;

import java.util.UUID;

/** One independently applied product amendment in a bulk request. */
public record BulkAmendment(UUID productId, ProductChange change) {
}
