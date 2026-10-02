package org.phuchoang.ecp.catalog.api.administration.product;

/** Requested publication-state transition and the reason recorded for it. */
public record PublicationWrite(String publicationStatus, String reason) { }
