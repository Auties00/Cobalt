package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.interactive.InteractiveHeaderAttachment;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.model.message.model.reserved.ExtendedMediaMessage;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.Specification;
import it.auties.whatsapp.util.Validate;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static it.auties.whatsapp.model.message.model.MediaMessageType.DOCUMENT;

/**
 * A model class that represents a message holding a document inside
 */
public final class DocumentMessage extends ExtendedMediaMessage<DocumentMessage>
        implements MediaMessage<DocumentMessage>, InteractiveHeaderAttachment, ButtonsMessageHeader, HighlyStructuredFourRowTemplateTitle, HydratedFourRowTemplateTitle {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String mediaUrl;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String mimetype;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private final String title;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private byte[] mediaSha256;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    private Long mediaSize;

    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private final Integer pageCount;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    private byte[] mediaKey;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    private final String fileName;

    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    private byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    private String mediaDirectPath;

    @ProtobufProperty(index = 11, type = ProtobufType.UINT64)
    private Long mediaKeyTimestampSeconds;

    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private final byte[] thumbnail;

    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    private final boolean contactVcard;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    private final String thumbnailDirectPath;

    @ProtobufProperty(index = 14, type = ProtobufType.BYTES)
    private final byte[] thumbnailSha256;

    @ProtobufProperty(index = 15, type = ProtobufType.BYTES)
    private final byte[] thumbnailEncSha256;

    @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
    private ContextInfo contextInfo;

    @ProtobufProperty(index = 18, type = ProtobufType.UINT32)
    private final Integer thumbnailHeight;

    @ProtobufProperty(index = 19, type = ProtobufType.UINT32)
    private final Integer thumbnailWidth;

    @ProtobufProperty(index = 20, type = ProtobufType.STRING)
    private final String caption;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DocumentMessage(String mediaUrl, String mimetype, String title, byte[] mediaSha256, Long mediaSize, Integer pageCount, byte[] mediaKey, String fileName, byte[] mediaEncryptedSha256, String mediaDirectPath, Long mediaKeyTimestampSeconds, byte[] thumbnail, boolean contactVcard, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, ContextInfo contextInfo, Integer thumbnailHeight, Integer thumbnailWidth, String caption) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.title = title;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.pageCount = pageCount;
        this.mediaKey = mediaKey;
        this.fileName = fileName;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.thumbnail = thumbnail;
        this.contactVcard = contactVcard;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.contextInfo = contextInfo;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.caption = caption;
    }

    @ProtobufBuilder(className = "DocumentMessageSimpleBuilder")
    static DocumentMessage customBuilder(byte[] media, String fileName, String mimeType, String title, int pageCount, byte[] thumbnail, ContextInfo contextInfo) {
        var extensionIndex = fileName.lastIndexOf(".");
        Validate.isTrue(extensionIndex != -1 && extensionIndex + 1 < fileName.length(), "Expected fileName to be formatted as name.extension");
        var extension = fileName.substring(extensionIndex + 1);
        return new DocumentMessageBuilder()
                .mimetype(getMimeType(media, fileName, mimeType))
                .fileName(fileName)
                .pageCount(pageCount > 0 ? pageCount : Medias.getPagesCount(media).orElse(1))
                .title(title)
                .thumbnail(thumbnail != null ? null : Medias.getDocumentThumbnail(media).orElse(null))
                .thumbnailWidth(Specification.Whatsapp.THUMBNAIL_WIDTH)
                .thumbnailHeight(Specification.Whatsapp.THUMBNAIL_HEIGHT)
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .build()
                .setDecodedMedia(media);
    }

    private static String getMimeType(byte[] media, String fileName, String mimeType) {
        return Optional.ofNullable(mimeType)
                .or(() -> Medias.getMimeType(fileName))
                .or(() -> Medias.getMimeType(media))
                .orElse(DOCUMENT.mimeType());
    }

    public OptionalInt pageCount() {
        return pageCount == null ? OptionalInt.empty() : OptionalInt.of(pageCount);
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Optional<String> fileName() {
        return Optional.ofNullable(fileName);
    }

    public boolean contactVcard() {
        return contactVcard;
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public DocumentMessage setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public DocumentMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public DocumentMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public DocumentMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public DocumentMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public DocumentMessage setMediaEncryptedSha256(byte[] bytes) {
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
    public DocumentMessage setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.DOCUMENT;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.DOCUMENT;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.DOCUMENT;
    }

    @Override
    public ButtonsMessageHeader.Type buttonHeaderType() {
        return ButtonsMessageHeader.Type.DOCUMENT;
    }

    @Override
    public HydratedFourRowTemplateTitle.Type hydratedTitleType() {
        return HydratedFourRowTemplateTitle.Type.DOCUMENT;
    }

    @Override
    public InteractiveHeaderAttachment.Type interactiveHeaderType() {
        return InteractiveHeaderAttachment.Type.DOCUMENT;
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
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

    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    @Override
    public DocumentMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }
}