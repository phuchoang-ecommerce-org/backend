package org.phuchoang.ecp.identity.api.facade;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.error.DomainException;
import org.phuchoang.ecp.identity.api.error.GenErrorCode;
import org.phuchoang.ecp.identity.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.identity.internal.application.error.ApplicationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityApiErrorsTest {

    @Test
    void preservesThePublicErrorCodeAndDetail() {
        assertThatThrownBy(() -> IdentityApiErrors.translate((Runnable) () -> {
            throw new ApplicationException(ApplicationErrorCode.NOT_AUTHENTICATED, "Invalid credentials.");
        })).isInstanceOf(DomainException.class)
            .hasMessage("Invalid credentials.")
            .satisfies(exception -> assertThat(((DomainException) exception).errorCode())
                .isEqualTo(GenErrorCode.NOT_AUTHENTICATED));
    }
}
