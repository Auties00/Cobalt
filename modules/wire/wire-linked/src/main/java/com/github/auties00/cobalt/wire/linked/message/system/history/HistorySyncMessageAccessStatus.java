package com.github.auties00.cobalt.wire.linked.message.system.history;

import com.github.auties00.cobalt.wire.linked.message.Message;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Reports whether the companion device has been granted full access to the
 * primary device's message history as part of a history sync.
 *
 * <p>Primary devices may decide to withhold the complete message history from
 * newly linked companions and deliver only a truncated recent window. This
 * payload carries the flag that communicates that decision to the companion so
 * that it can inform the user and adjust its local UI accordingly.
 */
@ProtobufMessage(name = "Message.HistorySyncMessageAccessStatus")
public final class HistorySyncMessageAccessStatus implements Message {
    /**
     * Whether the primary device has authorised full historical access for the
     * companion.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean completeAccessGranted;


    /**
     * Constructs a new message access status payload.
     *
     * @param completeAccessGranted whether complete access has been granted,
     *                              may be {@code null} which is interpreted as
     *                              {@code false}
     */
    HistorySyncMessageAccessStatus(Boolean completeAccessGranted) {
        this.completeAccessGranted = completeAccessGranted;
    }

    /**
     * Returns whether the primary device has granted the companion full access
     * to the message history.
     *
     * @return {@code true} if complete historical access was granted, or
     *         {@code false} when access is restricted or the flag was not set
     */
    public boolean completeAccessGranted() {
        return completeAccessGranted != null && completeAccessGranted;
    }

    /**
     * Sets whether the primary device has granted full historical access to
     * the companion.
     *
     * @param completeAccessGranted the new access-granted flag, may be
     *                              {@code null}
     */
    public void setCompleteAccessGranted(Boolean completeAccessGranted) {
        this.completeAccessGranted = completeAccessGranted;
    }
}
