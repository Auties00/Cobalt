package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.ProcessedVideo;
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
 * A message whose payload is an encrypted video file.
 *
 * <p>Video messages are used for regular video attachments, animated GIFs delivered
 * as looping videos, motion photos, view-once videos and AI-generated clips.
 * Alongside the standard media metadata shared with other {@link MediaMessage}
 * types, this class carries video-specific information such as playback duration,
 * dimensions, a streaming sidecar, interactive annotations placed on the video,
 * pre-transcoded quality variants ({@link ProcessedVideo}) and classification
 * fields like {@link Attribution} and {@link VideoSourceType}.
 *
 * <p>Video messages can also be used as headers of interactive messages and as
 * titles of template messages.
 */
@ProtobufMessage(name = "Message.VideoMessage")
public final class VideoMessage implements InteractiveHeader, InteractiveMessage.MediaSpec, TemplateMessage.Title, TemplateMessage.TitleSpec, MediaMessage {
    /**
     * URL of the encrypted video on WhatsApp's media servers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * MIME type of the video container.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String mimetype;

    /**
     * SHA-256 digest of the decrypted video bytes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * Size in bytes of the decrypted video payload.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Playback duration in seconds.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer seconds;

    /**
     * Symmetric key used to decrypt the video payload.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * Optional caption shown under the video.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String caption;

    /**
     * Whether the video should be rendered as a looping, muted GIF.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    Boolean gifPlayback;

    /**
     * Video height in pixels.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    Integer height;

    /**
     * Video width in pixels.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    Integer width;

    /**
     * SHA-256 digest of the encrypted video bytes as stored on the server.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * Legacy interactive annotations anchored on the video.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    List<InteractiveAnnotation> interactiveAnnotations;

    /**
     * CDN direct path of the encrypted video.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * Moment at which the {@link #mediaKey} was generated.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * Inline low-resolution JPEG thumbnail used for instant preview.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    byte[] jpegThumbnail;

    /**
     * Contextual information attached to the video.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Sidecar enabling streaming decryption of the video as it downloads.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    byte[] streamingSidecar;

    /**
     * Third-party GIF service that provided the clip when {@link #gifPlayback} is
     * set.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.ENUM)
    Attribution gifAttribution;

    /**
     * Whether the video can only be viewed once by the recipient.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    Boolean viewOnce;

    /**
     * CDN direct path of the encrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    String thumbnailDirectPath;

    /**
     * SHA-256 digest of the decrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BYTES)
    byte[] thumbnailSha256;

    /**
     * SHA-256 digest of the encrypted high-resolution thumbnail.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    byte[] thumbnailEncSha256;

    /**
     * URL of a static image fallback for the video.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.STRING)
    String staticUrl;

    /**
     * Interactive annotations anchored on the video.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.MESSAGE)
    List<InteractiveAnnotation> annotations;

    /**
     * Accessibility label describing the video for screen readers.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    String accessibilityLabel;

    /**
     * Additional pre-transcoded versions of the same video at different quality
     * tiers.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.MESSAGE)
    List<ProcessedVideo> processedVideos;

    /**
     * When the video is a clip of a longer external video, the full duration of
     * the source clip in seconds.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.UINT32)
    Integer externalShareFullVideoDurationInSeconds;

    /**
     * Presentation offset in milliseconds for motion photos, indicating the point
     * in the video that corresponds to the still image.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.UINT64)
    Long motionPhotoPresentationOffsetMs;

    /**
     * URL of the video's extended metadata document.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.STRING)
    String metadataUrl;

    /**
     * Whether the video was captured by a user or produced by an AI model.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.ENUM)
    VideoSourceType videoSourceType;

    /**
     * Domain identifier that scopes how the {@link #mediaKey} was derived.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.ENUM)
    MediaMessageKeyDomain mediaKeyDomain;


    /**
     * Constructs a new video message with the given metadata.
     *
     * @param mediaUrl                                the CDN URL of the encrypted video
     * @param mimetype                                the video MIME type
     * @param mediaSha256                             the hash of the decrypted video
     * @param mediaSize                               the size of the decrypted video
     * @param seconds                                 the playback duration
     * @param mediaKey                                the decryption key
     * @param caption                                 the caption shown with the video
     * @param gifPlayback                             whether the video should loop as a GIF
     * @param height                                  the video height
     * @param width                                   the video width
     * @param mediaEncryptedSha256                    the hash of the encrypted video
     * @param interactiveAnnotations                  legacy annotations anchored on the video
     * @param mediaDirectPath                         the CDN direct path
     * @param mediaKeyTimestamp                       when the key was generated
     * @param jpegThumbnail                           inline JPEG preview bytes
     * @param contextInfo                             the context information
     * @param streamingSidecar                        streaming decryption sidecar
     * @param gifAttribution                          the GIF source attribution
     * @param viewOnce                                whether the video is single-view
     * @param thumbnailDirectPath                     CDN path of the high-res thumbnail
     * @param thumbnailSha256                         hash of the decrypted thumbnail
     * @param thumbnailEncSha256                      hash of the encrypted thumbnail
     * @param staticUrl                               URL of the static fallback image
     * @param annotations                             annotations anchored on the video
     * @param accessibilityLabel                      accessibility description
     * @param processedVideos                         pre-transcoded quality variants
     * @param externalShareFullVideoDurationInSeconds full source duration for clip sharing
     * @param motionPhotoPresentationOffsetMs         motion photo presentation offset
     * @param metadataUrl                             URL of the extended metadata document
     * @param videoSourceType                         classification of the video source
     * @param mediaKeyDomain                          key derivation domain
     */
    VideoMessage(String mediaUrl, String mimetype, byte[] mediaSha256, Long mediaSize, Integer seconds, byte[] mediaKey, String caption, Boolean gifPlayback, Integer height, Integer width, byte[] mediaEncryptedSha256, List<InteractiveAnnotation> interactiveAnnotations, String mediaDirectPath, Instant mediaKeyTimestamp, byte[] jpegThumbnail, ContextInfo contextInfo, byte[] streamingSidecar, Attribution gifAttribution, Boolean viewOnce, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, String staticUrl, List<InteractiveAnnotation> annotations, String accessibilityLabel, List<ProcessedVideo> processedVideos, Integer externalShareFullVideoDurationInSeconds, Long motionPhotoPresentationOffsetMs, String metadataUrl, VideoSourceType videoSourceType, MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.seconds = seconds;
        this.mediaKey = mediaKey;
        this.caption = caption;
        this.gifPlayback = gifPlayback;
        this.height = height;
        this.width = width;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.interactiveAnnotations = interactiveAnnotations;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.jpegThumbnail = jpegThumbnail;
        this.contextInfo = contextInfo;
        this.streamingSidecar = streamingSidecar;
        this.gifAttribution = gifAttribution;
        this.viewOnce = viewOnce;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.staticUrl = staticUrl;
        this.annotations = annotations;
        this.accessibilityLabel = accessibilityLabel;
        this.processedVideos = processedVideos;
        this.externalShareFullVideoDurationInSeconds = externalShareFullVideoDurationInSeconds;
        this.motionPhotoPresentationOffsetMs = motionPhotoPresentationOffsetMs;
        this.metadataUrl = metadataUrl;
        this.videoSourceType = videoSourceType;
        this.mediaKeyDomain = mediaKeyDomain;
    }

