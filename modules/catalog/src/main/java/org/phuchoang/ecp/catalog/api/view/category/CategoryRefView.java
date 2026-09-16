package org.phuchoang.ecp.catalog.api.view.category;

import java.util.UUID;

/**
 * A compact category breadcrumb reference.
 *
 * @param id stable category identifier
 * @param name display name
 * @param slug human-readable category identifier, never a materialized path
 */
public record CategoryRefView(
    /** Stable category identifier. */ UUID id,
    /** Display name. */ String name,
    /** Human-readable category identifier, never a materialized path. */ String slug) {
}
