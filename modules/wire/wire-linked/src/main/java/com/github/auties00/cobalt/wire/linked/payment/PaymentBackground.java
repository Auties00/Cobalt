package com.github.auties00.cobalt.wire.linked.payment;

import java.time.Instant;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A background image displayed behind a payment message in the chat view.
 *
 * <p>The background carries an image resource identified by {@link #id() id},
 * together with its dimensions, MIME type, and ARGB colour values used for
 * placeholder rendering before the image has loaded. The actual encrypted image
 * bytes are referenced indirectly through the {@link #mediaData() mediaData} field,
 * which provides the media key, SHA-256 hashes, and the CDN direct path needed to
 * download and decrypt the image.
 *
 * <p>The {@link #type() type} field classifies the background variant. The current
 * set of variants defines {@link Type#DEFAULT DEFAULT} for the standard payment
 * background and {@link Type#UNKNOWN UNKNOWN} as a fallback.
 *
 * <p>Payment backgrounds are attached to
 * {@link com.github.auties00.cobalt.wire.linked.message.payment.RequestPaymentMessage
 * RequestPaymentMessage} and
 * {@link com.github.auties00.cobalt.wire.linked.message.payment.SendPaymentMessage
 * SendPaymentMessage} to give the payment message a distinctive visual appearance.
 */
@ProtobufMessage(name = "PaymentBackground")
public final class PaymentBackground {
    /**
     * The unique identifier of the background image resource.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The size of the background image file in bytes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long fileLength;

    /**
     * The width of the background image in pixels.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    Integer width;

    /**
     * The height of the background image in pixels.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    Integer height;

    /**
     * The MIME type of the background image, such as {@code "image/jpeg"} or
     * {@code "image/png"}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String mimetype;

    /**
     * The ARGB colour value displayed as a placeholder while the background image is
     * loading. The value is encoded as a 32-bit fixed-width integer where each byte
     * represents, from most significant to least significant, the alpha, red, green,
     * and blue channels.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.FIXED32)
    Integer placeholderArgb;

    /**
     * The ARGB colour value used for primary text rendered over the payment background.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.FIXED32)
    Integer textArgb;

    /**
     * The ARGB colour value used for secondary or subtext rendered over the payment
     * background.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.FIXED32)
    Integer subtextArgb;

    /**
     * The encrypted media data for the background image, containing the media key,
     * SHA-256 hashes, and the CDN direct path.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    MediaData mediaData;

    /**
     * The variant of this payment background.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    Type type;

    /**
     * Constructs a new {@code PaymentBackground} with the given properties.
     *
     * @param id              the image resource identifier
     * @param fileLength      the file size in bytes
     * @param width           the image width in pixels
     * @param height          the image height in pixels
     * @param mimetype        the MIME type of the image
     * @param placeholderArgb the placeholder ARGB colour
     * @param textArgb        the primary text ARGB colour
     * @param subtextArgb     the secondary text ARGB colour
     * @param mediaData       the encrypted media data
     * @param type            the background variant
     */
    PaymentBackground(String id, Long fileLength, Integer width, Integer height, String mimetype, Integer placeholderArgb, Integer textArgb, Integer subtextArgb, MediaData mediaData, Type type) {
        this.id = id;
        this.fileLength = fileLength;
        this.width = width;
        this.height = height;
        this.mimetype = mimetype;
        this.placeholderArgb = placeholderArgb;
        this.textArgb = textArgb;
        this.subtextArgb = subtextArgb;
        this.mediaData = mediaData;
        this.type = type;
    }

    /**
     * Returns the unique identifier of the background image resource.
     *
     * @return an {@code Optional} containing the image identifier, or empty if not set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the size of the background image file in bytes.
     *
     * @return an {@code OptionalLong} containing the file length, or empty if not set
     */
    public OptionalLong fileLength() {
        return fileLength == null ? OptionalLong.empty() : OptionalLong.of(fileLength);
    }

    /**
     * Returns the width of the background image in pixels.
     *
     * @return an {@code OptionalInt} containing the width, or empty if not set
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the height of the background image in pixels.
     *
     * @return an {@code OptionalInt} containing the height, or empty if not set
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the MIME type of the background image.
     *
     * @return an {@code Optional} containing the MIME type, or empty if not set
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the ARGB placeholder colour value.
     *
     * @return an {@code OptionalInt} containing the ARGB value, or empty if not set
     */
    public OptionalInt placeholderArgb() {
        return placeholderArgb == null ? OptionalInt.empty() : OptionalInt.of(placeholderArgb);
    }

    /**
     * Returns the ARGB primary text colour value.
     *
     * @return an {@code OptionalInt} containing the ARGB value, or empty if not set
     */
    public OptionalInt textArgb() {
        return textArgb == null ? OptionalInt.empty() : OptionalInt.of(textArgb);
    }

    /**
     * Returns the ARGB secondary text colour value.
     *
     * @return an {@code OptionalInt} containing the ARGB value, or empty if not set
     */
    public OptionalInt subtextArgb() {
        return subtextArgb == null ? OptionalInt.empty() : OptionalInt.of(subtextArgb);
    }

    /**
     * Returns the encrypted media data for the background image.
     *
     * @return an {@code Optional} containing the media data, or empty if not set
     */
    public Optional<MediaData> mediaData() {
        return Optional.ofNullable(mediaData);
    }

    /**
     * Returns the variant of this payment background.
     *
     * @return an {@code Optional} containing the type, or empty if not set
     */
    public Optional<Type> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Sets the unique identifier of the background image resource.
     *
     * @param id the image identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the size of the background image file in bytes.
     *
     * @param fileLength the file length
     */
    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }

    /**
     * Sets the width of the background image in pixels.
     *
     * @param width the width
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Sets the height of the background image in pixels.
     *
     * @param height the height
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Sets the MIME type of the background image.
     *
     * @param mimetype the MIME type
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Sets the ARGB placeholder colour value.
     *
     * @param placeholderArgb the ARGB colour
     */
    public void setPlaceholderArgb(Integer placeholderArgb) {
        this.placeholderArgb = placeholderArgb;
    }

    /**
     * Sets the ARGB primary text colour value.
     *
     * @param textArgb the ARGB colour
     */
    public void setTextArgb(Integer textArgb) {
        this.textArgb = textArgb;
    }

    /**
     * Sets the ARGB secondary text colour value.
     *
     * @param subtextArgb the ARGB colour
     */
    public void setSubtextArgb(Integer subtextArgb) {
        this.subtextArgb = subtextArgb;
    }

    /**
     * Sets the encrypted media data for the background image.
     *
     * @param mediaData the media data
     */
    public void setMediaData(MediaData mediaData) {
        this.mediaData = mediaData;
    }

    /**
     * Sets the variant of this payment background.
     *
     * @param type the background variant
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * The variant of a payment background image.
     *
     * <p>Two variants are currently defined: {@link #DEFAULT} for the standard
     * payment background and {@link #UNKNOWN} as a fallback when the type is not
     * recognized or not set.
     */
    @ProtobufEnum(name = "PaymentBackground.Type")
    public enum Type {
        /**
         * The background type is not recognized or was not specified.
         */
        UNKNOWN(0),

        /**
         * The standard default payment background.
         */
        DEFAULT(1);

        /**
         * Constructs a new {@code Type} with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index of this background type.
         */
        final int index;

        /**
         * Returns the protobuf enum index of this background type.
         *
         * @return the numeric index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Encrypted media data associated with a {@link PaymentBackground}, providing the
     * cryptographic keys and CDN path needed to download and decrypt the background
     * image.
     *
     * <p>The {@link #mediaKey() mediaKey} is the AES-256 key used to decrypt the
     * downloaded ciphertext. The {@link #fileSha256() fileSha256} is the SHA-256 hash
     * of the plaintext file, while {@link #fileEncSha256() fileEncSha256} is the
     * SHA-256 hash of the encrypted file as stored on the CDN. The
     * {@link #directPath() directPath} is the CDN path from which the encrypted file
     * can be downloaded.
     */
    @ProtobufMessage(name = "PaymentBackground.MediaData")
    public static final class MediaData {
        /**
         * The AES-256 encryption key used to decrypt the background image after
         * downloading it from the CDN.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] mediaKey;

        /**
         * The epoch-second timestamp indicating when the media key was generated.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.INT64)
        Long mediaKeyTimestamp;

        /**
         * The SHA-256 hash of the plaintext (decrypted) background image file.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] fileSha256;

        /**
         * The SHA-256 hash of the encrypted background image file as stored on the CDN.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
        byte[] fileEncSha256;

        /**
         * The CDN direct path from which the encrypted background image can be
         * downloaded.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        String directPath;

        /**
         * Constructs a new {@code MediaData} with the given properties.
         *
         * @param mediaKey          the AES-256 media encryption key
         * @param mediaKeyTimestamp  the epoch-second timestamp of key generation
         * @param fileSha256        the SHA-256 hash of the plaintext file
         * @param fileEncSha256     the SHA-256 hash of the encrypted file
         * @param directPath        the CDN direct path
         */
        MediaData(byte[] mediaKey, Long mediaKeyTimestamp, byte[] fileSha256, byte[] fileEncSha256, String directPath) {
            this.mediaKey = mediaKey;
            this.mediaKeyTimestamp = mediaKeyTimestamp;
            this.fileSha256 = fileSha256;
            this.fileEncSha256 = fileEncSha256;
            this.directPath = directPath;
        }

        /**
         * Returns the AES-256 encryption key for the background image.
         *
         * @return an {@code Optional} containing the media key bytes, or empty if
         *         not set
         */
        public Optional<byte[]> mediaKey() {
            return Optional.ofNullable(mediaKey);
        }

        /**
         * Returns the timestamp at which the media key was generated, as an
         * {@link Instant}.
         *
         * @return an {@code Optional} containing the key generation timestamp, or
         *         empty if not set
         */
        public Optional<Instant> mediaKeyTimestamp() {
            return Optional.ofNullable(mediaKeyTimestamp)
                    .map(Instant::ofEpochSecond);
        }

        /**
         * Returns the SHA-256 hash of the plaintext background image file.
         *
         * @return an {@code Optional} containing the hash bytes, or empty if not set
         */
        public Optional<byte[]> fileSha256() {
            return Optional.ofNullable(fileSha256);
        }

        /**
         * Returns the SHA-256 hash of the encrypted background image file.
         *
         * @return an {@code Optional} containing the hash bytes, or empty if not set
         */
        public Optional<byte[]> fileEncSha256() {
            return Optional.ofNullable(fileEncSha256);
        }

        /**
         * Returns the CDN direct path for the encrypted background image.
         *
         * @return an {@code Optional} containing the path, or empty if not set
         */
        public Optional<String> directPath() {
            return Optional.ofNullable(directPath);
        }

        /**
         * Sets the AES-256 encryption key for the background image.
         *
         * @param mediaKey the media key bytes
         */
        public void setMediaKey(byte[] mediaKey) {
            this.mediaKey = mediaKey;
        }

        /**
         * Sets the epoch-second timestamp of when the media key was generated.
         *
         * @param mediaKeyTimestamp the timestamp as epoch seconds
         */
        public void setMediaKeyTimestamp(Long mediaKeyTimestamp) {
            this.mediaKeyTimestamp = mediaKeyTimestamp;
        }

        /**
         * Sets the SHA-256 hash of the plaintext background image file.
         *
         * @param fileSha256 the hash bytes
         */
        public void setFileSha256(byte[] fileSha256) {
            this.fileSha256 = fileSha256;
        }

        /**
         * Sets the SHA-256 hash of the encrypted background image file.
         *
         * @param fileEncSha256 the hash bytes
         */
        public void setFileEncSha256(byte[] fileEncSha256) {
            this.fileEncSha256 = fileEncSha256;
        }

        /**
         * Sets the CDN direct path for the encrypted background image.
         *
         * @param directPath the CDN path
         */
        public void setDirectPath(String directPath) {
            this.directPath = directPath;
        }
    }
}
