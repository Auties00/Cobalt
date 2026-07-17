package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAnnotation;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveHeader;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * A message whose payload is an encrypted image file.
 *
 * <p>Image messages are used for regular photo attachments, view-once images, QR
 * code statuses, and AI-generated or AI-modified pictures. Alongside the
 * standard media metadata (CDN URL, integrity hashes, decryption key), this class
 * carries image-specific fields such as dimensions, progressive JPEG scan sidecars,
 * inline JPEG previews, on-image interactive annotations (stickers placed by the
 * sender) and the image source type.
 *
 * <p>Image messages can also be used as headers of interactive messages and as
 * titles of template messages.
 */
@ProtobufMessage(name = "Message.ImageMessage")
public final class ImageMessage implements InteractiveHeader, InteractiveMessage.MediaSpec, TemplateMessage.Title, TemplateMessage.TitleSpec, MediaMessage {
    /**
     * URL of the encrypted image on WhatsApp's media servers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * MIME type of the image, typically {@code image/jpeg} or {@code image/webp}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String mimetype;

    /**
     * Optional text caption shown underneath the image.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String caption;

    /**
     * SHA-256 digest of the decrypted image bytes, used to verify integrity.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * Size in bytes of the decrypted image payload.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Image height in pixels.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    Integer height;

    /**
     * Image width in pixels.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer width;

    /**
     * Symmetric key used to decrypt the image payload.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * SHA-256 digest of the encrypted image bytes as stored on the server.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * Interactive annotations (stickers, mentions, location tags) anchored on the
     * image at send time.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    List<InteractiveAnnotation> interactiveAnnotations;

    /**
     * CDN direct path of the encrypted image.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * Moment at which the {@link #mediaKey} was generated.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * Inline low-resolution JPEG thumbnail used for instant preview before the full
     * image downloads.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * Contextual information attached to the image, such as a quoted message or
     * forwarding metadata.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Sidecar holding the data needed to decrypt the first progressive JPEG scan
     * before the whole file is available.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    byte[] firstScanSidecar;

    /**
     * Length in bytes of the first progressive JPEG scan.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
    Integer firstScanLength;

    /**
     * Experiment group identifier used for progressive rendering A/B tests.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
    Integer experimentGroupId;

    /**
     * Sidecar holding decryption data for every progressive JPEG scan.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    byte[] scansSidecar;

    /**
     * Length in bytes of each progressive JPEG scan.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.UINT32)
    List<Integer> scanLengths;

    /**
     * SHA-256 digest of the decrypted mid-quality fallback image.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    byte[] midQualityFileSha256;

    /**
     * SHA-256 digest of the encrypted mid-quality fallback image.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.BYTES)
    byte[] midQualityFileEncSha256;

    /**
     * Whether the image can only be viewed once by the recipient before being
     * discarded.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    Boolean viewOnce;

    /**
     * CDN direct path of the encrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * SHA-256 digest of the decrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * SHA-256 digest of the encrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * URL of a static (non-animated) fallback asset used in contexts where
     * animation is not allowed.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.STRING)
    String staticUrl;

    /**
     * Newer interactive annotation list replacing {@link #interactiveAnnotations}.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.MESSAGE)
    List<InteractiveAnnotation> annotations;

    /**
     * Whether the image was taken by a user, AI-generated or AI-modified.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.ENUM)
    ImageSourceType imageSourceType;

    /**
     * Accessibility label describing the image for screen readers.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.STRING)
    String accessibilityLabel;

    /**
     * Domain identifier that scopes how the {@link #mediaKey} was derived.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.ENUM)
    MediaMessageKeyDomain mediaKeyDomain;

    /**
     * URL encoded in the image when it renders a QR code.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.STRING)
    String qrUrl;


    /**
     * Constructs a new image message with the given metadata.
     *
     * @param mediaUrl                the CDN URL of the encrypted image
     * @param mimetype                the image MIME type
     * @param caption                 a caption shown below the image
     * @param mediaSha256             the hash of the decrypted image
     * @param mediaSize               the size of the decrypted image
     * @param height                  the image height in pixels
     * @param width                   the image width in pixels
     * @param mediaKey                the decryption key
     * @param mediaEncryptedSha256    the hash of the encrypted image
     * @param interactiveAnnotations  legacy interactive annotations anchored on the image
     * @param mediaDirectPath         the CDN direct path
     * @param mediaKeyTimestamp       when the key was generated
     * @param jpegThumbnail           inline low-resolution JPEG preview bytes
     * @param contextInfo             the context information
     * @param firstScanSidecar        first progressive scan sidecar
     * @param firstScanLength         first progressive scan length
     * @param experimentGroupId       progressive rendering experiment id
     * @param scansSidecar            sidecar for all progressive scans
     * @param scanLengths             length of each progressive scan
     * @param midQualityFileSha256    hash of the decrypted mid-quality fallback
     * @param midQualityFileEncSha256 hash of the encrypted mid-quality fallback
     * @param viewOnce                whether the image is single-view
     * @param thumbnailDirectPath     CDN path of the encrypted hi-res thumbnail
     * @param thumbnailSha256         hash of the decrypted hi-res thumbnail
     * @param thumbnailEncSha256      hash of the encrypted hi-res thumbnail
     * @param staticUrl               URL of the static fallback asset
     * @param annotations             interactive annotations anchored on the image
     * @param imageSourceType         classification of the image source
     * @param accessibilityLabel      accessibility description
     * @param mediaKeyDomain          key derivation domain
     * @param qrUrl                   URL encoded in the image's QR code, if any
     */
    ImageMessage(String mediaUrl, String mimetype, String caption, byte[] mediaSha256, Long mediaSize, Integer height, Integer width, byte[] mediaKey, byte[] mediaEncryptedSha256, List<InteractiveAnnotation> interactiveAnnotations, String mediaDirectPath, Instant mediaKeyTimestamp, byte[] jpegThumbnail, ContextInfo contextInfo, byte[] firstScanSidecar, Integer firstScanLength, Integer experimentGroupId, byte[] scansSidecar, List<Integer> scanLengths, byte[] midQualityFileSha256, byte[] midQualityFileEncSha256, Boolean viewOnce, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, String staticUrl, List<InteractiveAnnotation> annotations, ImageSourceType imageSourceType, String accessibilityLabel, MediaMessageKeyDomain mediaKeyDomain, String qrUrl) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.caption = caption;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.height = height;
        this.width = width;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.interactiveAnnotations = interactiveAnnotations;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
        this.firstScanSidecar = firstScanSidecar;
        this.firstScanLength = firstScanLength;
        this.experimentGroupId = experimentGroupId;
        this.scansSidecar = scansSidecar;
        this.scanLengths = scanLengths;
        this.midQualityFileSha256 = midQualityFileSha256;
        this.midQualityFileEncSha256 = midQualityFileEncSha256;
        this.viewOnce = viewOnce;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.staticUrl = staticUrl;
        this.annotations = annotations;
        this.imageSourceType = imageSourceType;
        this.accessibilityLabel = accessibilityLabel;
        this.mediaKeyDomain = mediaKeyDomain;
        this.qrUrl = qrUrl;
    }

    /**
     * Returns the CDN URL of the encrypted image payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    public Optional<String> url() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the CDN URL of the encrypted image payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the MIME type of the image.
     *
     * @return the MIME type, or empty if unknown
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the caption shown with the image.
     *
     * @return the caption, or empty if none was provided
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the SHA-256 digest of the decrypted image.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the decrypted image.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the size in bytes of the decrypted image.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size in bytes of the decrypted image.
     *
     * @return the size, or empty if unknown
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the image height in pixels.
     *
     * @return the height, or empty if unknown
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the image width in pixels.
     *
     * @return the width, or empty if unknown
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the decryption key for the image payload.
     *
     * @return the key, or empty if unknown
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the SHA-256 digest of the encrypted image.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted image.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the legacy interactive annotations anchored on the image.
     *
     * @return an unmodifiable list, empty if none are present
     */
    public List<InteractiveAnnotation> interactiveAnnotations() {
        return interactiveAnnotations == null ? List.of() : Collections.unmodifiableList(interactiveAnnotations);
    }

    /**
     * Returns the CDN direct path of the encrypted image.
     *
     * @return the path, or empty if not yet uploaded
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path of the encrypted image.
     *
     * @return the path, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the moment at which the media key was generated.
     *
     * @return the timestamp, or empty if unknown
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the inline low-resolution JPEG preview bytes.
     *
     * @return the thumbnail bytes, or empty if not present
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the context information attached to this image message.
     *
     * @return the context info, or empty if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the sidecar enabling decryption of the first progressive JPEG scan.
     *
     * @return the sidecar, or empty if not present
     */
    public Optional<byte[]> firstScanSidecar() {
        return Optional.ofNullable(firstScanSidecar);
    }

    /**
     * Returns the length in bytes of the first progressive JPEG scan.
     *
     * @return the length, or empty if unknown
     */
    public OptionalInt firstScanLength() {
        return firstScanLength == null ? OptionalInt.empty() : OptionalInt.of(firstScanLength);
    }

    /**
     * Returns the experiment group identifier for progressive rendering.
     *
     * @return the identifier, or empty if unset
     */
    public OptionalInt experimentGroupId() {
        return experimentGroupId == null ? OptionalInt.empty() : OptionalInt.of(experimentGroupId);
    }

    /**
     * Returns the sidecar enabling decryption of all progressive JPEG scans.
     *
     * @return the sidecar, or empty if not present
     */
    public Optional<byte[]> scansSidecar() {
        return Optional.ofNullable(scansSidecar);
    }

    /**
     * Returns the length in bytes of each progressive JPEG scan.
     *
     * @return an unmodifiable list of lengths, empty if unknown
     */
    public List<Integer> scanLengths() {
        return scanLengths == null ? List.of() : Collections.unmodifiableList(scanLengths);
    }

    /**
     * Returns the SHA-256 digest of the decrypted mid-quality fallback image.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> midQualityFileSha256() {
        return Optional.ofNullable(midQualityFileSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted mid-quality fallback image.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> midQualityFileEncSha256() {
        return Optional.ofNullable(midQualityFileEncSha256);
    }

    /**
     * Returns whether the image is marked as single-view.
     *
     * @return {@code true} if the image can only be viewed once
     */
    public boolean viewOnce() {
        return viewOnce != null && viewOnce;
    }

    /**
     * Returns the CDN direct path of the encrypted high-resolution thumbnail.
     *
     * @return the path, or empty if not present
     */
    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the decrypted high-resolution thumbnail.
     *
     * @return the hash, or empty if not present
     */
    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted high-resolution thumbnail.
     *
     * @return the hash, or empty if not present
     */
    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    /**
     * Returns the URL of the static (non-animated) fallback asset.
     *
     * @return the URL, or empty if not present
     */
    public Optional<String> staticUrl() {
        return Optional.ofNullable(staticUrl);
    }

    /**
     * Returns the interactive annotations anchored on the image.
     *
     * @return an unmodifiable list, empty if none are present
     */
    public List<InteractiveAnnotation> annotations() {
        return annotations == null ? List.of() : Collections.unmodifiableList(annotations);
    }

    /**
     * Returns the classification of the image source.
     *
     * @return the source type, or empty if unset
     */
    public Optional<ImageSourceType> imageSourceType() {
        return Optional.ofNullable(imageSourceType);
    }

    /**
     * Returns the accessibility label describing the image.
     *
     * @return the label, or empty if not provided
     */
    public Optional<String> accessibilityLabel() {
        return Optional.ofNullable(accessibilityLabel);
    }

    /**
     * Returns the domain identifier that scopes how the media key was derived.
     *
     * @return the key domain, or empty if unset
     */
    public Optional<MediaMessageKeyDomain> mediaKeyDomain() {
        return Optional.ofNullable(mediaKeyDomain);
    }

    /**
     * Returns the URL encoded in the image when it renders a QR code.
     *
     * @return the QR URL, or empty if the image is not a QR code
     */
    public Optional<String> qrUrl() {
        return Optional.ofNullable(qrUrl);
    }

    /**
     * Returns the {@link MediaPath} classification for image media, used to select
     * the correct CDN upload/download endpoint.
     *
     * @return {@link MediaPath#IMAGE}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.IMAGE;
    }

    /**
     * Updates the CDN URL of the encrypted image payload.
     *
     * @param mediaUrl the new URL, or {@code null} to clear
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Updates the image MIME type.
     *
     * @param mimetype the new MIME type, or {@code null} to clear
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Updates the caption.
     *
     * @param caption the new caption, or {@code null} to clear
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Updates the SHA-256 digest of the decrypted image.
     *
     * @param mediaSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Updates the size in bytes of the decrypted image.
     *
     * @param mediaSize the new size
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Updates the image height.
     *
     * @param height the new height in pixels, or {@code null} to clear
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Updates the image width.
     *
     * @param width the new width in pixels, or {@code null} to clear
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Updates the symmetric key used to decrypt the image payload.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the SHA-256 digest of the encrypted image.
     *
     * @param mediaEncryptedSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Updates the legacy interactive annotations anchored on the image.
     *
     * @param interactiveAnnotations the new list, or {@code null} to clear
     */
    public void setInteractiveAnnotations(List<InteractiveAnnotation> interactiveAnnotations) {
        this.interactiveAnnotations = interactiveAnnotations;
    }

    /**
     * Updates the CDN direct path.
     *
     * @param mediaDirectPath the new path, or {@code null} to clear
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Updates the moment at which the media key was generated.
     *
     * @param mediaKeyTimestamp the new timestamp, or {@code null} to clear
     */
    @Override
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Updates the inline JPEG thumbnail bytes.
     *
     * @param jpegThumbnail the new thumbnail, or {@code null} to clear
     */
    public void setJpegThumbnail(byte[] jpegThumbnail) {
        this.jpegThumbnail = jpegThumbnail;
    }

    /**
     * Updates the context information attached to this image message.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the first progressive scan sidecar.
     *
     * @param firstScanSidecar the new sidecar, or {@code null} to clear
     */
    public void setFirstScanSidecar(byte[] firstScanSidecar) {
        this.firstScanSidecar = firstScanSidecar;
    }

    /**
     * Updates the length of the first progressive scan.
     *
     * @param firstScanLength the new length, or {@code null} to clear
     */
    public void setFirstScanLength(Integer firstScanLength) {
        this.firstScanLength = firstScanLength;
    }

    /**
     * Updates the progressive rendering experiment identifier.
     *
     * @param experimentGroupId the new identifier, or {@code null} to clear
     */
    public void setExperimentGroupId(Integer experimentGroupId) {
        this.experimentGroupId = experimentGroupId;
    }

    /**
     * Updates the sidecar for all progressive JPEG scans.
     *
     * @param scansSidecar the new sidecar, or {@code null} to clear
     */
    public void setScansSidecar(byte[] scansSidecar) {
        this.scansSidecar = scansSidecar;
    }

    /**
     * Updates the lengths of the progressive JPEG scans.
     *
     * @param scanLengths the new list of lengths, or {@code null} to clear
     */
    public void setScanLengths(List<Integer> scanLengths) {
        this.scanLengths = scanLengths;
    }

    /**
     * Updates the SHA-256 digest of the decrypted mid-quality image.
     *
     * @param midQualityFileSha256 the new hash, or {@code null} to clear
     */
    public void setMidQualityFileSha256(byte[] midQualityFileSha256) {
        this.midQualityFileSha256 = midQualityFileSha256;
    }

    /**
     * Updates the SHA-256 digest of the encrypted mid-quality image.
     *
     * @param midQualityFileEncSha256 the new hash, or {@code null} to clear
     */
    public void setMidQualityFileEncSha256(byte[] midQualityFileEncSha256) {
        this.midQualityFileEncSha256 = midQualityFileEncSha256;
    }

    /**
     * Updates the single-view flag.
     *
     * @param viewOnce {@code true} to mark the image as single-view, {@code false} or {@code null} otherwise
     */
    public void setViewOnce(Boolean viewOnce) {
        this.viewOnce = viewOnce;
    }

    /**
     * Updates the CDN path of the encrypted high-resolution thumbnail.
     *
     * @param thumbnailDirectPath the new path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Updates the SHA-256 digest of the decrypted thumbnail.
     *
     * @param thumbnailSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Updates the SHA-256 digest of the encrypted thumbnail.
     *
     * @param thumbnailEncSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Updates the URL of the static fallback asset.
     *
     * @param staticUrl the new URL, or {@code null} to clear
     */
    public void setStaticUrl(String staticUrl) {
        this.staticUrl = staticUrl;
    }

    /**
     * Updates the interactive annotations anchored on the image.
     *
     * @param annotations the new list, or {@code null} to clear
     */
    public void setAnnotations(List<InteractiveAnnotation> annotations) {
        this.annotations = annotations;
    }

    /**
     * Updates the image source classification.
     *
     * @param imageSourceType the new source type, or {@code null} to clear
     */
    public void setImageSourceType(ImageSourceType imageSourceType) {
        this.imageSourceType = imageSourceType;
    }

    /**
     * Updates the accessibility label.
     *
     * @param accessibilityLabel the new label, or {@code null} to clear
     */
    public void setAccessibilityLabel(String accessibilityLabel) {
        this.accessibilityLabel = accessibilityLabel;
    }

    /**
     * Updates the key derivation domain.
     *
     * @param mediaKeyDomain the new domain, or {@code null} to clear
     */
    public void setMediaKeyDomain(MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaKeyDomain = mediaKeyDomain;
    }

    /**
     * Updates the QR code URL encoded in the image.
     *
     * @param qrUrl the new URL, or {@code null} to clear
     */
    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    /**
     * Classification of the origin of an image.
     *
     * <p>Used to flag AI-generated or AI-modified pictures and rasterized text
     * statuses so that clients can render them with appropriate disclaimers.
     */
    @ProtobufEnum(name = "Message.ImageMessage.ImageSourceType")
    public static enum ImageSourceType {
        /**
         * The image was captured or supplied directly by a user.
         */
        USER_IMAGE(0),
        /**
         * The image was fully generated by an AI model.
         */
        AI_GENERATED(1),
        /**
         * The image was modified by an AI model after being supplied by the user.
         */
        AI_MODIFIED(2),
        /**
         * The image is a rasterized text status.
         */
        RASTERIZED_TEXT_STATUS(3);

        /**
         * Constructs a new enum constant.
         *
         * @param index the protobuf wire index used to serialize this constant
         */
        ImageSourceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the index
         */
        public int index() {
            return this.index;
        }
    }
}
