/** Ordering bounded context, owner of order lifecycle and the order-placement collaboration. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "identity::api", "cart::api", "inventory::api", "promotion::api" }
)
package org.phuchoang.ecp.ordering;
