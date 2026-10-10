package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;

/** Maps the adjusted aggregate state to the command-owned result. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AdjustStockMapper {

    @Mapping(target = "availableQuantity", expression = "java(stockItem.availableQuantity())")
    AdjustStockResult result(StockItem stockItem);
}
