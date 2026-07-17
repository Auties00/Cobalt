package com.github.auties00.cobalt.wire.linked.message.text;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that applies an emoji reaction to another message.
 *
 * <p>Reactions are lightweight expressions attached to an existing message,
 * identified by its {@link MessageKey}. Sending a reaction with a non-empty
 * text sets or replaces the reaction from the sender, while sending an
 * empty text removes it. Clients render reactions as a collapsed row of
 * emojis under the target message along with the count of senders for
 * each emoji.
 */
@ProtobufMessage(name = "Message.ReactionMessage")
public final class ReactionMessage implements Message {
    /**
     * The key identifying the message this reaction is applied to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The reaction emoji as a string, or an empty string to remove the
     * sender's previous reaction.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * An optional grouping key used by the server to cluster reactions
     * that should be aggregated together.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String groupingKey;

    /**
     * The timestamp, in milliseconds, at which the sender applied this
     * reaction locally.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;


    /**
     * Constructs a new reaction message with the supplied fields.
     *
     * @param key                the key of the target message
     * @param text               the reaction emoji, or empty to remove a previous one
     * @param groupingKey        the optional grouping key
     * @param senderTimestampMs  the local send timestamp
     */
    ReactionMessage(MessageKey key, String text, String groupingKey, Instant senderTimestampMs) {
        this.key = key;
        this.text = text;
        this.groupingKey = groupingKey;
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Returns the key of the message this reaction is applied to, if present.
     *
     * @return an {@link Optional} containing the target {@link MessageKey},
     *         or {@link Optional#empty()} if no target has been set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the reaction emoji, if present.
     *
     * <p>An empty string indicates that the sender is removing their
     * previous reaction; a {@code null} value indicates the field has
     * not been set.
     *
     * @return an {@link Optional} containing the reaction text,
     *         or {@link Optional#empty()} if none has been set
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the optional server-side grouping key, if present.
     *
     * @return an {@link Optional} containing the grouping key,
     *         or {@link Optional#empty()} if none has been set
     */
    public Optional<String> groupingKey() {
        return Optional.ofNullable(groupingKey);
    }

    /**
     * Returns the local timestamp at which the sender applied this
     * reaction, if present.
     *
     * @return an {@link Optional} containing the {@link Instant} captured
     *         by the sender, or {@link Optional#empty()} if none has been set
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Sets the key of the message this reaction is applied to.
     *
     * @param key the target message key, or {@code null} to clear
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the reaction emoji text.
     *
     * @param text the reaction emoji, an empty string to remove the
     *             sender's previous reaction, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the optional server-side grouping key.
     *
     * @param groupingKey the grouping key, or {@code null} to clear
     */
    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    /**
     * Sets the local timestamp at which the sender applied this reaction.
     *
     * @param senderTimestampMs the send timestamp, or {@code null} to clear
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }
}
