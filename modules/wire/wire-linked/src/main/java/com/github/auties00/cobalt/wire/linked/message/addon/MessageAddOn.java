package com.github.auties00.cobalt.wire.linked.message.addon;

import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents an add-on attached to an existing chat message.
 *
 * <p>Add-ons are supplementary payloads that extend, react to, or modify a
 * previously sent message without replacing it. WhatsApp uses this mechanism
 * to deliver reactions, poll updates, event responses, and in-chat pins as
 * lightweight follow-ups linked to a parent message by key.
 *
 * <p>Each add-on carries:
 * <ul>
 *   <li>The kind of add-on it represents, described by {@link MessageAddOnType}</li>
 *   <li>The actual payload, wrapped in a {@link LinkedMessageContainer}</li>
 *   <li>Timestamps describing when the sender produced the add-on and when the server acknowledged it</li>
 *   <li>The current delivery {@link MessageStatus}</li>
 *   <li>Optional expiry metadata carried by {@link MessageAddOnContextInfo}</li>
 *   <li>The {@link MessageKey} uniquely identifying this add-on</li>
 *   <li>A {@link LegacyMessageContainer} for compatibility with older clients</li>
 * </ul>
 */
@ProtobufMessage(name = "MessageAddOn")
public final class MessageAddOn {
    /**
     * The kind of add-on this entry represents.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    MessageAddOnType messageAddOnType;

    /**
     * The payload carried by this add-on, encoded as a standard
     * {@link LinkedMessageContainer} so any supported message type can be nested.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LinkedMessageContainer messageContainerAddOn;

    /**
     * The instant at which the sender produced the add-on on the originating device.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant senderTimestampMs;

    /**
     * The instant at which the WhatsApp server observed and acknowledged the add-on.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant serverTimestampMs;

    /**
     * The current delivery status of this add-on in the chat.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    MessageStatus status;

    /**
     * Optional context describing how long the add-on remains valid and how
     * its expiry is computed.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    MessageAddOnContextInfo addOnContextInfo;

    /**
     * The message key that uniquely identifies this add-on within the chat.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    MessageKey messageAddOnKey;

    /**
     * Optional legacy payload preserved for compatibility with older clients
     * that did not use the add-on framework.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    LegacyMessageContainer legacyMessage;


    /**
     * Constructs a new message add-on with the provided fields.
     *
     * @param messageAddOnType the kind of add-on
     * @param messageContainerAddOn the payload container
     * @param senderTimestampMs the timestamp when the sender produced the add-on
     * @param serverTimestampMs the timestamp when the server acknowledged the add-on
     * @param status the delivery status
     * @param addOnContextInfo optional expiry metadata
     * @param messageAddOnKey the message key identifying this add-on
     * @param legacyMessage optional legacy payload for older clients
     */
    MessageAddOn(MessageAddOnType messageAddOnType, LinkedMessageContainer messageContainerAddOn, Instant senderTimestampMs, Instant serverTimestampMs, MessageStatus status, MessageAddOnContextInfo addOnContextInfo, MessageKey messageAddOnKey, LegacyMessageContainer legacyMessage) {
        this.messageAddOnType = messageAddOnType;
        this.messageContainerAddOn = messageContainerAddOn;
        this.senderTimestampMs = senderTimestampMs;
        this.serverTimestampMs = serverTimestampMs;
        this.status = status;
        this.addOnContextInfo = addOnContextInfo;
        this.messageAddOnKey = messageAddOnKey;
        this.legacyMessage = legacyMessage;
    }

    /**
     * Returns the kind of add-on this entry represents.
     *
     * @return an {@link Optional} containing the add-on type, or empty if unspecified
     */
    public Optional<MessageAddOnType> messageAddOnType() {
        return Optional.ofNullable(messageAddOnType);
    }

    /**
     * Returns the payload carried by this add-on.
     *
     * @return an {@link Optional} containing the message container, or empty if no payload was provided
     */
    public Optional<LinkedMessageContainer> messageAddOn() {
        return Optional.ofNullable(messageContainerAddOn);
    }

    /**
     * Returns the instant at which the sender produced this add-on.
     *
     * @return an {@link Optional} containing the sender timestamp, or empty if unspecified
     */
    public Optional<Instant> senderTimestampMs() {
        return Optional.ofNullable(senderTimestampMs);
    }

    /**
     * Returns the instant at which the WhatsApp server acknowledged this add-on.
     *
     * @return an {@link Optional} containing the server timestamp, or empty if unspecified
     */
    public Optional<Instant> serverTimestampMs() {
        return Optional.ofNullable(serverTimestampMs);
    }

