package com.github.auties00.cobalt.wire.linked.message.event;

import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.message.Message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents an end-to-end encrypted reply to a WhatsApp event.
 *
 * <p>When a user RSVPs to an event, their {@link EventResponseMessage} is
 * first serialized and then encrypted with a per-event symmetric key so that
 * only participants of the chat (and in particular the event creator) can
 * read the payload. The ciphertext, together with the initialisation vector
 * used by the cipher and the {@link MessageKey} of the original event
 * creation message, is wrapped in an {@code EncEventResponseMessage} before
 * being transmitted.
 *
 * <p>This is the variant that actually travels over the wire when a user
 * taps one of the RSVP buttons attached to an {@link EventMessage}. The
 * recipient uses {@link #eventCreationMessageKey()} to locate the event
 * being replied to, derives the decryption key from the event payload, and
 * decrypts {@link #encPayload()} using the IV in {@link #encIv()} to recover
 * the underlying {@link EventResponseMessage}.
 */
@ProtobufMessage(name = "Message.EncEventResponseMessage")
public final class EncEventResponseMessage implements Message {
    /**
     * The {@link MessageKey} identifying the event that this response refers
     * to.
     *
     * <p>Recipients use this key to look up the original {@link EventMessage}
     * inside the conversation so that the corresponding encryption key can
     * be derived and the response payload can be decrypted.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    MessageKey eventCreationMessageKey;

    /**
     * The encrypted, serialized bytes of an {@link EventResponseMessage}.
     *
     * <p>Once decrypted using the key derived from the original event and
     * the initialisation vector stored in {@link #encIv}, this byte array
     * deserialises back into an {@link EventResponseMessage} that carries
     * the RSVP choice, timestamp, and optional extra guest count.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] encPayload;

    /**
     * The initialisation vector used by the symmetric cipher that produced
     * {@link #encPayload}.
     *
     * <p>The IV is randomly generated on the sender side for every response
     * and must be preserved unchanged for the ciphertext to be decryptable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] encIv;

    /**
     * Constructs a new {@code EncEventResponseMessage} that wraps an
     * encrypted RSVP.
     *
     * <p>The constructor is package-private. Application code should build
     * instances through the generated {@code EncEventResponseMessageBuilder}.
     *
     * @param eventCreationMessageKey the {@link MessageKey} of the event being
     *                                replied to, or {@code null} if not set
     * @param encPayload              the encrypted {@link EventResponseMessage}
     *                                bytes, or {@code null} if not set
     * @param encIv                   the initialisation vector used by the
     *                                cipher, or {@code null} if not set
     */
    EncEventResponseMessage(MessageKey eventCreationMessageKey, byte[] encPayload, byte[] encIv) {
        this.eventCreationMessageKey = eventCreationMessageKey;
        this.encPayload = encPayload;
        this.encIv = encIv;
    }

    /**
     * Returns the {@link MessageKey} of the event that this response refers
     * to.
     *
     * @return an {@link Optional} containing the event creation message key,
     *         or {@code Optional.empty()} when no key was supplied
     */
    public Optional<MessageKey> eventCreationMessageKey() {
        return Optional.ofNullable(eventCreationMessageKey);
    }

    /**
     * Returns the encrypted, serialized {@link EventResponseMessage} payload.
     *
     * @return an {@link Optional} containing the ciphertext, or
     *         {@code Optional.empty()} when no payload is set
     */
    public Optional<byte[]> encPayload() {
        return Optional.ofNullable(encPayload);
    }

    /**
     * Returns the initialisation vector used to encrypt
     * {@link #encPayload()}.
     *
     * @return an {@link Optional} containing the IV bytes, or
     *         {@code Optional.empty()} when no IV is set
     */
    public Optional<byte[]> encIv() {
        return Optional.ofNullable(encIv);
    }

    /**
     * Sets the {@link MessageKey} of the event that this response refers to.
     *
     * @param eventCreationMessageKey the new event creation message key, or
     *                                {@code null} to clear the field
     */
    public void setEventCreationMessageKey(MessageKey eventCreationMessageKey) {
        this.eventCreationMessageKey = eventCreationMessageKey;
    }

    /**
     * Sets the encrypted serialized payload.
     *
     * @param encPayload the new ciphertext bytes, or {@code null} to clear
     *                   the field
     */
    public void setEncPayload(byte[] encPayload) {
        this.encPayload = encPayload;
    }

    /**
     * Sets the initialisation vector used by the cipher.
     *
     * @param encIv the new IV bytes, or {@code null} to clear the field
     */
    public void setEncIv(byte[] encIv) {
        this.encIv = encIv;
    }
}
