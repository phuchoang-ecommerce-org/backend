package org.phuchoang.ecp.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.ErrorCode;
import org.phuchoang.ecp.sharedkernel.api.FieldErrorCodes;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * The single {@code @RestControllerAdvice} producing {@code application/problem+json} for every
 * error this API returns (Integration Contract.md §4.1, Backend Architecture.md §6.3). No handler
 * ever leaks a stack trace, an exception type, or a SQL fragment into {@code detail} (§4.5 rule 3).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Problem> handleDomain(DomainException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.errorCode();
        return problemResponse(errorCode.code(), errorCode.title(), errorCode.httpStatus(),
            exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Problem> handleValidation(ValidationException exception, HttpServletRequest request) {
        return problemResponse(GenErrorCode.VALIDATION_FAILED.code(), GenErrorCode.VALIDATION_FAILED.title(),
            GenErrorCode.VALIDATION_FAILED.httpStatus(), "See errors for the failing fields.", request,
            exception.errors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleBeanValidation(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldError(
                fieldError.getField(),
                fieldError.getCode() != null && fieldError.getCode().equals("NotNull")
                    ? FieldErrorCodes.REQUIRED
                    : FieldErrorCodes.CONSTRAINT_VIOLATED,
                fieldError.getDefaultMessage()))
            .toList();
        return problemResponse(GenErrorCode.VALIDATION_FAILED.code(), GenErrorCode.VALIDATION_FAILED.title(),
            GenErrorCode.VALIDATION_FAILED.httpStatus(), "See errors for the failing fields.", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldError> errors = exception.getConstraintViolations().stream()
            .map(violation -> new FieldError(
                violation.getPropertyPath().toString(),
                FieldErrorCodes.CONSTRAINT_VIOLATED,
                violation.getMessage()))
            .toList();
        return problemResponse(GenErrorCode.VALIDATION_FAILED.code(), GenErrorCode.VALIDATION_FAILED.title(),
            GenErrorCode.VALIDATION_FAILED.httpStatus(), "See errors for the failing fields.", request, errors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Problem> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return problemResponse(GenErrorCode.VALIDATION_FAILED.code(), GenErrorCode.VALIDATION_FAILED.title(),
            GenErrorCode.VALIDATION_FAILED.httpStatus(), "The request body or parameters could not be read.",
            request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Problem> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return problemResponse(GenErrorCode.NOT_FOUND.code(), GenErrorCode.NOT_FOUND.title(),
            GenErrorCode.NOT_FOUND.httpStatus(), null, request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnmapped(Exception exception, HttpServletRequest request) {
        // ECP-GEN-5000 is a fallback, not a designed outcome (Error Codes.md §3.1) — logged at WARN
        // so a missing mapping is discoverable rather than merely tidy (Backend Architecture.md §6.3).
        log.warn("Unmapped exception reached GlobalExceptionHandler for {}", request.getRequestURI(), exception);
        return problemResponse(GenErrorCode.UNMAPPED_ERROR.code(), GenErrorCode.UNMAPPED_ERROR.title(),
            GenErrorCode.UNMAPPED_ERROR.httpStatus(), null, request, List.of());
    }

    private ResponseEntity<Problem> problemResponse(String code, String title, int status, String detail,
            HttpServletRequest request, List<FieldError> errors) {
        Problem problem = Problem.of(code, title, status, detail, request.getRequestURI(),
            CorrelationIdFilter.currentCorrelationId(), errors);
        return ResponseEntity.status(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            .body(problem);
    }
}
