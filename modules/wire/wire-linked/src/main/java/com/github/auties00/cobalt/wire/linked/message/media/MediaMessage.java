package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A common contract for messages whose content is a piece of encrypted media.
 *
 * <p>WhatsApp stores every media attachment (audio, image, video, document,
 * sticker) as an encrypted blob on its CDN. A media message only travels with the
 * metadata required to locate the blob, decrypt it and verify its integrity. This
 * sealed interface surfaces that metadata in a uniform way and is implemented by
 * {@link AudioMessage}, {@link DocumentMessage}, {@link ImageMessage},
 * {@link StickerMessage} and {@link VideoMessage}.
 *
 * <p>Media messages are also contextual, meaning they can carry quoting or
 * forwarding metadata, and act as {@link MediaProvider} so that media upload and
 * download pipelines can consume them generically.
 */
public sealed interface MediaMessage
        extends ContextualMessage, MediaProvider
        permits AudioMessage, DocumentMessage, ImageMessage, StickerMessage, VideoMessage {

    /**
     * Returns the CDN URL of the encrypted media payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    Optional<String> url();

    /**
     * Returns the SHA-256 digest of the decrypted media payload, used to verify
     * integrity after download.
     *
     * @return the hash, or empty if unknown
     */
    Optional<byte[]> fileSha256();

    /**
     * Returns the size in bytes of the decrypted media payload.
     *
     * @return the size, or empty if unknown
     */
    OptionalLong fileLength();

    /**
     * Returns the symmetric key used to decrypt the media payload.
     *
     * @return the key, or empty if the payload has not been prepared
     */
    Optional<byte[]> mediaKey();

    /**
     * Returns the SHA-256 digest of the encrypted media bytes as stored on the
     * server.
     *
     * @return the hash, or empty if unknown
     */
    Optional<byte[]> fileEncSha256();

    /**
     * Returns the CDN direct path used to locate the encrypted payload.
     *
     * @return the path, or empty if not yet uploaded
     */
    Optional<String> directPath();

    /**
     * Returns the moment at which the {@link #mediaKey()} was generated.
     *
     * @return the timestamp, or empty if unknown
     */
    Optional<Instant> mediaKeyTimestamp();

    /**
     * Returns the MIME type of the media payload.
     *
     * @return the MIME type, or empty if unknown
     */
    Optional<String> mimetype();
}
