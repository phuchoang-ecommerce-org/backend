package org.phuchoang.ecp.catalog.api.facade;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.api.error.DomainException;
import org.phuchoang.ecp.catalog.api.error.GenErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogApiErrorsTest {

    @Test
    void preservesThePublicErrorCodeAndDetail() {
        assertThatThrownBy(() -> CatalogApiErrors.translate((Runnable) () -> {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Product not found.");
        })).isInstanceOf(DomainException.class)
            .hasMessage("Product not found.")
            .satisfies(exception -> assertThat(((DomainException) exception).errorCode())
                .isEqualTo(GenErrorCode.NOT_FOUND));
    }
}
