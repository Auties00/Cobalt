package com.github.auties00.cobalt.wire.linked.message.system.appstate;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Peer message sent by a companion device to request missing app state
 * sync keys from the primary device.
 *
 * <p>A companion that encounters an app state mutation encrypted with a
 * key it does not own (for example because the share message was lost or
 * because it was linked after the key was first distributed) sends this
 * request, listing the {@link AppStateSyncKeyId}s it needs. The primary
 * device replies with an {@link AppStateSyncKeyShare} containing the
 * matching {@link AppStateSyncKey} entries.
 */
@ProtobufMessage(name = "Message.AppStateSyncKeyRequest")
public final class AppStateSyncKeyRequest implements Message {
    /**
     * The identifiers of the app state sync keys the companion is
     * requesting from the primary device.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<AppStateSyncKeyId> keyIds;


    /**
     * Creates a new request for the given app state sync key identifiers.
     *
     * @param keyIds the identifiers of the keys to request, or
     *               {@code null} if unset
     */
    AppStateSyncKeyRequest(List<AppStateSyncKeyId> keyIds) {
        this.keyIds = keyIds;
    }

    /**
     * Returns the identifiers of the app state sync keys being requested.
     *
     * @return an unmodifiable {@link List} of {@link AppStateSyncKeyId}
     *         entries, or an empty list if no identifiers have been set
     */
    public List<AppStateSyncKeyId> keyIds() {
        return keyIds == null ? List.of() : Collections.unmodifiableList(keyIds);
    }

    /**
     * Sets the identifiers of the app state sync keys being requested.
     *
     * @param keyIds the identifiers to request, or {@code null} to clear
     */
    public void setKeyIds(List<AppStateSyncKeyId> keyIds) {
        this.keyIds = keyIds;
    }
}
