/** Audit bounded context. Its eventual public API records immutable business-action evidence without exposing internals. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "shared-kernel::api", "identity::api" }
)
package org.phuchoang.ecp.audit;
