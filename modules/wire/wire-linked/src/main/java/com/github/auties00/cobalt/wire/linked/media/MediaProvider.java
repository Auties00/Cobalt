package com.github.auties00.cobalt.wire.linked.media;

import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncNotification;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.media.StickerAction;
import com.github.auties00.cobalt.wire.linked.message.media.MediaMessage;
import com.github.auties00.cobalt.wire.linked.preference.Sticker;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A unified view over every Cobalt type that references a downloadable
 * attachment on WhatsApp's media CDN.
 *
 * <p>Media attachments in WhatsApp come in many shapes: end-to-end encrypted
 * image, video, audio, document, and sticker messages; application state
 * synchronization blobs; history sync payloads; and sticker metadata attached
 * to account preferences. All of them share a common set of fields that
 * describe where the encrypted bytes live on the CDN and how to decrypt and
 * validate them. This sealed interface abstracts those common fields so that
 * the upload and download pipelines can operate on any media-bearing type
 * without special casing.
 *
 * <p>Implementations may choose to leave some accessors empty when the
 * underlying type does not carry that particular field. For example,
 * {@link ExternalBlobReference} has no media URL because external blobs are
 * always retrieved through the direct path.
 */
public sealed interface MediaProvider
        permits StickerAction, MediaMessage, Sticker, ExternalBlobReference, HistorySyncNotification, ExtendedTextMessage {
    /**
     * Returns the CDN URL at which the encrypted media can be downloaded.
     *
     * @return an {@link Optional} containing the URL, or empty if not set
     */
    Optional<String> mediaUrl();

    /**
     * Sets the CDN URL for the media.
     *
     * @param mediaUrl the media URL
     */
    void setMediaUrl(String mediaUrl);

    /**
     * Returns the CDN direct path at which the encrypted media can be fetched.
     *
     * @return an {@link Optional} containing the direct path, or empty if not set
     */
    Optional<String> mediaDirectPath();

    /**
     * Sets the CDN direct path for the media.
     *
     * @param mediaDirectPath the direct path
     */
    void setMediaDirectPath(String mediaDirectPath);

    /**
     * Returns the symmetric key used to decrypt the downloaded media bytes.
     *
     * @return an {@link Optional} containing the media key, or empty if not set
     */
    Optional<byte[]> mediaKey();

    /**
     * Sets the symmetric key used to decrypt the media.
     *
     * @param bytes the media key
     */
    void setMediaKey(byte[] bytes);

    /**
     * Sets the timestamp at which the media key was generated.
     *
     * <p>Implementations that do not track a key timestamp may ignore this
     * call.
     *
     * @param timestamp the media key timestamp, or {@code null} to clear
     */
    void setMediaKeyTimestamp(Instant timestamp);

    /**
     * Returns the SHA-256 digest of the plaintext media, used for integrity
     * verification after decryption.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    Optional<byte[]> mediaSha256();

    /**
     * Sets the SHA-256 digest of the plaintext media.
     *
     * @param bytes the plaintext hash bytes
     */
    void setMediaSha256(byte[] bytes);

    /**
     * Returns the SHA-256 digest of the encrypted media, used for integrity
     * verification before decryption.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    Optional<byte[]> mediaEncryptedSha256();

    /**
     * Sets the SHA-256 digest of the encrypted media.
     *
     * @param bytes the encrypted hash bytes
     */
    void setMediaEncryptedSha256(byte[] bytes);

    /**
     * Returns the size of the media file in bytes.
     *
     * @return an {@link OptionalLong} containing the file size, or empty if not set
     */
    OptionalLong mediaSize();

    /**
     * Sets the size of the media file in bytes.
     *
     * @param mediaSize the file size
     */
    void setMediaSize(long mediaSize);

    /**
     * Returns the media path classification describing the CDN route and
     * encryption key label for this provider's content.
     *
     * @return the media path, never {@code null}
     */
    MediaPath mediaPath();
}
