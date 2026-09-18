package org.phuchoang.ecp.catalog.internal.domain.model;

import java.util.Locale;

/** The one Catalog slug algorithm — products and categories derive their URL-safe identifiers here. */
public final class Slugs {

    private Slugs() {
    }

    /** Lower-cases {@code value}, collapses every non-alphanumeric run to one hyphen, and trims hyphens. */
    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
