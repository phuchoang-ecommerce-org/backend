package org.phuchoang.ecp.catalog.api.facade;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.catalog.api.administration.category.CategoryWrite;
import org.phuchoang.ecp.catalog.api.administration.product.ImageWrite;
import org.phuchoang.ecp.catalog.api.administration.product.VariantWrite;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogDtoMapperTest {

    private final CatalogDtoMapper mapper = Mappers.getMapper(CatalogDtoMapper.class);

    @Test
    void mapsApiDefaultsIntoAdministrationCommands() {
        assertThat(mapper.addVariant(new VariantWrite("TSHIRT-M", null, new MoneyView(new BigDecimal("19.99"), "USD"),
            Map.of("size", "M"), 180, null)))
            .extracting(command -> command.name(), command -> command.amount(), command -> command.currency(),
                command -> command.active())
            .containsExactly("TSHIRT-M", new BigDecimal("19.99"), "USD", true);
        assertThat(mapper.addImage(new ImageWrite("https://example.test/t-shirt.jpg", "T-Shirt", null)).sortOrder())
            .isZero();
        assertThat(mapper.createCategory(new CategoryWrite("T-Shirts", UUID.randomUUID(), null, null, null)))
            .extracting(command -> command.sortOrder(), command -> command.featured())
            .containsExactly(0, false);
    }
}
