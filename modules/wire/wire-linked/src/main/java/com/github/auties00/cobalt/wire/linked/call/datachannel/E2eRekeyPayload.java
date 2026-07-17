package com.github.auties00.cobalt.wire.linked.call.datachannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * End-to-end rekey bundle published when a call's media keys must rotate.
 *
 * <p>WhatsApp rotates the per-call SRTP keys on participant join, leave,
 * and other lifecycle events. Each rekey carries one
 * {@link RekeyKeyEntry} per encryption domain
 * ({@link RekeyKeyType#AUDIO}, {@link RekeyKeyType#VIDEO},
 * {@link RekeyKeyType#APPDATA}); the receiver applies the new keys
 * atomically to keep the streams in sync.
 */
@ProtobufMessage(name = "E2eRekeyPayload")
public final class E2eRekeyPayload {
    /**
     * The per-domain key entries to install.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<RekeyKeyEntry> keys;

    /**
     * Constructs a new {@code E2eRekeyPayload}.
     *
     * @param keys the per-domain key entries
     */
    E2eRekeyPayload(List<RekeyKeyEntry> keys) {
        this.keys = keys;
    }

    /**
     * Returns the per-domain key entries.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<RekeyKeyEntry> keys() {
        return keys == null ? List.of() : Collections.unmodifiableList(keys);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof E2eRekeyPayload that
                && Objects.equals(this.keys, that.keys));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keys);
    }

    @Override
    public String toString() {
        return "E2eRekeyPayload[keys=" + keys() + ']';
    }
}
