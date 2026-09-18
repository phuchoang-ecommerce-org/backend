package org.phuchoang.ecp.web.common.security;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

/** Everything a controller needs to know about the request beyond its body: who is calling, under which correlation id. */
public record RequestContext(UUID correlationId, IdentityActor caller) {
}
