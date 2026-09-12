package org.phuchoang.ecp.identity.application.internal;

import org.phuchoang.ecp.identity.domain.RoleCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.phuchoang.ecp.identity.domain.RoleCode.ADMINISTRATOR;
import static org.phuchoang.ecp.identity.domain.RoleCode.CUSTOMER;
import static org.phuchoang.ecp.identity.domain.RoleCode.CUSTOMER_SUPPORT;
import static org.phuchoang.ecp.identity.domain.RoleCode.GUEST;
import static org.phuchoang.ecp.identity.domain.RoleCode.STAFF;
import static org.phuchoang.ecp.identity.domain.RoleCode.WAREHOUSE_OPERATOR;

/**
 * The `CUS` (Customer &amp; Identity) rows of `04-shared/Permission Matrix.md` §5.1, verbatim —
 * every operation the domain defines, even though only five are wired to a controller this
 * sprint. Encoding the whole table now is cheap and avoids relitigating it next sprint.
 *
 * <p>Ownership (the matrix's "Own" column) is enforced by each application service against the
 * specific resource (e.g. "this is the caller's own session"), not by this table — this table
 * answers only "does this role reach this operation at all" (`NFR-SEC-01`).
 */
public final class PermissionMatrix {

    public static final String RESEND_EMAIL_VERIFICATION = "resendEmailVerification";
    public static final String VERIFY_EMAIL_ADDRESS = "verifyEmailAddress";
    public static final String SEARCH_ACCOUNTS = "searchAccounts";
    public static final String REGISTER_ACCOUNT = "registerAccount";
    public static final String GET_OWN_ACCOUNT = "getOwnAccount";
    public static final String UPDATE_OWN_PROFILE = "updateOwnProfile";
    public static final String LIST_OWN_ADDRESSES = "listOwnAddresses";
    public static final String ADD_OWN_ADDRESS = "addOwnAddress";
    public static final String REMOVE_OWN_ADDRESS = "removeOwnAddress";
    public static final String GET_OWN_ADDRESS = "getOwnAddress";
    public static final String REPLACE_OWN_ADDRESS = "replaceOwnAddress";
    public static final String CHANGE_OWN_PASSWORD = "changeOwnPassword";
    public static final String REQUEST_PASSWORD_RESET = "requestPasswordReset";
    public static final String COMPLETE_PASSWORD_RESET = "completePasswordReset";
    public static final String RENEW_SESSION = "renewSession";
    public static final String END_ALL_OWN_SESSIONS = "endAllOwnSessions";
    public static final String LOG_IN = "logIn";
    public static final String LOG_OUT = "logOut";

    private static final Set<RoleCode> ANY_AUTHENTICATED =
        EnumSet.of(CUSTOMER, STAFF, WAREHOUSE_OPERATOR, CUSTOMER_SUPPORT, ADMINISTRATOR);

    private static final Map<String, Set<RoleCode>> ALLOWED_ROLES = Map.ofEntries(
        Map.entry(RESEND_EMAIL_VERIFICATION, EnumSet.of(GUEST, CUSTOMER)),
        Map.entry(VERIFY_EMAIL_ADDRESS, EnumSet.of(GUEST)),
        Map.entry(SEARCH_ACCOUNTS, EnumSet.of(CUSTOMER_SUPPORT, ADMINISTRATOR)),
        Map.entry(REGISTER_ACCOUNT, EnumSet.of(GUEST)),
        Map.entry(GET_OWN_ACCOUNT, ANY_AUTHENTICATED),
        Map.entry(UPDATE_OWN_PROFILE, ANY_AUTHENTICATED),
        Map.entry(LIST_OWN_ADDRESSES, EnumSet.of(CUSTOMER)),
        Map.entry(ADD_OWN_ADDRESS, EnumSet.of(CUSTOMER)),
        Map.entry(REMOVE_OWN_ADDRESS, EnumSet.of(CUSTOMER)),
        Map.entry(GET_OWN_ADDRESS, EnumSet.of(CUSTOMER)),
        Map.entry(REPLACE_OWN_ADDRESS, EnumSet.of(CUSTOMER)),
        Map.entry(CHANGE_OWN_PASSWORD, ANY_AUTHENTICATED),
        Map.entry(REQUEST_PASSWORD_RESET, EnumSet.of(GUEST)),
        Map.entry(COMPLETE_PASSWORD_RESET, EnumSet.of(GUEST)),
        Map.entry(RENEW_SESSION, ANY_AUTHENTICATED),
        Map.entry(END_ALL_OWN_SESSIONS, ANY_AUTHENTICATED),
        Map.entry(LOG_IN, EnumSet.of(GUEST)),
        Map.entry(LOG_OUT, ANY_AUTHENTICATED));

    private PermissionMatrix() {
    }

    static Set<RoleCode> allowedRoles(String operationId) {
        Set<RoleCode> allowed = ALLOWED_ROLES.get(operationId);
        if (allowed == null) {
            throw new IllegalArgumentException("No permission-matrix row declared for operation " + operationId);
        }
        return allowed;
    }
}
