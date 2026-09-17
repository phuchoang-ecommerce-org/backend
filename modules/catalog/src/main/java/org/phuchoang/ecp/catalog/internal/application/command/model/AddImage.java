package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.UUID;

/** Normalized command data for adding one aggregate-owned product image. */
public record AddImage(UUID id, String url, String altText, int sortOrder) { }
