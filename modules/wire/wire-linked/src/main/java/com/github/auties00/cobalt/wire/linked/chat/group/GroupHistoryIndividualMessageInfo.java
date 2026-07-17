package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Stores per-message metadata for a message that was received as part of a
 * group history bundle.
 *
 * <p>When a new participant joins a group with "share group history" enabled,
 * they receive a bundle of recent messages. Each message in that bundle is
 * tracked individually by this class, which records the original message key
 * and whether the message was subsequently edited after being received as
 * history.
 *
 * <p>This information allows the client to distinguish between messages that
 * were part of the original history bundle and messages that arrived through
 * normal delivery, and to detect edits that occurred after the bundle was
 * processed.
 *
 * @see MessageKey
 * @see GroupHistoryBundleInfo
 */
@ProtobufMessage(name = "GroupHistoryIndividualMessageInfo")
public final class GroupHistoryIndividualMessageInfo {
    /**
     * The message key identifying the specific message within the history
     * bundle, or {@code null} if not available.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey bundleMessageKey;

    /**
     * Whether this message was edited after being received as part of a
     * history bundle. A {@code null} value is treated as {@code false}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean editedAfterReceivedAsHistory;


    /**
     * Constructs a new {@code GroupHistoryIndividualMessageInfo} with the
     * specified bundle message key and edit flag.
     *
     * @param bundleMessageKey           the key of the message in the bundle,
     *                                   or {@code null}
     * @param editedAfterReceivedAsHistory whether the message was edited after
     *                                   being received as history, or
     *                                   {@code null} to indicate unknown
     */
    GroupHistoryIndividualMessageInfo(MessageKey bundleMessageKey, Boolean editedAfterReceivedAsHistory) {
        this.bundleMessageKey = bundleMessageKey;
        this.editedAfterReceivedAsHistory = editedAfterReceivedAsHistory;
    }

    /**
     * Returns the message key identifying the specific message within the
     * history bundle, if available.
     *
     * @return an {@code Optional} containing the bundle message key, or empty
     *         if not available
     */
    public Optional<MessageKey> bundleMessageKey() {
        return Optional.ofNullable(bundleMessageKey);
    }

    /**
     * Returns whether this message was edited after being received as part of
     * a group history bundle.
     *
     * <p>Returns {@code false} if the flag has not been set (i.e., the
     * underlying value is {@code null}).
     *
     * @return {@code true} if the message was edited after being received as
     *         history, {@code false} otherwise
     */
    public boolean editedAfterReceivedAsHistory() {
        return editedAfterReceivedAsHistory != null && editedAfterReceivedAsHistory;
    }

    /**
     * Sets the message key identifying the specific message within the
     * history bundle.
     *
     * @param bundleMessageKey the message key to set, or {@code null} to clear
     */
    public void setBundleMessageKey(MessageKey bundleMessageKey) {
        this.bundleMessageKey = bundleMessageKey;
    }

    /**
     * Sets whether this message was edited after being received as part of a
     * history bundle.
     *
     * @param editedAfterReceivedAsHistory {@code true} if the message was
     *                                     edited, {@code false} or
     *                                     {@code null} otherwise
     */
    public void setEditedAfterReceivedAsHistory(Boolean editedAfterReceivedAsHistory) {
        this.editedAfterReceivedAsHistory = editedAfterReceivedAsHistory;
    }
}
