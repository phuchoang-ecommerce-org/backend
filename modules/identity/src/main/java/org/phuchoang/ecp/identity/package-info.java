/** Identity and access bounded context; other modules use only its {@code api} named interface for caller identity and authorization. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "shared-kernel::api" }
)
package org.phuchoang.ecp.identity;
