package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Normalized command data for adding one purchasable product variant. */
public record AddVariant(UUID id, String sku, String name, BigDecimal amount, String currency,
        Map<String, String> options, Integer weightGrams, boolean active) { }
