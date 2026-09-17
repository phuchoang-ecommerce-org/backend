package org.phuchoang.ecp.catalog.api.request;
import java.util.UUID;

/** Administrator-supplied mutable category fields for {@code UC-ADM-02}. */
public record CategoryWrite(String name, UUID parentId, String imageUrl, Integer sortOrder, Boolean featured) { }
