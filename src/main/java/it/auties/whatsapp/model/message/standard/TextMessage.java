package it.auties.whatsapp.model.message.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.button.HydratedFourRowTemplateTitleType;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message holding text inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("ExtendedTextMessage")
public final class TextMessage extends ContextualMessage implements ButtonsMessageHeader, HydratedFourRowTemplateTitle {
    /**
     * The text that this message wraps
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String text;

    /**
     * The substring of this text message that links to {@link TextMessage#canonicalUrl}, if
     * available
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String matchedText;

    /**
     * The canonical url of the link that this text message wraps, if available
     */
    @ProtobufProperty(index = 4, type = STRING)
    private String canonicalUrl;

    /**
     * The description of the link that this text message wraps, if available
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String description;

    /**
     * The title of the link that this text message wraps, if available
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String title;

    /**
     * The color of this text message encoded as ARGB
     */
    @ProtobufProperty(index = 7, type = FIXED32)
    private Integer textArgb;

    /**
     * The background color of this text message encoded as ARGB
     */
    @ProtobufProperty(index = 8, type = FIXED32)
    private Integer backgroundArgb;

    /**
     * The type of font used for the text message.
     */
    @ProtobufProperty(index = 9, type = MESSAGE, implementation = TextMessage.TextMessageFontType.class)
    private TextMessageFontType font;

    /**
     * The type of preview that this text message provides. If said message contains a link, this
     * value will probably be {@link TextMessagePreviewType#VIDEO}. Not all links, though, produce a
     * preview.
     */
    @ProtobufProperty(index = 10, type = MESSAGE, implementation = TextMessage.TextMessagePreviewType.class)
    private TextMessagePreviewType previewType;

    /**
     * The thumbnail for this text message encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    /**
     * Determines whether the preview can be played inline
     */
    @ProtobufProperty(index = 18, type = BOOL)
    private boolean doNotPlayInline;

    @ProtobufProperty(index = 19, name = "thumbnailDirectPath", type = STRING)
    private String thumbnailDirectPath;

    @ProtobufProperty(index = 20, name = "thumbnailSha256", type = BYTES)
    private byte[] thumbnailSha256;

    @ProtobufProperty(index = 21, name = "thumbnailEncSha256", type = BYTES)
    private byte[] thumbnailEncSha256;

    @ProtobufProperty(index = 22, name = "mediaKey", type = BYTES)
    private byte[] mediaKey;

    @ProtobufProperty(index = 23, name = "mediaKeyTimestamp", type = INT64)
    private Long mediaKeyTimestamp;

    @ProtobufProperty(index = 24, name = "thumbnailHeight", type = UINT32)
    private Integer thumbnailHeight;

    @ProtobufProperty(index = 25, name = "thumbnailWidth", type = UINT32)
    private Integer thumbnailWidth;

    @ProtobufProperty(index = 26, name = "inviteLinkGroupType", type = MESSAGE)
    private InviteLinkGroupType inviteLinkGroupType;

    @ProtobufProperty(index = 27, name = "inviteLinkParentGroupSubjectV2", type = STRING)
    private String inviteLinkParentGroupSubjectV2;

    @ProtobufProperty(index = 28, name = "inviteLinkParentGroupThumbnailV2", type = BYTES)
    private byte[] inviteLinkParentGroupThumbnailV2;

    @ProtobufProperty(index = 29, name = "inviteLinkGroupTypeV2", type = MESSAGE)
    private InviteLinkGroupType inviteLinkGroupTypeV2;

    @ProtobufProperty(index = 30, name = "viewOnce", type = BOOL)
    private boolean viewOnce;

    /**
     * Constructs a TextMessage from a text
     *
     * @param text the text to wrap
     */
    public TextMessage(String text) {
        this.text = text;
    }

    /**
     * Constructs a TextMessage from a text
     *
     * @param text the text to wrap
     * @return a non-null TextMessage
     */
    @JsonCreator
    public static TextMessage of(String text) {
        return new TextMessage(text);
    }

    @Override
    public MessageType type() {
        return MessageType.TEXT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    @Override
    public HydratedFourRowTemplateTitleType hydratedTitleType() {
        return HydratedFourRowTemplateTitleType.TEXT;
    }

    /**
     * The constants of this enumerated type describe the various types of fonts that a
     * {@link TextMessage} supports. Not all clients currently display all fonts correctly.
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("FontType")
    public enum TextMessageFontType {
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

        @Getter
        private final int index;

        @JsonCreator
        public static TextMessageFontType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }

    /**
     * The constants of this enumerated type describe the various types of previuew that a
     * {@link TextMessage} can provide.
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("PreviewType")
    public enum TextMessagePreviewType {
        /**
         * No preview
         */
        NONE(0),

        /**
         * Video preview
         */
        VIDEO(1);
        
        @Getter
        private final int index;

        @JsonCreator
        public static TextMessagePreviewType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }

    @AllArgsConstructor
    public enum InviteLinkGroupType implements ProtobufMessage {
        DEFAULT(0),
        PARENT(1),
        SUB(2),
        DEFAULT_SUB(3);

        @Getter
        private final int index;

        @JsonCreator
        public static InviteLinkGroupType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}