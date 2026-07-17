package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Encrypted envelope for an addon payload that targets another message,
 * such as a reaction, a comment, or an event edit.
 *
 * <p>Some addons on WhatsApp (reactions, comments, poll votes, event
 * RSVPs) need to remain hidden from the server even though the server
 * must still be able to route them back to the parent message. To
 * achieve this WhatsApp derives a per-message secret shared between
 * the participants of the conversation and uses it to encrypt the
 * addon payload with AES-GCM. The resulting ciphertext and IV travel
 * inside a {@code SecretMessageContainer}; only clients that hold the
 * message secret can decrypt the contents.
 *
 * <p>The {@link #version()} field allows the encryption scheme to
 * evolve over time without breaking backwards compatibility with
 * older clients.
 */
@ProtobufMessage(name = "MessageSecretMessage")
public final class SecretMessageContainer {
    /**
     * The version of the secret-encryption scheme used to produce the
     * {@link #encPayload}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.SFIXED32)
    Integer version;

    /**
     * The initialisation vector used by the AES-GCM encryption.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] encIv;

    /**
     * The AES-GCM ciphertext produced from the addon payload.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] encPayload;


    /**
     * Constructs a new {@code SecretMessageContainer}.
     *
     * <p>The constructor is package-private; use
     * {@code SecretMessageContainerBuilder} to instantiate new values.
     *
     * @param version    the encryption scheme version, or {@code null}
     * @param encIv      the AES-GCM initialisation vector, or
     *                   {@code null}
     * @param encPayload the AES-GCM ciphertext, or {@code null}
     */
    SecretMessageContainer(Integer version, byte[] encIv, byte[] encPayload) {
        this.version = version;
        this.encIv = encIv;
        this.encPayload = encPayload;
    }

    /**
     * Returns the version of the secret-encryption scheme.
     *
     * @return an {@link OptionalInt} holding the version, or empty if
     *         none was set
     */
    public OptionalInt version() {
        return version == null ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * Returns the AES-GCM initialisation vector, if present.
     *
     * @return an {@link Optional} holding the IV bytes, or empty if
     *         none was set
     */
    public Optional<byte[]> encIv() {
        return Optional.ofNullable(encIv);
    }

    /**
     * Returns the AES-GCM ciphertext produced from the addon payload,
     * if present.
     *
     * @return an {@link Optional} holding the ciphertext bytes, or
     *         empty if none was set
     */
    public Optional<byte[]> encPayload() {
        return Optional.ofNullable(encPayload);
    }

    /**
     * Updates the encryption scheme version.
     *
     * @param version the new version, or {@code null} to clear
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Updates the AES-GCM initialisation vector.
     *
     * @param encIv the new IV bytes, or {@code null} to clear
     */
    public void setEncIv(byte[] encIv) {
        this.encIv = encIv;
    }

    /**
     * Updates the AES-GCM ciphertext.
     *
     * @param encPayload the new ciphertext bytes, or {@code null}
     *                   to clear
     */
    public void setEncPayload(byte[] encPayload) {
        this.encPayload = encPayload;
    }
}
