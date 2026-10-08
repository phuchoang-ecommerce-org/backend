package org.phuchoang.ecp.identity.api.facade;

import org.phuchoang.ecp.identity.api.error.DomainException;
import org.phuchoang.ecp.identity.api.error.GenErrorCode;
import org.phuchoang.ecp.identity.internal.application.error.ApplicationException;

import java.util.function.Supplier;

/** Translates application failures at Identity's public facade boundary. */
final class IdentityApiErrors {

    private IdentityApiErrors() {
    }

    static <T> T translate(Supplier<T> action) {
        try {
            return action.get();
        } catch (ApplicationException exception) {
            throw publicException(exception);
        }
    }

    static void translate(Runnable action) {
        try {
            action.run();
        } catch (ApplicationException exception) {
            throw publicException(exception);
        }
    }

    private static DomainException publicException(ApplicationException exception) {
        return new DomainException(GenErrorCode.valueOf(exception.errorCode().name()), exception.getMessage());
    }
}
