/** Cart bounded context, which owns customer and guest shopping-basket state. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "shared-kernel::api", "identity::api", "catalog::api", "promotion::api" }
)
package org.phuchoang.ecp.cart;