    /**
     * Returns the current delivery status of this add-on.
     *
     * @return an {@link Optional} containing the status, or empty if unspecified
     */
    public Optional<MessageStatus> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the optional expiry context for this add-on.
     *
     * @return an {@link Optional} containing the context info, or empty if none was set
     */
    public Optional<MessageAddOnContextInfo> addOnContextInfo() {
        return Optional.ofNullable(addOnContextInfo);
    }

    /**
     * Returns the message key that uniquely identifies this add-on.
     *
     * @return an {@link Optional} containing the key, or empty if unset
     */
    public Optional<MessageKey> messageAddOnKey() {
        return Optional.ofNullable(messageAddOnKey);
    }

    /**
     * Returns the legacy payload preserved for compatibility with older clients.
     *
     * @return an {@link Optional} containing the legacy container, or empty if none was set
     */
    public Optional<LegacyMessageContainer> legacyMessage() {
        return Optional.ofNullable(legacyMessage);
    }

    /**
     * Sets the kind of add-on this entry represents.
     *
     * @param messageAddOnType the new add-on type, or {@code null} to clear
     */
    public void setMessageAddOnType(MessageAddOnType messageAddOnType) {
        this.messageAddOnType = messageAddOnType;
    }

    /**
     * Sets the payload carried by this add-on.
     *
     * @param messageContainerAddOn the new payload container, or {@code null} to clear
     */
    public void setMessageAddOn(LinkedMessageContainer messageContainerAddOn) {
        this.messageContainerAddOn = messageContainerAddOn;
    }

    /**
     * Sets the instant at which the sender produced this add-on.
     *
     * @param senderTimestampMs the new sender timestamp, or {@code null} to clear
     */
    public void setSenderTimestampMs(Instant senderTimestampMs) {
        this.senderTimestampMs = senderTimestampMs;
    }

    /**
     * Sets the instant at which the server acknowledged this add-on.
     *
     * @param serverTimestampMs the new server timestamp, or {@code null} to clear
     */
    public void setServerTimestampMs(Instant serverTimestampMs) {
        this.serverTimestampMs = serverTimestampMs;
    }

    /**
     * Sets the current delivery status of this add-on.
     *
     * @param status the new status, or {@code null} to clear
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Sets the optional expiry context for this add-on.
     *
     * @param addOnContextInfo the new context info, or {@code null} to clear
     */
    public void setAddOnContextInfo(MessageAddOnContextInfo addOnContextInfo) {
        this.addOnContextInfo = addOnContextInfo;
    }

    /**
     * Sets the message key that uniquely identifies this add-on.
     *
     * @param messageAddOnKey the new message key, or {@code null} to clear
     */
    public void setMessageAddOnKey(MessageKey messageAddOnKey) {
        this.messageAddOnKey = messageAddOnKey;
    }

    /**
     * Sets the legacy payload carried by this add-on.
     *
     * @param legacyMessage the new legacy container, or {@code null} to clear
     */
    public void setLegacyMessage(LegacyMessageContainer legacyMessage) {
        this.legacyMessage = legacyMessage;
    }

    /**
     * Enumerates the kinds of add-ons that can be attached to a message.
     *
     * <p>Each constant describes the semantic role of an add-on so that
     * recipients know how to interpret the embedded payload:
     * <ul>
     *   <li>{@link #REACTION}: an emoji or text reaction to the parent message</li>
     *   <li>{@link #EVENT_RESPONSE}: a response to a scheduled event such as going or declining</li>
     *   <li>{@link #POLL_UPDATE}: a vote cast or updated on an existing poll</li>
     *   <li>{@link #PIN_IN_CHAT}: an in-chat pin marking a message as highlighted</li>
     *   <li>{@link #UNDEFINED}: fallback used when the type is not set or not recognised</li>
     * </ul>
     */
    @ProtobufEnum(name = "MessageAddOn.MessageAddOnType")
    public static enum MessageAddOnType {
        /**
         * Fallback value used when the add-on type is not set or not recognised.
         */
        UNDEFINED(0),
        /**
         * An emoji or textual reaction attached to an existing message.
         */
        REACTION(1),
        /**
         * A response to an event invitation, such as accepting or declining.
         */
        EVENT_RESPONSE(2),
        /**
         * A vote cast or updated on an existing poll message.
         */
        POLL_UPDATE(3),
        /**
         * An in-chat pin that highlights the parent message at the top of the chat.
         */
        PIN_IN_CHAT(4);

        /**
         * Constructs a new add-on type with the given protobuf wire index.
         *
         * @param index the protobuf wire index backing this constant
         */
        MessageAddOnType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
