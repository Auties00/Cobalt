package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Holds the user preferences that control whether incoming media attachments
 * are downloaded automatically for a given network context.
 *
 * <p>WhatsApp stores three independent profiles of this settings object, one
 * for each connectivity type (Wi-Fi, cellular data, and roaming), so that the
 * user can choose a different policy depending on how the device is online.
 * Each flag toggles automatic download for a single media category; when the
 * flag is {@code false} the media must be fetched explicitly by the user.
 *
 * @see GlobalSettings
 */
@ProtobufMessage(name = "AutoDownloadSettings")
public final class AutoDownloadSettings {
    /**
     * Whether incoming image messages are downloaded automatically.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean downloadImages;

    /**
     * Whether incoming audio messages are downloaded automatically.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean downloadAudio;

    /**
     * Whether incoming video messages are downloaded automatically.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean downloadVideo;

    /**
     * Whether incoming document messages are downloaded automatically.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean downloadDocuments;


    /**
     * Constructs a new auto-download settings instance with the given flags.
     *
     * @param downloadImages    whether images should be downloaded automatically, may be {@code null}
     * @param downloadAudio     whether audio should be downloaded automatically, may be {@code null}
     * @param downloadVideo     whether video should be downloaded automatically, may be {@code null}
     * @param downloadDocuments whether documents should be downloaded automatically, may be {@code null}
     */
    AutoDownloadSettings(Boolean downloadImages, Boolean downloadAudio, Boolean downloadVideo, Boolean downloadDocuments) {
        this.downloadImages = downloadImages;
        this.downloadAudio = downloadAudio;
        this.downloadVideo = downloadVideo;
        this.downloadDocuments = downloadDocuments;
    }

    /**
     * Returns whether incoming image messages are downloaded automatically.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if images are auto-downloaded, {@code false} otherwise
     */
    public boolean downloadImages() {
        return downloadImages != null && downloadImages;
    }

    /**
     * Returns whether incoming audio messages are downloaded automatically.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if audio is auto-downloaded, {@code false} otherwise
     */
    public boolean downloadAudio() {
        return downloadAudio != null && downloadAudio;
    }

    /**
     * Returns whether incoming video messages are downloaded automatically.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if video is auto-downloaded, {@code false} otherwise
     */
    public boolean downloadVideo() {
        return downloadVideo != null && downloadVideo;
    }

    /**
     * Returns whether incoming document messages are downloaded automatically.
     *
     * <p>A missing value is interpreted as {@code false}.
     *
     * @return {@code true} if documents are auto-downloaded, {@code false} otherwise
     */
    public boolean downloadDocuments() {
        return downloadDocuments != null && downloadDocuments;
    }

    /**
     * Updates the automatic download policy for image messages.
     *
     * @param downloadImages the new policy, or {@code null} to unset the field
     */
    public void setDownloadImages(Boolean downloadImages) {
        this.downloadImages = downloadImages;
    }

    /**
     * Updates the automatic download policy for audio messages.
     *
     * @param downloadAudio the new policy, or {@code null} to unset the field
     */
    public void setDownloadAudio(Boolean downloadAudio) {
        this.downloadAudio = downloadAudio;
    }

    /**
     * Updates the automatic download policy for video messages.
     *
     * @param downloadVideo the new policy, or {@code null} to unset the field
     */
    public void setDownloadVideo(Boolean downloadVideo) {
        this.downloadVideo = downloadVideo;
    }

    /**
     * Updates the automatic download policy for document messages.
     *
     * @param downloadDocuments the new policy, or {@code null} to unset the field
     */
    public void setDownloadDocuments(Boolean downloadDocuments) {
        this.downloadDocuments = downloadDocuments;
    }
}
