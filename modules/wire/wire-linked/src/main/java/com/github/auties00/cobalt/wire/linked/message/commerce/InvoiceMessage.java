package com.github.auties00.cobalt.wire.linked.message.commerce;

import com.github.auties00.cobalt.wire.linked.message.Message;

import java.time.Instant;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A payment invoice sent by a WhatsApp Business account to request payment
 * from a customer for goods or services.
 *
 * <p>The invoice carries a short descriptive note, a server-issued payment
 * token used to correlate the invoice with the downstream payment flow,
 * and a visual attachment (either an image or a PDF) that renders the
 * invoice details. The attachment is encrypted end-to-end using the same
 * scheme as other WhatsApp media; the fields on this message therefore
 * include the media key, plaintext and ciphertext SHA-256 hashes, the
 * media key issuance timestamp, the CDN direct path, and a JPEG thumbnail
 * used by clients to preview the invoice before download.
 */
@ProtobufMessage(name = "Message.InvoiceMessage")
public final class InvoiceMessage implements Message {
    /**
     * A short human-readable note shown next to the invoice.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String note;

    /**
     * The server-issued payment token that links the invoice to the
     * downstream payment flow.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String token;

    /**
     * Declares whether the attachment is an image or a PDF.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    AttachmentType attachmentType;

    /**
     * The MIME type of the attachment payload.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String attachmentMimetype;

    /**
     * The symmetric media key used to encrypt and decrypt the attachment.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    byte[] attachmentMediaKey;

    /**
     * The timestamp at which the media key was issued, used for media-key
     * rotation checks.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant attachmentMediaKeyTimestamp;

    /**
     * The SHA-256 hash of the plaintext attachment, used by the recipient
     * to verify integrity after decryption.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] attachmentFileSha256;

    /**
     * The SHA-256 hash of the ciphertext attachment as stored on the CDN,
     * used to verify the download before decryption.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] attachmentFileEncSha256;

    /**
     * The CDN direct path where the encrypted attachment can be
     * downloaded.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String attachmentDirectPath;

    /**
     * A JPEG thumbnail shown as a preview before the attachment is
     * downloaded.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BYTES)
    byte[] attachmentJpegThumbnail;


    /**
     * Constructs a new invoice message with every field set explicitly.
     *
     * <p>This constructor is package-private; callers should use the
     * generated {@code InvoiceMessageBuilder} to create instances.
     *
     * @param note the descriptive note shown next to the invoice
     * @param token the server-issued payment token
     * @param attachmentType the declared attachment type
     * @param attachmentMimetype the MIME type of the attachment
     * @param attachmentMediaKey the symmetric media key
     * @param attachmentMediaKeyTimestamp the media key issuance timestamp
     * @param attachmentFileSha256 the plaintext SHA-256 hash of the attachment
     * @param attachmentFileEncSha256 the ciphertext SHA-256 hash of the attachment
     * @param attachmentDirectPath the CDN direct path
     * @param attachmentJpegThumbnail the JPEG preview thumbnail
     */
    InvoiceMessage(String note, String token, AttachmentType attachmentType, String attachmentMimetype, byte[] attachmentMediaKey, Instant attachmentMediaKeyTimestamp, byte[] attachmentFileSha256, byte[] attachmentFileEncSha256, String attachmentDirectPath, byte[] attachmentJpegThumbnail) {
        this.note = note;
        this.token = token;
        this.attachmentType = attachmentType;
        this.attachmentMimetype = attachmentMimetype;
        this.attachmentMediaKey = attachmentMediaKey;
        this.attachmentMediaKeyTimestamp = attachmentMediaKeyTimestamp;
        this.attachmentFileSha256 = attachmentFileSha256;
        this.attachmentFileEncSha256 = attachmentFileEncSha256;
        this.attachmentDirectPath = attachmentDirectPath;
        this.attachmentJpegThumbnail = attachmentJpegThumbnail;
    }

    /**
     * Returns the short human-readable note shown next to the invoice.
     *
     * @return an {@link Optional} containing the note, or empty if not set
     */
    public Optional<String> note() {
        return Optional.ofNullable(note);
    }

    /**
     * Returns the server-issued payment token that links the invoice to
     * the downstream payment flow.
     *
     * @return an {@link Optional} containing the token, or empty if not set
     */
    public Optional<String> token() {
        return Optional.ofNullable(token);
    }

    /**
     * Returns the declared type of the invoice attachment.
     *
     * @return an {@link Optional} containing the attachment type, or empty if not set
     */
    public Optional<AttachmentType> attachmentType() {
        return Optional.ofNullable(attachmentType);
    }

    /**
     * Returns the MIME type of the attachment payload.
     *
     * @return an {@link Optional} containing the MIME type, or empty if not set
     */
    public Optional<String> attachmentMimetype() {
        return Optional.ofNullable(attachmentMimetype);
    }

