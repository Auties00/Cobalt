package com.github.auties00.cobalt.wire.linked.reporting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * One media-upload attachment carried by a {@link BugReport}. Mirrors
 * the wire-level
 * {@code com.github.auties00.cobalt.node.smax.bugreporting.SmaxBugReportingReportBugMediaUpload}
 * tuple: encryption IV, cipher key, optional MIME type, optional file
 * name, and the encrypted content bytes.
 *
 * <p>All five fields are independently optional — the relay accepts
 * partial entries and surfaces them as placeholder media attachments
 * in the bug-report triage UI.
 */
@ProtobufMessage
public final class BugReportMediaUpload {
    /**
     * 16-byte IV used to encrypt the upload (base64 / hex string).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String mediaIv;

    /**
     * 32-byte cipher key used to encrypt the upload.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String mediaCipherKey;

    /**
     * Optional MIME type (e.g. {@code "image/jpeg"}).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String mediaType;

    /**
     * Optional original file name.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String mediaFileName;

    /**
     * Encrypted content bytes.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BYTES)
    final byte[] mediaElementValue;

    /**
     * Constructs a new {@code BugReportMediaUpload}.
     *
     * @param mediaIv           the encryption IV, or {@code null}
     * @param mediaCipherKey    the cipher key, or {@code null}
     * @param mediaType         the optional MIME type, or {@code null}
     * @param mediaFileName     the optional file name, or {@code null}
     * @param mediaElementValue the encrypted content bytes, or {@code null}
     */
    BugReportMediaUpload(String mediaIv, String mediaCipherKey, String mediaType,
                         String mediaFileName, byte[] mediaElementValue) {
        this.mediaIv = mediaIv;
        this.mediaCipherKey = mediaCipherKey;
        this.mediaType = mediaType;
        this.mediaFileName = mediaFileName;
        this.mediaElementValue = mediaElementValue;
    }

    /**
     * Returns the encryption IV.
     *
     * @return an {@link Optional} carrying the IV, or empty when unset
     */
    public Optional<String> mediaIv() {
        return Optional.ofNullable(mediaIv);
    }

    /**
     * Returns the cipher key.
     *
     * @return an {@link Optional} carrying the key, or empty when unset
     */
    public Optional<String> mediaCipherKey() {
        return Optional.ofNullable(mediaCipherKey);
    }

    /**
     * Returns the MIME type.
     *
     * @return an {@link Optional} carrying the MIME type, or empty when unset
     */
    public Optional<String> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    /**
     * Returns the original file name.
     *
     * @return an {@link Optional} carrying the file name, or empty when unset
     */
    public Optional<String> mediaFileName() {
        return Optional.ofNullable(mediaFileName);
    }

    /**
     * Returns the encrypted content bytes.
     *
     * @return an {@link Optional} carrying the bytes, or empty when unset
     */
    public Optional<byte[]> mediaElementValue() {
        return Optional.ofNullable(mediaElementValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BugReportMediaUpload) obj;
        return Objects.equals(mediaIv, that.mediaIv) &&
                Objects.equals(mediaCipherKey, that.mediaCipherKey) &&
                Objects.equals(mediaType, that.mediaType) &&
                Objects.equals(mediaFileName, that.mediaFileName) &&
                Arrays.equals(mediaElementValue, that.mediaElementValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaIv, mediaCipherKey, mediaType, mediaFileName, Arrays.hashCode(mediaElementValue));
    }

    @Override
    public String toString() {
        return "BugReportMediaUpload[" +
                "mediaIv=" + mediaIv + ", " +
                "mediaCipherKey=" + mediaCipherKey + ", " +
                "mediaType=" + mediaType + ", " +
                "mediaFileName=" + mediaFileName + ", " +
                "mediaElementValue=" + (mediaElementValue == null ? "null" : mediaElementValue.length + " bytes") + ']';
    }
}
