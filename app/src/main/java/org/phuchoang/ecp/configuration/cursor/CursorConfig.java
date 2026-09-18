package org.phuchoang.ecp.configuration.cursor;

import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.cursor.HmacCursorCodec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition-root wiring for the cursor codec. Fails fast at startup: a missing active key, or a
 * rotation key without its id, is a configuration error, not something to discover on the first
 * paginated request.
 */
@Configuration
@EnableConfigurationProperties(CursorProperties.class)
public class CursorConfig {

    @Bean
    CursorCodec cursorCodec(CursorProperties properties) {
        CursorSigningKey active = CursorSigningKey.utf8(properties.activeKeyId(),
            required(properties.activeKey(), "ECP_CURSOR_ACTIVE_KEY"));
        if (properties.previousKeyId().isBlank()) {
            if (!properties.previousKey().isBlank()) {
                throw new IllegalStateException("ECP_CURSOR_PREVIOUS_KEY_ID is required with ECP_CURSOR_PREVIOUS_KEY.");
            }
            return new HmacCursorCodec(active, null);
        }
        return new HmacCursorCodec(active, CursorSigningKey.utf8(properties.previousKeyId(),
            required(properties.previousKey(), "ECP_CURSOR_PREVIOUS_KEY")));
    }

    private static String required(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be configured with a 32-byte-or-longer secret.");
        }
        return value;
    }
}
