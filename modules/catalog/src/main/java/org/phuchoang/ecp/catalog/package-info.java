/**
 * Product catalog module. Its public contract is exposed only through {@link
 * org.phuchoang.ecp.catalog.api}; internal code may depend on the shared-kernel and identity
 * public APIs. Sprint 06 cache namespaces are {@code category-tree}, {@code
 * category-listing:{id}}, {@code variant:{id}}, and {@code cat:product:{id}}; the storefront
 * uses the same strings as revalidation tags.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared-kernel::api", "identity::api" }
)
package org.phuchoang.ecp.catalog;
