/** Shipping bounded context, which owns fulfilment and carrier-facing shipment progression. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "identity::api", "ordering::api", "audit::api" }
)
package org.phuchoang.ecp.shipping;
