/** Payment bounded context, which owns payment attempts, provider interaction, and refund state. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "identity::api" }
)
package org.phuchoang.ecp.payment;
