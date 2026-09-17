/** Inventory bounded context, the authoritative owner of sellable stock and reservation state. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "shared-kernel::api", "identity::api" }
)
package org.phuchoang.ecp.inventory;
