package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.interactive.InteractiveHeaderAttachment;
import it.auties.whatsapp.model.button.interactive.InteractiveLocationAnnotation;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import java.time.ZonedDateTime;
import java.util.*;

import static it.auties.whatsapp.model.message.model.MediaMessageType.IMAGE;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a message holding an image inside
 */
@ProtobufMessage(name = "Message.ImageMessage")
public final class ImageMessage extends MediaMessage<ImageMessage>
        implements InteractiveHeaderAttachment, ButtonsMessageHeader, HighlyStructuredFourRowTemplateTitle, HydratedFourRowTemplateTitle {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String mediaUrl;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String mimetype;
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final String caption;
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private byte[] mediaSha256;
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    private Long mediaSize;
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private final Integer height;
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    private final Integer width;
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    private byte[] mediaKey;
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    private byte[] mediaEncryptedSha256;
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    private final List<InteractiveLocationAnnotation> interactiveAnnotations;
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    private String mediaDirectPath;
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64)
    private Long mediaKeyTimestampSeconds;
    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    private final byte[] firstScanSidecar;
    @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
    private final Integer firstScanLength;
    @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
    private final Integer experimentGroupId;
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    private final byte[] scansSidecar;
    @ProtobufProperty(index = 22, type = ProtobufType.UINT32)
    private final List<Integer> scanLengths;
    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    private final byte[] midQualityFileSha256;
    @ProtobufProperty(index = 24, type = ProtobufType.BYTES)
    private final byte[] midQualityFileEncSha256;
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    private final boolean viewOnce;
    @ProtobufProperty(index = 26, type = ProtobufType.STRING)
    private final String thumbnailDirectPath;
    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    private final byte[] thumbnailSha256;
    @ProtobufProperty(index = 28, type = ProtobufType.BYTES)
    private final byte[] thumbnailEncSha256;
    @ProtobufProperty(index = 29, type = ProtobufType.STRING)
    private final String staticUrl;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ImageMessage(String mediaUrl, String mimetype, String caption, byte[] mediaSha256, Long mediaSize, Integer height, Integer width, byte[] mediaKey, byte[] mediaEncryptedSha256, List<InteractiveLocationAnnotation> interactiveAnnotations, String mediaDirectPath, Long mediaKeyTimestampSeconds, byte[] thumbnail, ContextInfo contextInfo, byte[] firstScanSidecar, Integer firstScanLength, Integer experimentGroupId, byte[] scansSidecar, List<Integer> scanLengths, byte[] midQualityFileSha256, byte[] midQualityFileEncSha256, boolean viewOnce, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, String staticUrl) {
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
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.thumbnail = thumbnail;
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
    }

    /**
     * Constructs a new builder to create a ImageMessage. The newsletters can be later sent using
     * {@link Whatsapp#sendMessage(ChatMessageInfo)}
     *
     * @param media       the non-null image that the new message wraps
     * @param mimeType    the mime type of the new message, by default
     *                    {@link MediaMessageType#mimeType()}
     * @param caption     the caption of the new message
     * @param thumbnail   the thumbnail of the document that the new message wraps
     * @param contextInfo the context info that the new message wraps
     * @return a non-null new message
     */
    @ProtobufBuilder(className = "ImageMessageSimpleBuilder")
    static ImageMessage simpleBuilder(byte[] media, String mimeType, String caption, byte[] thumbnail, ContextInfo contextInfo) {
        var dimensions = Medias.getDimensions(media, false);
        return new ImageMessageBuilder()
                .mimetype(requireNonNullElse(mimeType, IMAGE.mimeType()))
                .caption(caption)
                .width(dimensions.width())
                .height(dimensions.height())
                .thumbnail(thumbnail != null ? thumbnail : Medias.getImageThumbnail(media, true).orElse(null))
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .build()
                .setDecodedMedia(media);
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public ImageMessage setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public ImageMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public ImageMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public ImageMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public ImageMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public ImageMessage setMediaEncryptedSha256(byte[] bytes) {
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
    public ImageMessage setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.IMAGE;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.IMAGE;
    }

    @Override
    public HydratedFourRowTemplateTitle.Type hydratedTitleType() {
        return HydratedFourRowTemplateTitle.Type.IMAGE;
    }

    @Override
    public InteractiveHeaderAttachment.Type interactiveHeaderType() {
        return InteractiveHeaderAttachment.Type.IMAGE;
    }

    @Override
    public ButtonsMessageHeader.Type buttonHeaderType() {
        return ButtonsMessageHeader.Type.IMAGE;
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
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

    public List<InteractiveLocationAnnotation> interactiveAnnotations() {
        return Collections.unmodifiableList(interactiveAnnotations);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public Optional<byte[]> firstScanSidecar() {
        return Optional.ofNullable(firstScanSidecar);
    }

    public OptionalInt firstScanLength() {
        return firstScanLength == null ? OptionalInt.empty() : OptionalInt.of(firstScanLength);
    }

    public OptionalInt experimentGroupId() {
        return experimentGroupId == null ? OptionalInt.empty() : OptionalInt.of(experimentGroupId);
    }

    public Optional<byte[]> scansSidecar() {
        return Optional.ofNullable(scansSidecar);
    }

    public List<Integer> scanLengths() {
        return Collections.unmodifiableList(scanLengths);
    }

    public Optional<byte[]> midQualityFileSha256() {
        return Optional.ofNullable(midQualityFileSha256);
    }

    public Optional<byte[]> midQualityFileEncSha256() {
        return Optional.ofNullable(midQualityFileEncSha256);
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
    public ImageMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }
}