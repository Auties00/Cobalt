package com.github.auties00.cobalt.wire.cloud;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * The action that produced an {@code smb_app_state_sync} contact entry.
 *
 * <p>Each synced contact entry was produced by an {@link #ADD}, an {@link #UPDATE}, or a
 * {@link #REMOVE}. The {@link #UNKNOWN} constant guards against tokens this client does not yet model.
 *
 * <p>The wire tokens are lowercase; each constant maps to its lowercase token through {@link #token()},
 * for example {@link #ADD} serializes as {@code add}.
 */
@ProtobufEnum
public enum CloudAppStateSyncAction {
    /**
     * An action that this client does not recognise. Resolved for any token outside the modelled set so
     * that an unexpected value never fails decoding.
     */
    UNKNOWN(0, "unknown"),

    /**
     * The contact was added.
     */
    ADD(1, "add"),

    /**
     * The contact was updated.
     */
    UPDATE(2, "update"),

    /**
     * The contact was removed.
     */
    REMOVE(3, "remove");

    /**
     * The protobuf-assigned numeric index for this action.
     */
    final int index;

    /**
     * The lowercase WhatsApp wire token for this action.
     */
    final String token;

    /**
     * Constructs a {@code CloudAppStateSyncAction} with the specified protobuf index and wire token.
     *
     * @param index the protobuf enum index
     * @param token the lowercase wire token
     */
    CloudAppStateSyncAction(@ProtobufEnumIndex int index, String token) {
        this.index = index;
        this.token = token;
    }

    /**
     * Returns the {@code CloudAppStateSyncAction} matching the given wire token.
     *
     * <p>The lookup matches both the constant name and the lowercase wire token case-insensitively
     * against {@code input}; any unrecognised or {@code null} token resolves to {@link #UNKNOWN} so
     * that decoding never fails on an unexpected value.
     *
     * @param input the wire token, for example {@code "add"}, or {@code null}
     * @return the matching action, or {@link #UNKNOWN} when {@code input} matches no constant
     */
    public static CloudAppStateSyncAction of(String input) {
        if (input == null) {
            return UNKNOWN;
        }
        for (var value : values()) {
            if (value != UNKNOWN && (value.name().equalsIgnoreCase(input) || value.token.equalsIgnoreCase(input))) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the lowercase WhatsApp wire token for this action.
     *
     * @return the wire token, for example {@code "add"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the protobuf-assigned numeric index for this action.
     *
     * @return the protobuf enum index
     */
    public int index() {
        return index;
    }
}
