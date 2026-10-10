package org.phuchoang.ecp.catalog.internal.application.administration.command.product;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Maps the Product aggregate to application-owned administration results. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductSnapshotMapper {

    ProductSnapshot productSnapshot(Product product);

    VariantSnapshot variantSnapshot(Product.Variant variant);

    ImageSnapshot imageSnapshot(Product.Image image);

    default OffsetDateTime offsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
