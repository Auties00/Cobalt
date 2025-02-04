package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.util.Clock;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;


/**
 * A model class that represents a message holding text inside
 */
@ProtobufMessage(name = "Message.TextMessage")
public final class TextMessage implements ContextualMessage<TextMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private String text;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private String matchedText;

    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private String canonicalUrl;

    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    private String description;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private String title;

    @ProtobufProperty(index = 7, type = ProtobufType.FIXED32)
    private Integer textArgb;

    @ProtobufProperty(index = 8, type = ProtobufType.FIXED32)
    private Integer backgroundArgb;

    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    private FontType font;

    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    private PreviewType previewType;

    @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
    private byte[] thumbnail;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    private boolean doNotPlayInline;

    @ProtobufProperty(index = 19, type = ProtobufType.STRING)
    private String thumbnailDirectPath;

    @ProtobufProperty(index = 20, type = ProtobufType.BYTES)
    private byte[] thumbnailSha256;

    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    private byte[] thumbnailEncSha256;

    @ProtobufProperty(index = 22, type = ProtobufType.BYTES)
    private byte[] mediaKey;

    @ProtobufProperty(index = 23, type = ProtobufType.INT64)
    private Long mediaKeyTimestampSeconds;

    @ProtobufProperty(index = 24, type = ProtobufType.UINT32)
    private Integer thumbnailHeight;

    @ProtobufProperty(index = 25, type = ProtobufType.UINT32)
    private Integer thumbnailWidth;

    @ProtobufProperty(index = 26, type = ProtobufType.ENUM)
    private InviteLinkGroupType inviteLinkGroupType;

    @ProtobufProperty(index = 27, type = ProtobufType.STRING)
    private String inviteLinkParentGroupSubjectV2;

    @ProtobufProperty(index = 28, type = ProtobufType.BYTES)
    private byte[] inviteLinkParentGroupThumbnailV2;

    @ProtobufProperty(index = 29, type = ProtobufType.ENUM)
    private InviteLinkGroupType inviteLinkGroupTypeV2;

    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    private boolean viewOnce;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TextMessage(String text, String matchedText, String canonicalUrl, String description, String title, Integer textArgb, Integer backgroundArgb, FontType font, PreviewType previewType, byte[] thumbnail, ContextInfo contextInfo, boolean doNotPlayInline, String thumbnailDirectPath, byte[] thumbnailSha256, byte[] thumbnailEncSha256, byte[] mediaKey, Long mediaKeyTimestampSeconds, Integer thumbnailHeight, Integer thumbnailWidth, InviteLinkGroupType inviteLinkGroupType, String inviteLinkParentGroupSubjectV2, byte[] inviteLinkParentGroupThumbnailV2, InviteLinkGroupType inviteLinkGroupTypeV2, boolean viewOnce) {
        this.text = text;
        this.matchedText = matchedText;
        this.canonicalUrl = canonicalUrl;
        this.description = description;
        this.title = title;
        this.textArgb = textArgb;
        this.backgroundArgb = backgroundArgb;
        this.font = font;
        this.previewType = previewType;
        this.thumbnail = thumbnail;
        this.contextInfo = contextInfo;
        this.doNotPlayInline = doNotPlayInline;
        this.thumbnailDirectPath = thumbnailDirectPath;
        this.thumbnailSha256 = thumbnailSha256;
        this.thumbnailEncSha256 = thumbnailEncSha256;
        this.mediaKey = mediaKey;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailWidth = thumbnailWidth;
        this.inviteLinkGroupType = inviteLinkGroupType;
        this.inviteLinkParentGroupSubjectV2 = inviteLinkParentGroupSubjectV2;
        this.inviteLinkParentGroupThumbnailV2 = inviteLinkParentGroupThumbnailV2;
        this.inviteLinkGroupTypeV2 = inviteLinkGroupTypeV2;
        this.viewOnce = viewOnce;
    }

    public static TextMessage of(String text) {
        return new TextMessageBuilder()
                .text(text)
                .build();
    }

    @Override
    public MessageType type() {
        return MessageType.TEXT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public String text() {
        return text;
    }

    public Optional<String> matchedText() {
        return Optional.ofNullable(matchedText);
    }

    public Optional<String> canonicalUrl() {
        return Optional.ofNullable(canonicalUrl);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Integer textArgb() {
        return textArgb;
    }

    public OptionalInt backgroundArgb() {
        return backgroundArgb == null ? OptionalInt.empty() : OptionalInt.of(backgroundArgb);
    }

    public Optional<FontType> font() {
        return Optional.ofNullable(font);
    }

    public Optional<PreviewType> previewType() {
        return Optional.ofNullable(previewType);
    }

    public Optional<byte[]> thumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    public boolean doNotPlayInline() {
        return doNotPlayInline;
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

    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    public OptionalLong mediaKeyTimestampSeconds() {
        return Clock.parseTimestamp(mediaKeyTimestampSeconds);
    }

    public OptionalInt thumbnailHeight() {
        return thumbnailHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbnailHeight);
    }

    public OptionalInt thumbnailWidth() {
        return thumbnailWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbnailWidth);
    }

    public Optional<InviteLinkGroupType> inviteLinkGroupType() {
        return Optional.ofNullable(inviteLinkGroupType);
    }

    public Optional<String> inviteLinkParentGroupSubjectV2() {
        return Optional.ofNullable(inviteLinkParentGroupSubjectV2);
    }

    public Optional<byte[]> inviteLinkParentGroupThumbnailV2() {
        return Optional.ofNullable(inviteLinkParentGroupThumbnailV2);
    }

    public Optional<InviteLinkGroupType> inviteLinkGroupTypeV2() {
        return Optional.ofNullable(inviteLinkGroupTypeV2);
    }

    public boolean viewOnce() {
        return viewOnce;
    }

    public TextMessage setText(String text) {
        this.text = text;
        return this;
    }

    public TextMessage setMatchedText(String matchedText) {
        this.matchedText = matchedText;
        return this;
    }

    public TextMessage setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
        return this;
    }

    public TextMessage setDescription(String description) {
        this.description = description;
        return this;
    }

    public TextMessage setTitle(String title) {
        this.title = title;
        return this;
    }

    public TextMessage setTextArgb(Integer textArgb) {
        this.textArgb = textArgb;
        return this;
    }

    public TextMessage setBackgroundArgb(Integer backgroundArgb) {
        this.backgroundArgb = backgroundArgb;
        return this;
    }

    public TextMessage setFont(FontType font) {
        this.font = font;
        return this;
    }

    public TextMessage setPreviewType(PreviewType previewType) {
        this.previewType = previewType;
        return this;
    }

    public TextMessage setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
        return this;
    }

    public TextMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    public TextMessage setDoNotPlayInline(boolean doNotPlayInline) {
        this.doNotPlayInline = doNotPlayInline;
        return this;
    }

    public TextMessage setThumbnailDirectPath(String thumbnailDirectPath) {
        this.thumbnailDirectPath = thumbnailDirectPath;
        return this;
    }

    public TextMessage setThumbnailSha256(byte[] thumbnailSha256) {
        this.thumbnailSha256 = thumbnailSha256;
        return this;
    }

    public TextMessage setThumbnailEncSha256(byte[] thumbnailEncSha256) {
        this.thumbnailEncSha256 = thumbnailEncSha256;
        return this;
    }

    public TextMessage setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
        return this;
    }

    public TextMessage setMediaKeyTimestampSeconds(Long mediaKeyTimestampSeconds) {
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        return this;
    }

    public TextMessage setThumbnailHeight(Integer thumbnailHeight) {
        this.thumbnailHeight = thumbnailHeight;
        return this;
    }

    public TextMessage setThumbnailWidth(Integer thumbnailWidth) {
        this.thumbnailWidth = thumbnailWidth;
        return this;
    }

    public TextMessage setInviteLinkGroupType(InviteLinkGroupType inviteLinkGroupType) {
        this.inviteLinkGroupType = inviteLinkGroupType;
        return this;
    }

    public TextMessage setInviteLinkParentGroupSubjectV2(String inviteLinkParentGroupSubjectV2) {
        this.inviteLinkParentGroupSubjectV2 = inviteLinkParentGroupSubjectV2;
        return this;
    }

    public TextMessage setInviteLinkParentGroupThumbnailV2(byte[] inviteLinkParentGroupThumbnailV2) {
        this.inviteLinkParentGroupThumbnailV2 = inviteLinkParentGroupThumbnailV2;
        return this;
    }

    public TextMessage setInviteLinkGroupTypeV2(InviteLinkGroupType inviteLinkGroupTypeV2) {
        this.inviteLinkGroupTypeV2 = inviteLinkGroupTypeV2;
        return this;
    }

    public TextMessage setViewOnce(boolean viewOnce) {
        this.viewOnce = viewOnce;
        return this;
    }

    @ProtobufEnum(name = "Message.TextMessage.InviteLinkGroupType")
    public enum InviteLinkGroupType {
        DEFAULT(0),
        PARENT(1),
        SUB(2),
        DEFAULT_SUB(3);

        final int index;

        InviteLinkGroupType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }

    /**
     * The constants of this enumerated type describe the various types of fonts that a
     * {@link TextMessage} supports. Not all clients currently display all fonts correctly.
     */
    @ProtobufEnum(name = "Message.TextMessage.FontType")
    public enum FontType {
        /**
         * Sans Serif
         */
        SANS_SERIF(0),
        /**
         * Serif
         */
        SERIF(1),
        /**
         * Norican Regular
         */
        NORICAN_REGULAR(2),
        /**
         * Brydan Write
         */
        BRYNDAN_WRITE(3),
        /**
         * Bebasnue Regular
         */
        BEBASNEUE_REGULAR(4),
        /**
         * Oswald Heavy
         */
        OSWALD_HEAVY(5);

        final int index;

        FontType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }

    /**
     * The constants of this enumerated type describe the various types of previuew that a
     * {@link TextMessage} can provide.
     */
    @ProtobufEnum(name = "Message.TextMessage.PreviewType")
    public enum PreviewType {
        /**
         * No preview
         */
        NONE(0),

        /**
         * Video preview
         */
        VIDEO(1);

        final int index;

        PreviewType(int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }
    }
}