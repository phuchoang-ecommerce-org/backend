package org.phuchoang.ecp.catalog.internal.application.search.query;

/** Technical read-store failure translated at the query boundary to ECP-GEN-5030. */
public class SearchStoreUnavailableException extends RuntimeException {

    public SearchStoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
