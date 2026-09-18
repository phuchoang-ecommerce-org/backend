package org.phuchoang.ecp.catalog.internal.application.command.product;

/** A requested publication-state transition; the status is validated by the domain, not the API. */
public record SetPublication(String publicationStatus, String reason) {
}
