package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Local storage metadata attached to a message that references a media
 * attachment downloaded or cached on the current device.
 *
 * <p>Messages carrying media content can be accompanied by an instance of this
 * type to record where the decrypted file resides on the local filesystem.
 * This allows the client to display or replay the media without re-downloading
 * it from the CDN. The field is populated by the media pipeline after a
 * successful download and is typically empty on messages that have not yet
 * been downloaded or whose payload has been evicted from local storage.
 *
 * <p>This metadata is local to the device; it is not serialized for transport
 * to other devices or to the server.
 */
@ProtobufMessage(name = "MediaData")
public final class MediaData {
    /**
     * The absolute or relative path on the local filesystem where the
     * decrypted media file is stored.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String localPath;

    /**
     * Constructs a new {@code MediaData} pointing at the given local path.
     *
     * @param localPath the local filesystem path to the media file
     */
    MediaData(String localPath) {
        this.localPath = localPath;
    }

    /**
     * Returns the local filesystem path where the media file is stored.
     *
     * @return an {@link Optional} containing the local path, or empty if not set
     */
    public Optional<String> localPath() {
        return Optional.ofNullable(localPath);
    }

    /**
     * Sets the local filesystem path where the media file is stored.
     *
     * @param localPath the local path
     */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