    /**
     * Returns the CDN URL of the encrypted video payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    public Optional<String> url() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the CDN URL of the encrypted video payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the MIME type of the video.
     *
     * @return the MIME type, or empty if unknown
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the SHA-256 digest of the decrypted video.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the decrypted video.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the size in bytes of the decrypted video.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size in bytes of the decrypted video.
     *
     * @return the size, or empty if unknown
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the playback duration in seconds.
     *
     * @return the duration, or empty if unknown
     */
    public OptionalInt seconds() {
        return seconds == null ? OptionalInt.empty() : OptionalInt.of(seconds);
    }

    /**
     * Returns the symmetric key used to decrypt the video payload.
     *
     * @return the key, or empty if unknown
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the caption shown with the video.
     *
     * @return the caption, or empty if none was provided
     */
    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns whether the video should loop silently as a GIF.
     *
     * @return {@code true} if the video is a GIF playback
     */
    public boolean gifPlayback() {
        return gifPlayback != null && gifPlayback;
    }

    /**
     * Returns the video height in pixels.
     *
     * @return the height, or empty if unknown
     */
    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    /**
     * Returns the video width in pixels.
     *
     * @return the width, or empty if unknown
     */
    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    /**
     * Returns the SHA-256 digest of the encrypted video.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted video.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the legacy interactive annotations anchored on the video.
     *
     * @return an unmodifiable list, empty if none are present
     */
    public List<InteractiveAnnotation> interactiveAnnotations() {
        return interactiveAnnotations == null ? List.of() : Collections.unmodifiableList(interactiveAnnotations);
    }

