package com.github.auties00.cobalt.wire.linked.message.security;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a generic secret encrypted message that carries an updated
 * payload keyed to a previously sent message.
 *
 * <p>This envelope is the building block for features that need to
 * replace or amend an existing message in place, such as message edits
 * and event edits. Clients reference the original message by its
 * {@link MessageKey} and deliver the new content as ciphertext
 * encrypted with a symmetric key derived from the target message's
 * secret. The {@link SecretEncType} describes the operation the
 * payload performs so the receiver can apply it correctly.
 */
@ProtobufMessage(name = "Message.SecretEncryptedMessage")
public final class SecretEncMessage implements Message {
    /**
     * Identifies the message whose secret is used to encrypt the new
     * payload and to which the update applies.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey targetMessageKey;

    /**
     * Encrypted payload carrying the new or amended message content.
     *
     * <p>The plaintext is the protobuf-encoded content appropriate for
     * the declared {@link SecretEncType}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] encPayload;

    /**
     * Initialization vector used to encrypt {@link #encPayload}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] encIv;

    /**
     * Declares the semantics of the encrypted payload, so that the
     * receiver knows whether to treat it as a message edit, an event
     * edit, or another update.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    SecretEncType secretEncType;


    /**
     * Constructs a new secret encrypted message envelope.
     *
     * @param targetMessageKey the key of the message this envelope
     *                         updates, or {@code null} if unset
     * @param encPayload       the encrypted payload, or {@code null} if
     *                         unset
     * @param encIv            the initialization vector used to produce
     *                         {@code encPayload}, or {@code null} if unset
     * @param secretEncType    the semantics of the payload, or
     *                         {@code null} if unset
     */
    SecretEncMessage(MessageKey targetMessageKey, byte[] encPayload, byte[] encIv, SecretEncType secretEncType) {
        this.targetMessageKey = targetMessageKey;
        this.encPayload = encPayload;
        this.encIv = encIv;
        this.secretEncType = secretEncType;
    }

    /**
     * Returns the key of the message this envelope updates.
     *
     * @return an {@link Optional} holding the target {@link MessageKey},
     *         or {@link Optional#empty()} if the field is unset
     */
    public Optional<MessageKey> targetMessageKey() {
        return Optional.ofNullable(targetMessageKey);
    }

    /**
     * Returns the encrypted payload.
     *
     * @return an {@link Optional} holding the ciphertext bytes, or
     *         {@link Optional#empty()} if the field is unset
     */
    public Optional<byte[]> encPayload() {
        return Optional.ofNullable(encPayload);
    }

    /**
     * Returns the initialization vector used to encrypt the payload.
     *
     * @return an {@link Optional} holding the IV bytes, or
     *         {@link Optional#empty()} if the field is unset
     */
    public Optional<byte[]> encIv() {
        return Optional.ofNullable(encIv);
    }

    /**
     * Returns the semantics of the encrypted payload.
     *
     * @return an {@link Optional} holding the {@link SecretEncType},
     *         or {@link Optional#empty()} if the field is unset
     */
    public Optional<SecretEncType> secretEncType() {
        return Optional.ofNullable(secretEncType);
    }

    /**
     * Sets the key of the message this envelope updates.
     *
     * @param targetMessageKey the new target {@link MessageKey}, or
     *                         {@code null} to clear the field
     */
    public void setTargetMessageKey(MessageKey targetMessageKey) {
        this.targetMessageKey = targetMessageKey;
    }

    /**
     * Sets the encrypted payload.
     *
     * @param encPayload the new ciphertext bytes, or {@code null} to
     *                   clear the field
     */
    public void setEncPayload(byte[] encPayload) {
        this.encPayload = encPayload;
    }

    /**
     * Sets the initialization vector used to encrypt the payload.
     *
     * @param encIv the new IV bytes, or {@code null} to clear the field
     */
    public void setEncIv(byte[] encIv) {
        this.encIv = encIv;
    }

    /**
     * Sets the semantics of the encrypted payload.
     *
     * @param secretEncType the new {@link SecretEncType}, or
     *                      {@code null} to clear the field
     */
    public void setSecretEncType(SecretEncType secretEncType) {
        this.secretEncType = secretEncType;
    }

    /**
     * Enumerates the kinds of update that a {@link SecretEncMessage}
     * can carry.
     */
    @ProtobufEnum(name = "Message.SecretEncryptedMessage.SecretEncType")
    public static enum SecretEncType {
        /**
         * Used when the update type is unspecified or cannot be
         * recognised by the current client.
         */
        UNKNOWN(0),
        /**
         * Indicates that the payload carries an edit applied to an
         * existing event message.
         */
        EVENT_EDIT(1),
        /**
         * Indicates that the payload carries an edit that replaces the
         * body of the target message.
         */
        MESSAGE_EDIT(2);

        /**
         * Constructs a new secret encryption type constant.
         *
         * @param index the protobuf wire index associated with this
         *              enumeration value
         */
        SecretEncType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index associated with this enumeration value.
         */
        final int index;

        /**
         * Returns the protobuf wire index associated with this value.
         *
         * @return the integer index used to serialize this constant
         */
        public int index() {
            return this.index;
        }
    }
}
