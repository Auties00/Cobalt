package com.github.auties00.cobalt.wire.linked.contact;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Optional;

/**
 * The registration state of an account's username.
 *
 * <p>WhatsApp lets an account reserve a username before activating it, so a username is either
 * reserved (held but not yet reachable) or active (reachable by {@code @username}). The get-username
 * relay reply carries the current state token, which Cobalt persists so the state can be reported
 * without re-querying the relay.
 */
@ProtobufEnum(name = "UsernameState")
public enum UsernameState {
    /**
     * The username is reserved/held but not yet active; relay token {@code "RESERVED"}.
     */
    RESERVED(0),

    /**
     * The username is active and reachable; relay token {@code "ACTIVE"}.
     */
    ACTIVE(1);

    /**
     * The protobuf-encoded index used to persist this enum.
     */
    final int index;

    /**
     * Constructs a username-state variant bound to its protobuf index.
     *
     * @param index the protobuf index
     */
    UsernameState(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Resolves the username-state variant for a get-username relay state token.
     *
     * <p>The {@code "RESERVED"} token maps to {@link #RESERVED}; the {@code "ACTIVE"} and legacy
     * {@code "SET"} tokens map to {@link #ACTIVE}. Any other or {@code null} token yields an empty
     * result so the caller leaves the state unset.
     *
     * @param token the relay state token, possibly {@code null}
     * @return the matching variant, or empty when the token is unrecognised
     */
    public static Optional<UsernameState> ofToken(String token) {
        return switch (token) {
            case "RESERVED" -> Optional.of(RESERVED);
            case "ACTIVE", "SET" -> Optional.of(ACTIVE);
            case null, default -> Optional.empty();
        };
    }

    /**
     * Returns the protobuf-encoded index.
     *
     * @return the protobuf index
     */
    public int index() {
        return index;
    }
}
