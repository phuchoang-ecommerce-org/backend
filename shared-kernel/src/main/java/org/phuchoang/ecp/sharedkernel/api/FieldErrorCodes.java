package org.phuchoang.ecp.sharedkernel.api;

/**
 * The field-level sub-codes carried in {@code ECP-GEN-4000}'s {@code errors} array (Integration
 * Contract.md §4.3, Error Codes.md §4). This space is explicitly "open, not enumerated" — only the
 * two codes named as examples in every normative source are declared here; a new one is added the
 * same way a top-level code is, when the constraint it reports is implemented. {@code ECP-GEN-4001}
 * is deliberately absent — Error Codes.md §4 notes it is unassigned in every source and nothing
 * should assume it exists.
 */
public final class FieldErrorCodes {

    /** The field is required and was absent. */
    public static final String REQUIRED = "ECP-GEN-4002";

    /** The field failed a range or length constraint. */
    public static final String CONSTRAINT_VIOLATED = "ECP-GEN-4003";

    private FieldErrorCodes() {
    }
}
