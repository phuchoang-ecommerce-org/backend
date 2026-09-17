package org.phuchoang.ecp.identity.internal.application.internal;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.internal.application.CallerContext;
import org.phuchoang.ecp.identity.internal.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L1 — "permission-matrix cell asserted" (Sprint 03 backlog, every story), against
 * `04-shared/Permission Matrix.md` §5.1's `CUS` rows for the five operations this sprint wires.
 */
class PermissionMatrixAuthorizationServiceTest {

    private final AuthorizationService authorizationService = new PermissionMatrixAuthorizationService();

    @Test
    void registerAccountPermitsOnlyGuest() {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.REGISTER_ACCOUNT);
        assertDenied(customer(), PermissionMatrix.REGISTER_ACCOUNT);
    }

    @Test
    void verifyEmailAddressPermitsOnlyGuest() {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.VERIFY_EMAIL_ADDRESS);
        assertDenied(customer(), PermissionMatrix.VERIFY_EMAIL_ADDRESS);
    }

    @Test
    void resendEmailVerificationPermitsGuestAndCustomer() {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.RESEND_EMAIL_VERIFICATION);
        authorizationService.assertAuthorized(customer(), PermissionMatrix.RESEND_EMAIL_VERIFICATION);
        assertDenied(new CallerContext(UUID.randomUUID(), EnumSet.of(RoleCode.ADMINISTRATOR)),
            PermissionMatrix.RESEND_EMAIL_VERIFICATION);
    }

    @Test
    void logInPermitsOnlyGuest() {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.LOG_IN);
        assertDenied(customer(), PermissionMatrix.LOG_IN);
    }

    @Test
    void logOutPermitsAnyAuthenticatedRoleButNotGuest() {
        authorizationService.assertAuthorized(customer(), PermissionMatrix.LOG_OUT);
        authorizationService.assertAuthorized(new CallerContext(UUID.randomUUID(), EnumSet.of(RoleCode.ADMINISTRATOR)),
            PermissionMatrix.LOG_OUT);
        assertDenied(CallerContext.GUEST, PermissionMatrix.LOG_OUT);
    }

    private void assertDenied(CallerContext caller, String operationId) {
        assertThatThrownBy(() -> authorizationService.assertAuthorized(caller, operationId))
            .isInstanceOf(DomainException.class)
            .satisfies(e -> assertThat(((DomainException) e).errorCode().code()).isEqualTo("ECP-GEN-4030"));
    }

    private CallerContext customer() {
        return new CallerContext(UUID.randomUUID(), Set.of(RoleCode.CUSTOMER));
    }
}
