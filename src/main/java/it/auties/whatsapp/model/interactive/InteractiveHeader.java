package it.auties.whatsapp.model.interactive;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents the header of a product
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("Header")
public class InteractiveHeader implements ProtobufMessage {
    /**
     * The title of this header
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The subtitle of this header
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String subtitle;

    /**
     * Whether this message had a media attachment
     */
    @ProtobufProperty(index = 5, type = BOOL)
    private boolean mediaAttachment;

    /**
     * The document attachment
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = DocumentMessage.class)
    private DocumentMessage attachmentDocument;

    /**
     * The image attachment
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage attachmentImage;

    /**
     * The jpeg attachment
     */
    @ProtobufProperty(index = 6, type = BYTES)
    private byte[] attachmentThumbnail;

    /**
     * The video attachment
     */
    @ProtobufProperty(index = 7, type = MESSAGE, implementation = VideoMessage.class)
    private VideoMessage attachmentVideo;

    /**
     * Constructs a new builder to create a Product header
     *
     * @param title      the title of this header
     * @param subtitle   the subtitle of this header
     * @param attachment the attachment of this header
     * @return a non-null new header
     */
    @Builder(builderClassName = "ProductHeaderSimpleBuilder", builderMethodName = "simpleBuilder")
    private static InteractiveHeader customBuilder(String title, String subtitle, InteractiveHeaderAttachment attachment) {
        var builder = InteractiveHeader.builder()
                .title(title)
                .subtitle(subtitle);
        switch (attachment){
            case DocumentMessage documentMessage -> builder.attachmentDocument(documentMessage)
                    .mediaAttachment(true);
            case ImageMessage imageMessage -> builder.attachmentImage(imageMessage)
                    .mediaAttachment(true);
            case InteractiveHeaderThumbnail productHeaderThumbnail -> builder.attachmentThumbnail(productHeaderThumbnail.thumbnail())
                    .mediaAttachment(true);
            case VideoMessage videoMessage -> builder.attachmentVideo(videoMessage)
                    .mediaAttachment(true);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns the type of attachment of this message
     *
     * @return a non-null attachment type
     */
    public AttachmentType attachmentType() {
        if (attachmentDocument != null) {
            return AttachmentType.DOCUMENT;
        }
        if (attachmentImage != null) {
            return AttachmentType.IMAGE;
        }
        if (attachmentThumbnail != null) {
            return AttachmentType.THUMBNAIL;
        }
        if (attachmentVideo != null) {
            return AttachmentType.VIDEO;
        }
        return AttachmentType.NONE;
    }

    /**
     * Returns the attachment of this message if present
     *
     * @return a non-null attachment type
     */
    public Optional<InteractiveHeaderAttachment> attachment() {
        if (attachmentDocument != null) {
            return Optional.of(attachmentDocument);
        }
        if (attachmentImage != null) {
            return Optional.of(attachmentImage);
        }
        if (attachmentThumbnail != null) {
            return Optional.of(InteractiveHeaderThumbnail.of(attachmentThumbnail));
        }
        if (attachmentVideo != null) {
            return Optional.of(attachmentVideo);
        }
        return Optional.empty();
    }

    /**
     * Returns the document attachment of this message if present
     *
     * @return an optional
     */
    public Optional<DocumentMessage> attachmentDocument() {
        return Optional.ofNullable(attachmentDocument);
    }

    /**
     * Returns the image attachment of this message if present
     *
     * @return an optional
     */
    public Optional<ImageMessage> attachmentImage() {
        return Optional.ofNullable(attachmentImage);
    }

    /**
     * Returns the thumbnail attachment of this message if present
     *
     * @return an optional
     */
    public Optional<byte[]> attachmentThumbnail() {
        return Optional.ofNullable(attachmentThumbnail);
    }

    /**
     * Returns the video attachment of this message if present
     *
     * @return an optional
     */
    public Optional<VideoMessage> attachmentVideo() {
        return Optional.ofNullable(attachmentVideo);
    }

    /**
     * The constants of this enumerated type describe the various types of attachment that a product
     * header can have
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum AttachmentType {
        /**
         * No attachment
         */
        NONE(0),
        /**
         * Document message
         */
        DOCUMENT(3),
        /**
         * Image attachment
         */
        IMAGE(4),
        /**
         * Jpeg attachment
         */
        THUMBNAIL(6),
        /**
         * Video attachment
         */
        VIDEO(7);
        
        @Getter
        private final int index;

        @JsonCreator
        public static AttachmentType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(AttachmentType.NONE);
        }
    }
}