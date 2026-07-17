package com.github.auties00.cobalt.wire.linked.business.support;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One media attachment uploaded with a WhatsApp support bug report.
 *
 * <p>When a user attaches images or videos to a "report a problem" submission, each attachment is
 * uploaded encrypted and referenced by this descriptor: the {@link #cipherKey() cipher key} and
 * {@link #iv() initialisation vector} that decrypt it, the {@link #elementValue() element value}
 * locating the uploaded blob, the {@link #type() media kind} ({@code "IMAGE"} or {@code "VIDEO"}), and
 * the original {@link #fileName() file name}.
 */
@ProtobufMessage(name = "BusinessSupportBugReportMedia")
public final class BusinessSupportBugReportMedia {
    /**
     * Base64 cipher key that decrypts the uploaded attachment. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String cipherKey;

    /**
     * Element value locating the uploaded encrypted blob. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String elementValue;

    /**
     * Base64 initialisation vector paired with the cipher key. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String iv;

    /**
     * Media kind of the attachment ({@code "IMAGE"} or {@code "VIDEO"}). Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String type;

    /**
     * Original file name of the attachment. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String fileName;

    /**
     * Constructs a new {@code BusinessSupportBugReportMedia}. Every argument may be {@code null} to
     * leave the corresponding field unset.
     *
     * @param cipherKey    the cipher key, or {@code null}
     * @param elementValue the element value, or {@code null}
     * @param iv           the initialisation vector, or {@code null}
     * @param type         the media kind, or {@code null}
     * @param fileName     the original file name, or {@code null}
     */
    BusinessSupportBugReportMedia(String cipherKey, String elementValue, String iv, String type, String fileName) {
        this.cipherKey = cipherKey;
        this.elementValue = elementValue;
        this.iv = iv;
        this.type = type;
        this.fileName = fileName;
    }

    /**
     * Returns the cipher key that decrypts the uploaded attachment.
     *
     * @return an {@link Optional} carrying the cipher key, or empty when unset
     */
    public Optional<String> cipherKey() {
        return Optional.ofNullable(cipherKey);
    }

    /**
     * Returns the element value locating the uploaded encrypted blob.
     *
     * @return an {@link Optional} carrying the element value, or empty when unset
     */
    public Optional<String> elementValue() {
        return Optional.ofNullable(elementValue);
    }

    /**
     * Returns the initialisation vector paired with the cipher key.
     *
     * @return an {@link Optional} carrying the initialisation vector, or empty when unset
     */
    public Optional<String> iv() {
        return Optional.ofNullable(iv);
    }

    /**
     * Returns the media kind of the attachment.
     *
     * @return an {@link Optional} carrying the media kind, or empty when unset
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the original file name of the attachment.
     *
     * @return an {@link Optional} carrying the file name, or empty when unset
     */
    public Optional<String> fileName() {
        return Optional.ofNullable(fileName);
    }
}