    /**
     * Returns the symmetric media key used to encrypt and decrypt the
     * attachment.
     *
     * @return an {@link Optional} containing the media key bytes, or empty if not set
     */
    public Optional<byte[]> attachmentMediaKey() {
        return Optional.ofNullable(attachmentMediaKey);
    }

    /**
     * Returns the issuance timestamp of the media key, used for media-key
     * rotation checks.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not set
     */
    public Optional<Instant> attachmentMediaKeyTimestamp() {
        return Optional.ofNullable(attachmentMediaKeyTimestamp);
    }

    /**
     * Returns the SHA-256 hash of the plaintext attachment.
     *
     * @return an {@link Optional} containing the plaintext hash, or empty if not set
     */
    public Optional<byte[]> attachmentFileSha256() {
        return Optional.ofNullable(attachmentFileSha256);
    }

    /**
     * Returns the SHA-256 hash of the encrypted attachment as stored on
     * the CDN.
     *
     * @return an {@link Optional} containing the ciphertext hash, or empty if not set
     */
    public Optional<byte[]> attachmentFileEncSha256() {
        return Optional.ofNullable(attachmentFileEncSha256);
    }

    /**
     * Returns the CDN direct path where the encrypted attachment can be
     * downloaded.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    public Optional<String> attachmentDirectPath() {
        return Optional.ofNullable(attachmentDirectPath);
    }

    /**
     * Returns the JPEG thumbnail shown as a preview before the attachment
     * is downloaded.
     *
     * @return an {@link Optional} containing the thumbnail bytes, or empty if not set
     */
    public Optional<byte[]> attachmentJpegThumbnail() {
        return Optional.ofNullable(attachmentJpegThumbnail);
    }

    /**
     * Sets the short human-readable note shown next to the invoice.
     *
     * @param note the note, or {@code null} to clear it
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Sets the server-issued payment token.
     *
     * @param token the token, or {@code null} to clear it
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Sets the declared type of the invoice attachment.
     *
     * @param attachmentType the attachment type, or {@code null} to clear it
     */
    public void setAttachmentType(AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
    }

    /**
     * Sets the MIME type of the attachment payload.
     *
     * @param attachmentMimetype the MIME type, or {@code null} to clear it
     */
    public void setAttachmentMimetype(String attachmentMimetype) {
        this.attachmentMimetype = attachmentMimetype;
    }

    /**
     * Sets the symmetric media key used to encrypt and decrypt the
     * attachment.
     *
     * @param attachmentMediaKey the media key bytes, or {@code null} to clear them
     */
    public void setAttachmentMediaKey(byte[] attachmentMediaKey) {
        this.attachmentMediaKey = attachmentMediaKey;
    }

    /**
     * Sets the issuance timestamp of the media key.
     *
     * @param attachmentMediaKeyTimestamp the timestamp, or {@code null} to clear it
     */
    public void setAttachmentMediaKeyTimestamp(Instant attachmentMediaKeyTimestamp) {
        this.attachmentMediaKeyTimestamp = attachmentMediaKeyTimestamp;
    }

    /**
     * Sets the SHA-256 hash of the plaintext attachment.
     *
     * @param attachmentFileSha256 the plaintext hash, or {@code null} to clear it
     */
    public void setAttachmentFileSha256(byte[] attachmentFileSha256) {
        this.attachmentFileSha256 = attachmentFileSha256;
    }

    /**
     * Sets the SHA-256 hash of the encrypted attachment.
     *
     * @param attachmentFileEncSha256 the ciphertext hash, or {@code null} to clear it
     */
    public void setAttachmentFileEncSha256(byte[] attachmentFileEncSha256) {
        this.attachmentFileEncSha256 = attachmentFileEncSha256;
    }

    /**
     * Sets the CDN direct path where the encrypted attachment can be
     * downloaded.
     *
     * @param attachmentDirectPath the direct path, or {@code null} to clear it
     */
    public void setAttachmentDirectPath(String attachmentDirectPath) {
        this.attachmentDirectPath = attachmentDirectPath;
    }

    /**
     * Sets the JPEG thumbnail shown as a preview.
     *
     * @param attachmentJpegThumbnail the thumbnail bytes, or {@code null} to clear them
     */
    public void setAttachmentJpegThumbnail(byte[] attachmentJpegThumbnail) {
        this.attachmentJpegThumbnail = attachmentJpegThumbnail;
    }

    /**
     * Declares the media type of an {@link InvoiceMessage} attachment.
     */
    @ProtobufEnum(name = "Message.InvoiceMessage.AttachmentType")
    public static enum AttachmentType {
        /**
         * The attachment is an image rendering of the invoice.
         */
        IMAGE(0),
        /**
         * The attachment is a PDF rendering of the invoice.
         */
        PDF(1);

        /**
         * Constructs an attachment type with the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        AttachmentType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this enum constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