    /**
     * Returns the CDN direct path of the encrypted video.
     *
     * @return the path, or empty if not yet uploaded
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path of the encrypted video.
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
     * Returns the inline JPEG thumbnail bytes.
     *
     * @return the thumbnail, or empty if not present
     */
    public Optional<byte[]> jpegThumbnail() {
        return Optional.ofNullable(jpegThumbnail);
    }

    /**
     * Returns the context information attached to this video message.
     *
     * @return the context info, or empty if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the streaming sidecar enabling progressive decryption.
     *
     * @return the sidecar, or empty if not present
     */
    public Optional<byte[]> streamingSidecar() {
        return Optional.ofNullable(streamingSidecar);
    }

    /**
     * Returns the GIF source attribution.
     *
     * @return the attribution, or empty if the video is not a shared GIF
     */
    public Optional<Attribution> gifAttribution() {
        return Optional.ofNullable(gifAttribution);
    }

    /**
     * Returns whether the video is marked as single-view.
     *
     * @return {@code true} if the video can only be viewed once
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
     * Returns the URL of the static image fallback.
     *
     * @return the URL, or empty if not present
     */
    public Optional<String> staticUrl() {
        return Optional.ofNullable(staticUrl);
    }

    /**
     * Returns the interactive annotations anchored on the video.
     *
     * @return an unmodifiable list, empty if none are present
     */
    public List<InteractiveAnnotation> annotations() {
        return annotations == null ? List.of() : Collections.unmodifiableList(annotations);
    }

    /**
     * Returns the accessibility label describing the video.
     *
     * @return the label, or empty if not provided
     */
    public Optional<String> accessibilityLabel() {
        return Optional.ofNullable(accessibilityLabel);
    }

    /**
     * Returns the pre-transcoded quality variants available for this video.
     *
     * @return an unmodifiable list, empty if only the main variant is available
     */
    public List<ProcessedVideo> processedVideos() {
        return processedVideos == null ? List.of() : Collections.unmodifiableList(processedVideos);
    }

    /**
     * Returns the full duration in seconds of the source clip when this video is
     * an external share snippet.
     *
     * @return the duration, or empty if not applicable
     */
    public OptionalInt externalShareFullVideoDurationInSeconds() {
        return externalShareFullVideoDurationInSeconds == null ? OptionalInt.empty() : OptionalInt.of(externalShareFullVideoDurationInSeconds);
    }

    /**
     * Returns the motion photo presentation offset in milliseconds.
     *
     * @return the offset, or empty if the video is not a motion photo
     */
    public OptionalLong motionPhotoPresentationOffsetMs() {
        return motionPhotoPresentationOffsetMs == null ? OptionalLong.empty() : OptionalLong.of(motionPhotoPresentationOffsetMs);
    }

    /**
     * Returns the URL of the extended metadata document.
     *
     * @return the URL, or empty if no extended metadata exists
     */
    public Optional<String> metadataUrl() {
        return Optional.ofNullable(metadataUrl);
    }

    /**
     * Returns the classification of the video source.
     *
     * @return the source type, or empty if unset
     */
    public Optional<VideoSourceType> videoSourceType() {
        return Optional.ofNullable(videoSourceType);
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
     * Returns the {@link MediaPath} classification for video media.
     *
     * @return {@link MediaPath#VIDEO}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.VIDEO;
    }

    /**
     * Updates the CDN URL of the encrypted video payload.
     *
     * @param mediaUrl the new URL, or {@code null} to clear
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Updates the video MIME type.
     *
     * @param mimetype the new MIME type, or {@code null} to clear
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Updates the SHA-256 digest of the decrypted video.
     *
     * @param mediaSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Updates the size in bytes of the decrypted video.
     *
     * @param mediaSize the new size
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Updates the playback duration in seconds.
     *
     * @param seconds the new duration, or {@code null} to clear
     */
    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }

    /**
     * Updates the symmetric key used to decrypt the video payload.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the caption shown with the video.
     *
     * @param caption the new caption, or {@code null} to clear
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Updates the GIF playback flag.
     *
     * @param gifPlayback {@code true} if the video should loop as a GIF, {@code false} or {@code null} otherwise
     */
    public void setGifPlayback(Boolean gifPlayback) {
        this.gifPlayback = gifPlayback;
    }

