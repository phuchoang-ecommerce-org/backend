package org.phuchoang.ecp.sharedkernel.api;

import java.util.List;
import java.util.UUID;

/**
 * Creates and verifies opaque, signed keyset continuation tokens.
 *
 * <p>A cursor is valid only for the {@link CursorContext} with which it was issued. Callers pass
 * typed sort positions rather than serialising database values themselves; this keeps token
 * encoding, signature verification, and key rotation independent of a persistence technology.
 */
public interface CursorCodec {

    /** Encodes a continuation position using the active signing key. */
    String encode(CursorContext context, List<CursorValue> sortValues, UUID tieBreaker);

    /** Verifies and decodes a continuation position for the expected listing context. */
    CursorPosition decode(String token, CursorContext expectedContext);
}
