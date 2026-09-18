package org.phuchoang.ecp.configuration.cursor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code ecp.cursor.*}: the active HMAC key for opaque pagination cursors and one rotation key. */
@ConfigurationProperties("ecp.cursor")
public record CursorProperties(@DefaultValue("cursor-v1") String activeKeyId, @DefaultValue("") String activeKey,
        @DefaultValue("") String previousKeyId, @DefaultValue("") String previousKey) {
}
