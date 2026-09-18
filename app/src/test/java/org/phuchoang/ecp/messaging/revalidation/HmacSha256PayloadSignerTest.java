package org.phuchoang.ecp.messaging.revalidation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSha256PayloadSignerTest {

    @Test
    void producesTheSha256PrefixedHexHmacOverTheExactBytes() {
        PayloadSigner signer = new HmacSha256PayloadSigner(new RevalidationProperties("http://x", "key", "g"));

        // RFC 4231-style known answer for HMAC-SHA256("key", "The quick brown fox jumps over the lazy dog")
        assertThat(signer.sign("The quick brown fox jumps over the lazy dog"))
            .isEqualTo("sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
        assertThat(signer.sign("body")).isNotEqualTo(signer.sign("body "));
    }
}
