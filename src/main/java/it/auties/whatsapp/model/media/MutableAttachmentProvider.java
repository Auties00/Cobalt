package it.auties.whatsapp.model.media;

import it.auties.whatsapp.model.action.StickerAction;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.sync.ExternalBlobReference;
import it.auties.whatsapp.model.sync.HistorySyncNotification;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A sealed interface that represents a class that can provide data about a media
 */
public sealed interface MutableAttachmentProvider
        permits StickerAction, MediaMessage, ExternalBlobReference, HistorySyncNotification {
    /**
     * Returns the url to the media
     *
     * @return a nullable String
     */
    Optional<String> mediaUrl();

    /**
     * Sets the media url of this provider
     *
     */
    void setMediaUrl(String mediaUrl);

    /**
     * Returns the direct path to the media
     *
     * @return a nullable String
     */
    Optional<String> mediaDirectPath();

    /**
     * Sets the direct path of this provider
     *
     */
    void setMediaDirectPath(String mediaDirectPath);

    /**
     * Returns the key of this media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaKey();

    /**
     * Sets the media key of this provider
     *
     */
    void setMediaKey(byte[] bytes);

    /**
     * Sets the timestamp of the media key
     *
     */
    void setMediaKeyTimestamp(Long timestamp);

    /**
     * Returns the sha256 of this media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaSha256();

    /**
     * Sets the sha256 of the media in this provider
     *
     */
    void setMediaSha256(byte[] bytes);

    /**
     * Returns the sha256 of this encrypted media
     *
     * @return a non-null array of bytes
     */
    Optional<byte[]> mediaEncryptedSha256();

    /**
     * Sets the sha256 of the encrypted media in this provider
     *
     */
    void setMediaEncryptedSha256(byte[] bytes);

    /**
     * Returns the size of this media
     *
     * @return a long
     */
    OptionalLong mediaSize();

    /**
     * Sets the size of this media
     *
     */
    void setMediaSize(long mediaSize);


    /**
     * Returns the type of this attachment
     *
     * @return a non-null attachment
     */
    AttachmentType attachmentType();
}
