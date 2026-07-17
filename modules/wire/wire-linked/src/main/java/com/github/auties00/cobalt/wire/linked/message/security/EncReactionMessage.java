package com.github.auties00.cobalt.wire.linked.message.security;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents an encrypted reaction (for example an emoji) applied to a
 * previously sent message.
 *
 * <p>Reactions are emitted as lightweight end-to-end encrypted messages
 * that reference the target message by its {@link MessageKey}. The
 * reaction text itself is encrypted using a symmetric key derived from
 * the target message's secret, so only participants who can decrypt the
 * original message can read the reaction. The ciphertext and its
 * initialization vector are both carried here.
 */
@ProtobufMessage(name = "Message.EncReactionMessage")
public final class EncReactionMessage implements Message {
    /**
     * Identifies the message this reaction applies to.
     *
     * <p>The key uniquely locates the parent message so that the client
     * can display the reaction under the correct conversation item.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey targetMessageKey;

    /**
     * Encrypted payload containing the serialized reaction content.
     *
     * <p>The plaintext is the protobuf-encoded reaction, encrypted with
     * a symmetric key derived from the target message's secret.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] encPayload;

    /**
     * Initialization vector used to encrypt {@link #encPayload}.
     *
     * <p>The IV is required to decrypt the payload and is transmitted in
     * the clear alongside the ciphertext.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] encIv;

    /**
     * Constructs a new encrypted reaction message.
     *
     * @param targetMessageKey the key of the message being reacted to,
     *                         or {@code null} if unset
     * @param encPayload       the encrypted reaction payload, or
     *                         {@code null} if unset
     * @param encIv            the initialization vector used to produce
     *                         {@code encPayload}, or {@code null} if unset
     */
    EncReactionMessage(MessageKey targetMessageKey, byte[] encPayload, byte[] encIv) {
        this.targetMessageKey = targetMessageKey;
        this.encPayload = encPayload;
        this.encIv = encIv;
    }

    /**
     * Returns the key of the message this reaction applies to.
     *
     * @return an {@link Optional} holding the target {@link MessageKey},
     *         or {@link Optional#empty()} if the field is unset
     */
    public Optional<MessageKey> targetMessageKey() {
        return Optional.ofNullable(targetMessageKey);
    }

    /**
     * Returns the encrypted reaction payload.
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
     * Sets the key of the message this reaction applies to.
     *
     * @param targetMessageKey the new target {@link MessageKey}, or
     *                         {@code null} to clear the field
     */
    public void setTargetMessageKey(MessageKey targetMessageKey) {
        this.targetMessageKey = targetMessageKey;
    }

    /**
     * Sets the encrypted reaction payload.
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
}
