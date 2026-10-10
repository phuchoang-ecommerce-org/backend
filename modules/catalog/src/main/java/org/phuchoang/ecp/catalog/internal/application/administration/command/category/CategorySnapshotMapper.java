package org.phuchoang.ecp.catalog.internal.application.administration.command.category;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;

/** Maps the Category aggregate to its application-owned administration result. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategorySnapshotMapper {

    CategorySnapshot categorySnapshot(Category category);
}
