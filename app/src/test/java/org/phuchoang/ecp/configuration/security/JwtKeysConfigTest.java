package org.phuchoang.ecp.configuration.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Environment policy for key material: files win, the dev fallback is explicit, otherwise fail fast. */
class JwtKeysConfigTest {

    @Test
    void configuredKeyFilesSelectTheFileProvider() {
        assertThat(JwtKeysConfig.keyProvider(new JwtProperties("/keys/private.pem", "/keys/public.pem", true)))
            .isInstanceOf(FileJwtKeyProvider.class);
    }

    @Test
    void theDevelopmentFallbackRequiresTheExplicitOptIn() {
        assertThat(JwtKeysConfig.keyProvider(new JwtProperties("", "", true)))
            .isInstanceOf(DevelopmentJwtKeyProvider.class);
        assertThat(new DevelopmentJwtKeyProvider().load().getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void productionLikeConfigurationWithoutKeysFailsAtStartup() {
        assertThatThrownBy(() -> JwtKeysConfig.keyProvider(new JwtProperties("", "", false)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ecp.jwt.private-key-path")
            .hasMessageContaining("generate-dev-keys");
    }

    @Test
    void halfConfiguredKeyPathsAreNotTreatedAsKeyFiles() {
        assertThat(new JwtProperties("/keys/private.pem", "", true).hasKeyFiles()).isFalse();
    }
}
