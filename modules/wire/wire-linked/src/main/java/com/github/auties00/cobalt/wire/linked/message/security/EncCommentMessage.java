package com.github.auties00.cobalt.wire.linked.message.security;

import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents an encrypted comment attached to a previously sent message.
 *
 * <p>Comments are short replies threaded to a parent message. The payload
 * of the comment is encrypted end-to-end using a secret derived from the
 * target message, so only parties that can decrypt the original message
 * can read the comment. The comment itself is carried inside a
 * {@link Message} envelope that references the target message through
 * its {@link MessageKey} and provides the ciphertext together with the
 * initialization vector used during encryption.
 */
@ProtobufMessage(name = "Message.EncCommentMessage")
public final class EncCommentMessage implements Message {
    /**
     * Identifies the message this comment is attached to.
     *
     * <p>The key uniquely locates the parent message in the chat so that
     * clients can associate the comment with the correct conversation
     * item when rendering the thread.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey targetMessageKey;

    /**
     * Encrypted payload containing the serialized comment message.
     *
     * <p>The plaintext is the protobuf-encoded comment content, encrypted
     * with a symmetric key derived from the target message's secret.
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
     * Constructs a new encrypted comment message.
     *
     * @param targetMessageKey the key of the message being commented on,
     *                         or {@code null} if unset
     * @param encPayload       the encrypted comment payload, or {@code null}
     *                         if unset
     * @param encIv            the initialization vector used to produce
     *                         {@code encPayload}, or {@code null} if unset
     */
    EncCommentMessage(MessageKey targetMessageKey, byte[] encPayload, byte[] encIv) {
        this.targetMessageKey = targetMessageKey;
        this.encPayload = encPayload;
        this.encIv = encIv;
    }

    /**
     * Returns the key of the message this comment is attached to.
     *
     * @return an {@link Optional} holding the target {@link MessageKey},
     *         or {@link Optional#empty()} if the field is unset
     */
    public Optional<MessageKey> targetMessageKey() {
        return Optional.ofNullable(targetMessageKey);
    }

    /**
     * Returns the encrypted comment payload.
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
     * Sets the key of the message this comment is attached to.
     *
     * @param targetMessageKey the new target {@link MessageKey}, or
     *                         {@code null} to clear the field
     */
    public void setTargetMessageKey(MessageKey targetMessageKey) {
        this.targetMessageKey = targetMessageKey;
    }

    /**
     * Sets the encrypted comment payload.
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
