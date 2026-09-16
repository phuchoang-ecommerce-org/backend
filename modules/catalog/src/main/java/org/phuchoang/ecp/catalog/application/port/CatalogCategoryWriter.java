package org.phuchoang.ecp.catalog.application.port;

import org.phuchoang.ecp.catalog.application.command.CreateCategoryCommand;

/** Catalog-owned write port; it never exposes the catalog table to another module. */
public interface CatalogCategoryWriter {

    CreatedCategory create(CreateCategoryCommand command);

    record CreatedCategory(String path, int depth) {
    }
}
