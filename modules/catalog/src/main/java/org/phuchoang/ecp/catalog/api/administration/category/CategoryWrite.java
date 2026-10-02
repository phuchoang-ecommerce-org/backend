package org.phuchoang.ecp.catalog.api.administration.category;
import java.util.UUID;

/** Administrator-supplied mutable category fields for {@code UC-ADM-02}. */
public record CategoryWrite(String name, UUID parentId, String imageUrl, Integer sortOrder, Boolean featured) { }
