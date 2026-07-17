package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * The server's reply to a media re-upload request, carrying the outcome of the
 * operation and the new CDN coordinates on success.
 *
 * <p>When the client attempts to download media that is no longer reachable on
 * the CDN (for example because the original upload expired or was evicted), it
 * asks the sender's device to re-upload the file. The device uploads the file
 * again and the server responds with this notification, which identifies the
 * original message by its stanza id, reports whether the re-upload succeeded
 * through {@link ResultType}, and, when successful, supplies an updated direct
 * path for the newly uploaded content.
 *
 * <p>The notification payload itself is encrypted; the {@code messageSecret}
 * field is the input to HKDF for deriving the AES-GCM decryption key used on
 * the notification body.
 */
@ProtobufMessage(name = "MediaRetryNotification")
public final class MediaRetryNotification {
    /**
     * The stanza id of the message whose media was re-requested.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * The updated CDN direct path to the re-uploaded media, populated only
     * when {@link #result} is {@link ResultType#SUCCESS}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String directPath;

    /**
     * The outcome of the re-upload request.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    ResultType result;

    /**
     * The HKDF input secret used to derive the AES-GCM key that decrypts the
     * notification's encrypted payload.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] messageSecret;

    /**
     * Constructs a new {@code MediaRetryNotification} with the given fields.
     *
     * @param stanzaId      the stanza id of the original message
     * @param directPath    the updated CDN direct path, or {@code null} on failure
     * @param result        the outcome of the re-upload request
     * @param messageSecret the HKDF input secret for payload decryption
     */
    MediaRetryNotification(String stanzaId, String directPath, ResultType result, byte[] messageSecret) {
        this.stanzaId = stanzaId;
        this.directPath = directPath;
        this.result = result;
        this.messageSecret = messageSecret;
    }

    /**
     * Returns the stanza id of the message whose media was re-requested.
     *
     * @return an {@link Optional} containing the stanza id, or empty if not set
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Returns the updated CDN direct path to the re-uploaded media.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the outcome of the re-upload request.
     *
     * @return an {@link Optional} containing the result type, or empty if not set
     */
    public Optional<ResultType> result() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns the HKDF input secret used to derive the AES-GCM key for
     * decrypting the notification payload.
     *
     * @return an {@link Optional} containing the secret, or empty if not set
     */
    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    /**
     * Sets the stanza id of the original message.
     *
     * @param stanzaId the stanza id
     */
    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    /**
     * Sets the updated CDN direct path.
     *
     * @param directPath the direct path
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Sets the outcome of the re-upload request.
     *
     * @param result the result type
     */
    public void setResult(ResultType result) {
        this.result = result;
    }

    /**
     * Sets the HKDF input secret.
     *
     * @param messageSecret the message secret
     */
    public void setMessageSecret(byte[] messageSecret) {
        this.messageSecret = messageSecret;
    }

    /**
     * The outcome of a media re-upload request.
     *
     * <p>On the client side these codes are frequently mapped to HTTP-like
     * status values for internal handling: {@link #SUCCESS} corresponds to
     * {@code 200}, {@link #NOT_FOUND} and {@link #DECRYPTION_ERROR}
     * correspond to {@code 404}, and {@link #GENERAL_ERROR} corresponds to
     * {@code 500}.
     */
    @ProtobufEnum(name = "MediaRetryNotification.ResultType")
    public enum ResultType {
        /**
         * The re-upload failed due to an unspecified server-side error.
         * Numeric value {@code 0}.
         */
        GENERAL_ERROR(0),

        /**
         * The re-upload succeeded and the new direct path is available in the
         * enclosing notification.
         * Numeric value {@code 1}.
         */
        SUCCESS(1),

        /**
         * The requested media could not be located on the server and could
         * not be re-uploaded.
         * Numeric value {@code 2}.
         */
        NOT_FOUND(2),

        /**
         * The server could not decrypt the retry request, typically because
         * the message secret was invalid.
         * Numeric value {@code 3}.
         */
        DECRYPTION_ERROR(3);

        /**
         * Constructs a new {@code ResultType} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        ResultType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index backing this result type.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this result type.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }
}
