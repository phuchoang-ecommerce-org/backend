package org.phuchoang.ecp.identity.internal.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — `US-CUS-01` E2, `NFR-SEC-04` (interim policy; no strength policy is specified by R1). */
class PasswordPolicyTest {

    @Test
    void rejectsAPasswordShorterThanTheMinimumLength_NFR_SEC_04() {
        assertThat(PasswordPolicy.firstViolation("Ab1")).isPresent();
    }

    @Test
    void rejectsAPasswordWithNoUpperCaseLetter_NFR_SEC_04() {
        assertThat(PasswordPolicy.firstViolation("lowercase123")).isPresent();
    }

    @Test
    void rejectsAPasswordWithNoLowerCaseLetter_NFR_SEC_04() {
        assertThat(PasswordPolicy.firstViolation("UPPERCASE123")).isPresent();
    }

    @Test
    void rejectsAPasswordWithNoDigit_NFR_SEC_04() {
        assertThat(PasswordPolicy.firstViolation("NoDigitsHere")).isPresent();
    }

    @Test
    void acceptsAPasswordMeetingEveryCriterion_NFR_SEC_04() {
        assertThat(PasswordPolicy.satisfies("Str0ngPassword")).isTrue();
    }
}
