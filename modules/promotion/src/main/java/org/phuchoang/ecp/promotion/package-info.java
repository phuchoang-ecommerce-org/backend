/** Promotion bounded context, which owns eligibility, discount, voucher, and redemption rules. */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
    allowedDependencies = { "identity::api" }
)
package org.phuchoang.ecp.promotion;
