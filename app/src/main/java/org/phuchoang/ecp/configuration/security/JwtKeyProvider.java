package org.phuchoang.ecp.configuration.security;

import java.security.KeyPair;

/** Where the RS256 signing keypair comes from — file-based key material, or a development fallback. */
interface JwtKeyProvider {

    KeyPair load();
}
