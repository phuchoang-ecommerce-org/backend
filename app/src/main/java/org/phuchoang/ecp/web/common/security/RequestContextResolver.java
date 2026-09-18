package org.phuchoang.ecp.web.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The one place the HTTP/security representation of identity (a JWT) is translated into the
 * bounded contexts' caller representation, and correlation metadata is attached. Controllers never
 * interpret claims themselves; if claim names change, only the resolver does.
 */
public interface RequestContextResolver {

    /** @param jwt the authenticated principal, or {@code null} for an anonymous request (→ {@code GUEST}) */
    RequestContext resolve(Jwt jwt);
}
