package org.phuchoang.ecp.config;

import org.phuchoang.ecp.sharedkernel.api.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.HmacCursorCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition-root wiring for the active cursor HMAC key and one rotation key. */
@Configuration
public class CursorConfig {

    @Bean
    CursorCodec cursorCodec(@Value("${ecp.cursor.active-key-id}") String activeKeyId,
            @Value("${ecp.cursor.active-key}") String activeKey,
            @Value("${ecp.cursor.previous-key-id:}") String previousKeyId,
            @Value("${ecp.cursor.previous-key:}") String previousKey) {
        CursorSigningKey active = CursorSigningKey.utf8(activeKeyId, required(activeKey, "ECP_CURSOR_ACTIVE_KEY"));
        if (previousKeyId == null || previousKeyId.isBlank()) {
            if (previousKey != null && !previousKey.isBlank()) {
                throw new IllegalStateException("ECP_CURSOR_PREVIOUS_KEY_ID is required with ECP_CURSOR_PREVIOUS_KEY.");
            }
            return new HmacCursorCodec(active, null);
        }
        return new HmacCursorCodec(active,
            CursorSigningKey.utf8(previousKeyId, required(previousKey, "ECP_CURSOR_PREVIOUS_KEY")));
    }

    private static String required(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must be configured with a 32-byte-or-longer secret.");
        }
        return value;
    }
}
