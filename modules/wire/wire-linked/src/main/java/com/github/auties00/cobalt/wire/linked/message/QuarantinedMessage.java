package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Snapshot of a message that has been withheld from the chat because
 * WhatsApp's Defense Mode classified it as potentially abusive.
 *
 * <p>When Defense Mode blocks a suspicious message, the client keeps
 * a copy of the original payload locally so that the user can review
 * it, restore it, or discard it without any data being lost. This
 * class is the container used for that local copy: it preserves the
 * raw protobuf bytes of the original message and, when possible, a
 * plain text rendering extracted for display inside the quarantine UI.
 */
@ProtobufMessage(name = "QuarantinedMessage")
public final class QuarantinedMessage {
    /**
     * The raw, encoded protobuf bytes of the message that was
     * quarantined.
     *
     * <p>These bytes can be re-injected into the normal processing
     * pipeline if the user chooses to restore the message.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] originalData;

    /**
     * A human-readable rendering of the quarantined message suitable
     * for display inside the quarantine review UI, if one could be
     * produced.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String extractedText;


    /**
     * Constructs a new {@code QuarantinedMessage}.
     *
     * <p>The constructor is package-private; use
     * {@code QuarantinedMessageBuilder} to instantiate new values.
     *
     * @param originalData  the raw protobuf bytes of the original
     *                      message, or {@code null}
     * @param extractedText the text extracted for display, or
     *                      {@code null}
     */
    QuarantinedMessage(byte[] originalData, String extractedText) {
        this.originalData = originalData;
        this.extractedText = extractedText;
    }

    /**
     * Returns the raw protobuf bytes of the message that was
     * quarantined, if preserved.
     *
     * @return an {@link Optional} holding the original encoded
     *         message, or empty if the bytes were not preserved
     */
    public Optional<byte[]> originalData() {
        return Optional.ofNullable(originalData);
    }

    /**
     * Returns a human-readable rendering of the quarantined message,
     * if one could be produced.
     *
     * @return an {@link Optional} holding the extracted text, or
     *         empty if no text rendering is available
     */
    public Optional<String> extractedText() {
        return Optional.ofNullable(extractedText);
    }

    /**
     * Updates the raw protobuf bytes of the quarantined message.
     *
     * @param originalData the new encoded bytes, or {@code null}
     *                     to clear
     */
    public void setOriginalData(byte[] originalData) {
        this.originalData = originalData;
    }

    /**
     * Updates the extracted text rendering.
     *
     * @param extractedText the new extracted text, or {@code null}
     *                      to clear
     */
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
}
