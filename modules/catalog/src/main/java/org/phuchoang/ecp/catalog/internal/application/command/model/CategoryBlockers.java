package org.phuchoang.ecp.catalog.internal.application.command.model;

/** Dependent counts used to reject deletion of a non-empty category under {@code BR-CAT-03}. */
public record CategoryBlockers(long productCount, long childCategoryCount) { }