    /**
     * Updates the video height.
     *
     * @param height the new height in pixels, or {@code null} to clear
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Updates the video width.
     *
     * @param width the new width in pixels, or {@code null} to clear
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Updates the SHA-256 digest of the encrypted video.
     *
     * @param mediaEncryptedSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Updates the legacy interactive annotations.
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
     * Updates the context information attached to this video message.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the streaming sidecar.
     *
     * @param streamingSidecar the new sidecar, or {@code null} to clear
     */
    public void setStreamingSidecar(byte[] streamingSidecar) {
        this.streamingSidecar = streamingSidecar;
    }

    /**
     * Updates the GIF attribution.
     *
     * @param gifAttribution the new attribution, or {@code null} to clear
     */
    public void setGifAttribution(Attribution gifAttribution) {
        this.gifAttribution = gifAttribution;
    }

    /**
     * Updates the single-view flag.
     *
     * @param viewOnce {@code true} to mark as single-view, {@code false} or {@code null} otherwise
     */
    public void setViewOnce(Boolean viewOnce) {
        this.viewOnce = viewOnce;
    }

    /**
     * Updates the CDN direct path of the high-resolution thumbnail.
     *
     * @param thumbnailDirectPath the new path, or {@code null} to clear
     */
    public void setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
    }

    /**
     * Updates the decrypted thumbnail hash.
     *
     * @param thumbnailSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
    }

    /**
     * Updates the encrypted thumbnail hash.
     *
     * @param thumbnailEncSha256 the new hash, or {@code null} to clear
     */
    public void setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
    }

    /**
     * Updates the static fallback URL.
     *
     * @param staticUrl the new URL, or {@code null} to clear
     */
    public void setStaticUrl(String staticUrl) {
        this.staticUrl = staticUrl;
    }

    /**
     * Updates the interactive annotations.
     *
     * @param annotations the new list, or {@code null} to clear
     */
    public void setAnnotations(List<InteractiveAnnotation> annotations) {
        this.annotations = annotations;
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
     * Updates the pre-transcoded quality variants.
     *
     * @param processedVideos the new list, or {@code null} to clear
     */
    public void setProcessedVideos(List<ProcessedVideo> processedVideos) {
        this.processedVideos = processedVideos;
    }

    /**
     * Updates the external share full video duration.
     *
     * @param externalShareFullVideoDurationInSeconds the new duration, or {@code null} to clear
     */
    public void setExternalShareFullVideoDurationInSeconds(Integer externalShareFullVideoDurationInSeconds) {
        this.externalShareFullVideoDurationInSeconds = externalShareFullVideoDurationInSeconds;
    }

    /**
     * Updates the motion photo presentation offset.
     *
     * @param motionPhotoPresentationOffsetMs the new offset, or {@code null} to clear
     */
    public void setMotionPhotoPresentationOffsetMs(Long motionPhotoPresentationOffsetMs) {
        this.motionPhotoPresentationOffsetMs = motionPhotoPresentationOffsetMs;
    }

    /**
     * Updates the extended metadata document URL.
     *
     * @param metadataUrl the new URL, or {@code null} to clear
     */
    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    /**
     * Updates the video source classification.
     *
     * @param videoSourceType the new source type, or {@code null} to clear
     */
    public void setVideoSourceType(VideoSourceType videoSourceType) {
        this.videoSourceType = videoSourceType;
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
     * Third-party GIF service that provided a shared GIF.
     *
     * <p>Used for attribution purposes when a GIF is reshared from a third-party
     * catalog.
     */
    @ProtobufEnum(name = "Message.VideoMessage.Attribution")
    public static enum Attribution {
        /**
         * No third-party attribution is set.
         */
        NONE(0),
        /**
         * GIF sourced from the Giphy catalog.
         */
        GIPHY(1),
        /**
         * GIF sourced from the Tenor catalog.
         */
        TENOR(2),
        /**
         * GIF sourced from the Klipy catalog.
         */
        KLIPY(3);

        /**
         * Constructs a new enum constant.
         *
         * @param index the protobuf wire index used to serialize this constant
         */
        Attribution(@ProtobufEnumIndex int index) {
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

    /**
     * Classification of the origin of a video.
     *
     * <p>Used to flag AI-generated videos so that clients can render them with
     * appropriate disclaimers.
     */
    @ProtobufEnum(name = "Message.VideoMessage.VideoSourceType")
    public static enum VideoSourceType {
        /**
         * The video was captured or supplied directly by a user.
         */
        USER_VIDEO(0),
        /**
         * The video was fully generated by an AI model.
         */
        AI_GENERATED(1);

        /**
         * Constructs a new enum constant.
         *
         * @param index the protobuf wire index used to serialize this constant
         */
        VideoSourceType(@ProtobufEnumIndex int index) {
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
