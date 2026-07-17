package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Strength classifier of a Click-to-WhatsApp (CTWA) Facebook access token.
 *
 * <p>The CTWA email-recovery flow exchanges a user-supplied verification
 * code for a Facebook Graph API bearer token. The relay sometimes annotates
 * the token with a strength marker so the client can decide whether the
 * token is suitable for sensitive Ads-Manager operations: a {@link #STRONG}
 * token has been freshly minted from a successful recovery code and may be
 * used to sign expensive operations, while a {@link #WEAK} token has been
 * derived from a longer-lived session and should only be used for read-only
 * surfaces.
 */
@ProtobufEnum
public enum CtwaAdTokenStrength {
    /**
     * The token was minted from a fresh recovery code and is suitable for
     * sensitive operations.
     */
    STRONG(0),

    /**
     * The token was derived from a longer-lived session and should only be
     * used for read-only surfaces.
     */
    WEAK(1);

    /**
     * The protobuf wire-format index associated with this strength value.
     */
    final int index;

    /**
     * Constructs a new {@code CtwaAdTokenStrength} with the supplied
     * protobuf index.
     *
     * @param index the protobuf wire-format index
     */
    CtwaAdTokenStrength(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the protobuf wire-format index associated with this value.
     *
     * @return the protobuf wire-format index
     */
    public int index() {
        return index;
    }
}
