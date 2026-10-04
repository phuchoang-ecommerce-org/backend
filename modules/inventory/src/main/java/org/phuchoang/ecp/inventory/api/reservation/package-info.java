/**
 * Internal module-integration commands for Inventory reservation state. This is deliberately not
 * an HTTP contract: Ordering's future adapter invokes it inside the documented local transaction.
 */
@org.springframework.modulith.NamedInterface("api")
package org.phuchoang.ecp.inventory.api.reservation;
