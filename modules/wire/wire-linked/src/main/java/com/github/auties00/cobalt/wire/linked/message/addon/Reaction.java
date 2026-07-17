package com.github.auties00.cobalt.wire.linked.message.addon;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents a reaction sent by a user in response to a previous message.
 *
 * <p>A reaction associates an emoji or short text with a target message via
 * its {@link MessageKey}. WhatsApp uses an empty text to signal that the
 * reaction has been removed, which lets clients efficiently synchronise the
 * current state of a user's reaction without needing separate delete events.
 *
 * <p>Reactions can also be grouped by a {@code groupingKey}, which allows
 * custom reaction flows such as emoji variants to collapse into a single
 * aggregated entry. Each reaction carries its sender-side timestamp and a
 * flag indicating whether the local user has already seen it.
 */
@ProtobufMessage(name = "Reaction")
public final class Reaction {
    /**
     * The message key of the message that is being reacted to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey key;

    /**
     * The emoji or text of the reaction. An empty value indicates that a
     * previously sent reaction has been removed.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * Optional identifier used to group variants of the same reaction
     * together, so that clients can aggregate counts correctly.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String groupingKey;

    /**
     * The instant at which the sender produced this reaction.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;

    /**
     * Whether this reaction has not yet been seen by the local user.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean unread;


    /**
     * Constructs a new reaction with the provided fields.
     *
     * @param key the key of the message being reacted to
     * @param text the emoji or text of the reaction, or empty to remove a previous reaction
     * @param groupingKey optional grouping identifier
     * @param senderTimestampMs when the sender produced the reaction
     * @param unread whether the reaction is still unread by the local user
     */
    Reaction(MessageKey key, String text, String groupingKey, Instant senderTimestampMs, Boolean unread) {
        this.key = key;
        this.text = text;
        this.groupingKey = groupingKey;
        this.senderTimestampMs = senderTimestampMs;
        this.unread = unread;
    }

    /**
     * Returns the message key of the message being reacted to.
     *
     * @return an {@link Optional} containing the target message key, or empty if not set
     */
    public Optional<MessageKey> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the emoji or text content of this reaction.
     *
     * <p>An empty string signals that the sender has removed a previously
     * sent reaction on the same message.
     *
     * @return an {@link Optional} containing the reaction text, or empty if not set
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns the optional grouping identifier used to aggregate variants
     * of the same reaction together.
     *
     * @return an {@link Optional} containing the grouping key, or empty if not set
     */
    public Optional<String> groupingKey() {
        return Optional.ofNullable(groupingKey);
    }

    /**
     * Returns the instant at which the sender produced this reaction.
     *
     * @return an {@link Optional} containing the sender timestamp, or empty if not set
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Returns whether this reaction is still unread by the local user.
     *
     * <p>Returns {@code false} both when the flag is explicitly unset and when
     * it is {@code false} on the wire.
     *
     * @return {@code true} if the reaction has not yet been seen, {@code false} otherwise
     */
    public boolean unread() {
        return unread != null && unread;
    }

    /**
     * Sets the message key of the message being reacted to.
     *
     * @param key the new target message key, or {@code null} to clear
     */
    public void setKey(MessageKey key) {
        this.key = key;
    }

    /**
     * Sets the emoji or text content of this reaction.
     *
     * @param text the new reaction text, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Sets the optional grouping identifier.
     *
     * @param groupingKey the new grouping key, or {@code null} to clear
     */
    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    /**
     * Sets the instant at which the sender produced this reaction.
     *
     * @param senderTimestampMs the new sender timestamp, or {@code null} to clear
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Sets whether this reaction is still unread by the local user.
     *
     * @param unread the new unread flag, or {@code null} to clear
     */
    public void setUnread(Boolean unread) {
        this.unread = unread;
    }
}
