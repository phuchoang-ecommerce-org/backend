package org.phuchoang.ecp.identity.internal.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The non-disclosure pairs `BR-CUS-04` relies on must stay indistinguishable at the wire. */
class IdentityErrorsTest {

    @Test
    void credentialFailuresAreNotAuthenticated() {
        assertThat(IdentityErrors.invalidCredentials().errorCode().code()).isEqualTo("ECP-GEN-4010");
    }

    @Test
    void bothRefreshRejectionsShareOneCode() {
        assertThat(IdentityErrors.refreshTokenRejected().errorCode().code())
            .isEqualTo("ECP-GEN-4011")
            .isEqualTo(IdentityErrors.refreshTokenReused().errorCode().code());
    }

    @Test
    void ownershipAndAbsenceAreBothNotFound() {
        assertThat(IdentityErrors.addressNotFound().errorCode().code()).isEqualTo("ECP-GEN-4040");
        assertThat(IdentityErrors.accountNotFound().errorCode().code()).isEqualTo("ECP-GEN-4040");
        assertThat(IdentityErrors.invalidVerificationLink().errorCode().code()).isEqualTo("ECP-GEN-4040");
        assertThat(IdentityErrors.invalidResetLink().errorCode().code()).isEqualTo("ECP-GEN-4040");
    }
}
