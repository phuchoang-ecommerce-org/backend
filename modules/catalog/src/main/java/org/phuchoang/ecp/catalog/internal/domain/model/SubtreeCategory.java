package org.phuchoang.ecp.catalog.internal.domain.model;

import java.util.UUID;

/** One category in a materialised-path subtree, as needed to name the listings a change affects. */
public record SubtreeCategory(UUID id, String slug) {
}
