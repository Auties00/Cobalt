package it.auties.whatsapp.model.media;

import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.sync.ExternalBlobReference;
import it.auties.whatsapp.model.sync.HistorySyncNotification;

/**
 * A sealed interface that represents a class that can provide data about a media
 */
public sealed interface AttachmentProvider permits MediaMessage, ExternalBlobReference, HistorySyncNotification {
    /**
     * Returns the url to the media
     *
     * @return a nullable String
     */
    String mediaUrl();

    /**
     * Sets the media url of this provider
     *
     * @return the same provider
     */
    AttachmentProvider mediaUrl(String mediaUrl);

    /**
     * Returns the direct path to the media
     *
     * @return a nullable String
     */
    String mediaDirectPath();

    /**
     * Sets the direct path of this provider
     *
     * @return the same provider
     */
    AttachmentProvider mediaDirectPath(String mediaDirectPath);

    /**
     * Returns the key of this media
     *
     * @return a non-null array of bytes
     */
    byte[] mediaKey();

    /**
     * Sets the media key of this provider
     *
     * @return the same provider
     */
    AttachmentProvider mediaKey(byte[] bytes);

    /**
     * Returns the sha256 of this media
     *
     * @return a non-null array of bytes
     */
    byte[] mediaSha256();

    /**
     * Sets the sha256 of the media in this provider
     *
     * @return the same provider
     */
    AttachmentProvider mediaSha256(byte[] bytes);

    /**
     * Returns the sha256 of this encrypted media
     *
     * @return a non-null array of bytes
     */
    byte[] mediaEncryptedSha256();

    /**
     * Sets the sha256 of the encrypted media in this provider
     *
     * @return the same provider
     */
    AttachmentProvider mediaEncryptedSha256(byte[] bytes);

    /**
     * Returns the size of this media
     *
     * @return a long
     */
    long mediaSize();

    /**
     * Sets the size of this media
     *
     * @return a long
     */
    AttachmentProvider mediaSize(long mediaSize);

    /**
     * Returns the name of this media for Whatsapp
     *
     * @return a non-null String
     */
    String mediaName();
}
