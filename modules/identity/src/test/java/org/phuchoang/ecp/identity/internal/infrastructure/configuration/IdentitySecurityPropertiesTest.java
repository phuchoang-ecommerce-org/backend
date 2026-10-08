package org.phuchoang.ecp.identity.internal.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class IdentitySecurityPropertiesTest {

    @Test
    void acceptsValidTokenAndPasswordSettings() {
        assertThat(new IdentityTokenProperties("ecp-api", 900).accessTokenTtlSeconds()).isEqualTo(900);
        assertThat(new PasswordHashingProperties(16, 32, 1, 19_456, 2).argon2MemoryKib()).isEqualTo(19_456);
    }

    @Test
    void rejectsUnsafeTokenAndPasswordSettings() {
        assertThatIllegalArgumentException().isThrownBy(() -> new IdentityTokenProperties("", 900));
        assertThatIllegalArgumentException().isThrownBy(() -> new PasswordHashingProperties(16, 32, 1, 0, 2));
    }
}
