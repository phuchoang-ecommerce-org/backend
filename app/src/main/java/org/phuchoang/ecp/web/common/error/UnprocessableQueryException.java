package org.phuchoang.ecp.web.common.error;

/** A syntactically valid query whose documented parameter combination is semantically invalid. */
public class UnprocessableQueryException extends RuntimeException {

    public UnprocessableQueryException(String message) {
        super(message);
    }
}
