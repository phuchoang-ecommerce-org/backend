/** Reporting bounded context, responsible for derived analytical read models rather than transactional ownership. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "identity::api" }
)
package org.phuchoang.ecp.reporting;
