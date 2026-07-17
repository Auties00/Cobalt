package com.github.auties00.cobalt.wire.linked.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A server-originated notification announcing that a media file can be fetched
 * through an optimized express download path.
 *
 * <p>WhatsApp can make certain media available on a faster CDN route than the
 * default one; when it does, the server emits this notification so the client
 * can bypass the normal download flow and retrieve the file directly from the
 * express path URL. The notification is correlated with the originating media
 * message through the encrypted-file hash, and the file length is included for
 * download validation.
 *
 * <p>Instances of this type are typically received as part of a wider protocol
 * message rather than constructed by client code.
 */
@ProtobufMessage(name = "MediaNotifyMessage")
public final class MediaNotifyMessage {
    /**
     * The optimized CDN URL from which the media file can be retrieved.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String expressPathUrl;

    /**
     * The SHA-256 digest of the encrypted media file, used to match this
     * notification to the corresponding media message.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] fileEncSha256;

    /**
     * The total size of the media file in bytes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64)
    Long fileLength;

    /**
     * Constructs a new {@code MediaNotifyMessage} with the given express-path
     * coordinates.
     *
     * @param expressPathUrl the optimized CDN URL
     * @param fileEncSha256  the SHA-256 digest of the encrypted file
     * @param fileLength     the file size in bytes
     */
    MediaNotifyMessage(String expressPathUrl, byte[] fileEncSha256, Long fileLength) {
        this.expressPathUrl = expressPathUrl;
        this.fileEncSha256 = fileEncSha256;
        this.fileLength = fileLength;
    }

    /**
     * Returns the optimized CDN URL from which the media file can be retrieved.
     *
     * @return an {@link Optional} containing the URL, or empty if not set
     */
    public Optional<String> expressPathUrl() {
        return Optional.ofNullable(expressPathUrl);
    }

    /**
     * Returns the SHA-256 digest of the encrypted media file.
     *
     * @return an {@link Optional} containing the hash bytes, or empty if not set
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    /**
     * Returns the total size of the media file in bytes.
     *
     * @return an {@link OptionalLong} containing the file length, or empty if not set
     */
    public OptionalLong fileLength() {
        return fileLength == null ? OptionalLong.empty() : OptionalLong.of(fileLength);
    }

    /**
     * Sets the optimized CDN URL.
     *
     * @param expressPathUrl the express path URL
     */
    public void setExpressPathUrl(String expressPathUrl) {
        this.expressPathUrl = expressPathUrl;
    }

    /**
     * Sets the SHA-256 digest of the encrypted file.
     *
     * @param fileEncSha256 the encrypted file hash
     */
    public void setFileEncSha256(byte[] fileEncSha256) {
        this.fileEncSha256 = fileEncSha256;
    }

    /**
     * Sets the total size of the media file in bytes.
     *
     * @param fileLength the file length
     */
    public void setFileLength(Long fileLength) {
        this.fileLength = fileLength;
    }
}
