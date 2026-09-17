package org.phuchoang.ecp.catalog.api.request;

/** Administrator-supplied product-image metadata; display order is owned by the product aggregate. */
public record ImageWrite(String url, String altText, Integer sortOrder) { }
