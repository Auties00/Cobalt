package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.interactive.InteractiveHeaderAttachment;
import it.auties.whatsapp.model.button.interactive.InteractiveLocationAnnotation;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import java.time.ZonedDateTime;
import java.util.*;

import static it.auties.whatsapp.model.message.model.MediaMessageType.VIDEO;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a message holding a video inside
 */
@ProtobufMessage(name = "Message.VideoMessage")
public final class VideoOrGifMessage extends MediaMessage<VideoOrGifMessage>
        implements InteractiveHeaderAttachment, ButtonsMessageHeader, HighlyStructuredFourRowTemplateTitle, HydratedFourRowTemplateTitle {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String mediaUrl;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String mimetype;
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    private Long mediaSize;
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    private final Integer duration;
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String caption;
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    private final boolean gifPlayback;
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    private final Integer height;
    @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
    private final Integer width;
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    private byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    private final List<InteractiveLocationAnnotation> interactiveAnnotations;
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 14, type = ProtobufType.INT64)
    private long mediaKeyTimestampSeconds;
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    private final byte[] streamingSidecar;
    @ProtobufProperty(index = 19, type = ProtobufType.ENUM)
    private final Attribution gifAttribution;
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    private final boolean viewOnce;
    @ProtobufProperty(index = 21, type = ProtobufType.STRING)
    private final String thumbnailDirectPath;
    @ProtobufProperty(index = 22, type = ProtobufType.BYTES)
    private final byte[] thumbnailSha256;
    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    private final byte[] thumbnailEncSha256;
    @ProtobufProperty(index = 24, type = ProtobufType.STRING)
    private final String staticUrl;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VideoOrGifMessage(String mediaUrl, String mimetype, byte[] mediaSha256, Long mediaSize, Integer duration, byte[] mediaKey, String caption, boolean gifPlayback, Integer height, Integer width, byte[] mediaEncryptedSha256, List<InteractiveLocationAnnotation> interactiveAnnotations, String mediaDirectPath, long mediaKeyTimestampSeconds, byte[] thumbnail, ContextInfo contextInfo, byte[] streamingSidecar, Attribution gifAttribution, boolean viewOnce, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, String staticUrl) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.duration = duration;
        this.mediaKey = mediaKey;
        this.caption = caption;
        this.gifPlayback = gifPlayback;
        this.height = height;
        this.width = width;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.interactiveAnnotations = interactiveAnnotations;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
        this.streamingSidecar = streamingSidecar;
        this.gifAttribution = gifAttribution;
        this.viewOnce = viewOnce;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.staticUrl = staticUrl;
    }

    @ProtobufBuilder(className = "VideoMessageSimpleBuilder")
    static VideoOrGifMessage videoBuilder(byte[] media, String mimeType, String caption, byte[] thumbnail, ContextInfo contextInfo) {
        var dimensions = Medias.getDimensions(media, true);
        var duration = Medias.getDuration(media);
        return new VideoOrGifMessageBuilder()
                .mimetype(requireNonNullElse(mimeType, VIDEO.mimeType()))
                .thumbnail(thumbnail != null ? thumbnail : Medias.getVideoThumbnail(media).orElse(null))
                .caption(caption)
                .width(dimensions.width())
                .height(dimensions.height())
                .duration(duration)
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .build()
                .setDecodedMedia(media);
    }

    @ProtobufBuilder(className = "GifMessageSimpleBuilder")
    static VideoOrGifMessage gifBuilder(byte[] media, String mimeType, String caption, Attribution gifAttribution, byte[] thumbnail, ContextInfo contextInfo) {
        if (!isNotGif(media, mimeType)) {
            throw new IllegalArgumentException("Cannot create a VideoMessage with mime type image/gif: gif messages on whatsapp are videos played as gifs");
        }
        var dimensions = Medias.getDimensions(media, true);
        var duration = Medias.getDuration(media);
        return new VideoOrGifMessageBuilder()
                .mimetype(requireNonNullElse(mimeType, VIDEO.mimeType()))
                .thumbnail(thumbnail != null ? thumbnail : Medias.getVideoThumbnail(media).orElse(null))
                .caption(caption)
                .width(dimensions.width())
                .height(dimensions.height())
                .duration(duration)
                .gifPlayback(true)
                .gifAttribution(requireNonNullElse(gifAttribution, Attribution.NONE))
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .build()
                .setDecodedMedia(media);
    }

    private static boolean isNotGif(byte[] media, String mimeType) {
        return Medias.getMimeType(media)
                .filter("image/gif"::equals)
                .isEmpty() && (!Objects.equals(mimeType, "image/gif"));
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public VideoOrGifMessage setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public VideoOrGifMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public VideoOrGifMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public VideoOrGifMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public VideoOrGifMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public VideoOrGifMessage setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public OptionalLong mediaKeyTimestampSeconds() {
        return Clock.parseTimestamp(mediaKeyTimestampSeconds);
    }

    @Override
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Clock.parseSeconds(mediaKeyTimestampSeconds);
    }

    @Override
    public VideoOrGifMessage setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    public OptionalInt height() {
        return height == null ? OptionalInt.empty() : OptionalInt.of(height);
    }

    public OptionalInt width() {
        return width == null ? OptionalInt.empty() : OptionalInt.of(width);
    }

    public boolean gifPlayback() {
        return gifPlayback;
    }

    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.VIDEO;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.VIDEO;
    }

    @Override
    public HydratedFourRowTemplateTitle.Type hydratedTitleType() {
        return HydratedFourRowTemplateTitle.Type.VIDEO;
    }

    @Override
    public InteractiveHeaderAttachment.Type interactiveHeaderType() {
        return InteractiveHeaderAttachment.Type.VIDEO;
    }

    @Override
    public ButtonsMessageHeader.Type buttonHeaderType() {
        return ButtonsMessageHeader.Type.VIDEO;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    public OptionalInt duration() {
        return duration == null ? OptionalInt.empty() : OptionalInt.of(duration);
    }

    public List<InteractiveLocationAnnotation> interactiveAnnotations() {
        return Collections.unmodifiableList(interactiveAnnotations);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<byte[]> streamingSidecar() {
        return Optional.ofNullable(streamingSidecar);
    }

    public Optional<Attribution> gifAttribution() {
        return Optional.ofNullable(gifAttribution);
    }

    public boolean viewOnce() {
        return viewOnce;
    }

    public Optional<String> thumbnailDirectPath() {
        return Optional.ofNullable(thumbnailDirectPath);
    }

    public Optional<byte[]> thumbnailSha256() {
        return Optional.ofNullable(thumbnailSha256);
    }

    public Optional<byte[]> thumbnailEncSha256() {
        return Optional.ofNullable(thumbnailEncSha256);
    }

    public Optional<String> staticUrl() {
        return Optional.ofNullable(staticUrl);
    }

    @Override
    public VideoOrGifMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    /**
     * The constants of this enumerated type describe the various sources from where a gif can come
     * from
     */
    @ProtobufEnum(name = "Message.VideoMessage.Attribution")
    public enum Attribution {
        /**
         * No source was specified
         */
        NONE(0),
        /**
         * Giphy
         */
        GIPHY(1),
        /**
         * Tenor
         */
        TENOR(2);

        final int index;

        Attribution(int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}