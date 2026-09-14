package org.phuchoang.ecp.catalog.application.query;

import java.util.UUID;

/**
 * Sprint 04 (`US-AUD-03`) RBAC wiring demo stub — see {@link GetProductService}.
 *
 * @param id identifier supplied to the demo operation
 * @param name hard-coded demo display name; not persisted catalog data
 */
public record ProductView(
    /** Identifier supplied to the demo operation. */ UUID id,
    /** Hard-coded demo display name; not persisted catalog data. */ String name) {
}
