package org.phuchoang.ecp.catalog.internal.application.command;

/**
 * The Catalog rows of `Permission Matrix.md` §5.1, as operation ids understood by identity's
 * {@code IdentityAuthorization} OHS. Every command service names the one that guards it at its
 * entry point — never an inline string.
 */
public final class CatalogPermissions {

    public static final String MANAGE_PRODUCTS = "manageProducts";
    public static final String MANAGE_CATEGORIES = "manageCategories";

    private CatalogPermissions() {
    }
}
