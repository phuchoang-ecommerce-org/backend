/** Shipping bounded context, which owns fulfilment and carrier-facing shipment progression. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "shared-kernel::api", "identity::api" }
)
package org.phuchoang.ecp.shipping;
