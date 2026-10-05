package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.error.DomainException;
import org.phuchoang.ecp.catalog.api.error.GenErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;

import java.util.function.Supplier;

/** Translates application failures at Catalog's public boundary. */
final class CatalogApiErrors {

    private CatalogApiErrors() {
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
