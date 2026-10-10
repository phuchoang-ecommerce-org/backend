package org.phuchoang.ecp.identity.api.view;

import java.util.List;

/**
 * `components/schemas/identity.yaml#/Session`, minus the cookie half — see the Sprint 03 plan's
 * "Key design decisions": this backend is always a JSON API, never a cookie-issuing one.
 */
public record SessionResponse(String accessToken, String refreshToken, long expiresIn, boolean restricted,
        AccountView account, List<String> cartMergeNotices) {
    public SessionResponse {
        cartMergeNotices = List.copyOf(cartMergeNotices);
    }

    public SessionResponse(String accessToken, String refreshToken, long expiresIn, boolean restricted, AccountView account) {
        this(accessToken, refreshToken, expiresIn, restricted, account, List.of());
    }

}
