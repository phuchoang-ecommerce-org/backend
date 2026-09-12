package org.phuchoang.ecp.identity.domain;

/**
 * The six fixed actor classes (`identity_role.code`, Permission Matrix.md §2.1, Solution
 * Architecture.md §4). A caller may hold more than one; authority is the union.
 */
public enum RoleCode {
    GUEST, CUSTOMER, STAFF, WAREHOUSE_OPERATOR, CUSTOMER_SUPPORT, ADMINISTRATOR
}
