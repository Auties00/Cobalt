package it.auties.whatsapp.model.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
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

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

/**
 * A model class that represents the header of a product
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newRawProductHeaderBuilder")
@Jacksonized
@Accessors(fluent = true)
public class ProductHeader implements ProtobufMessage {
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
    @ProtobufProperty(index = 5, type = BOOLEAN)
    private boolean hasMediaAttachment;

    /**
     * The document attachment
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = DocumentMessage.class)
    private DocumentMessage documentAttachment;

    /**
     * The image attachment
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ImageMessage.class)
    private ImageMessage imageAttachment;

    /**
     * The jpeg attachment
     */
    @ProtobufProperty(index = 6, type = BYTES)
    private byte[] thumbnailAttachment;

    /**
     * The video attachment
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = VideoMessage.class)
    private VideoMessage videoAttachment;

    /**
     * Constructs a new builder to create a header with a document
     *
     * @param title      the title of this header
     * @param subtitle   the subtitle of this header
     * @param attachment the attachment of this header
     * @return a non-null new header
     */
    @Builder(builderClassName = "DocumentProductHeaderBuilder", builderMethodName = "newProductHeaderWithDocumentMessageBuilder")
    private static ProductHeader documentBuilder(String title, String subtitle, DocumentMessage attachment) {
        return ProductHeader.newRawProductHeaderBuilder()
                .title(title)
                .subtitle(subtitle)
                .documentAttachment(attachment)
                .hasMediaAttachment(true)
                .build();
    }

    /**
     * Constructs a new builder to create a header with an image
     *
     * @param title      the title of this header
     * @param subtitle   the subtitle of this header
     * @param attachment the attachment of this header
     * @return a non-null new header
     */
    @Builder(builderClassName = "ImageProductHeaderBuilder", builderMethodName = "newProductHeaderWithImageMessageBuilder")
    private static ProductHeader imageBuilder(String title, String subtitle, ImageMessage attachment) {
        return ProductHeader.newRawProductHeaderBuilder()
                .title(title)
                .subtitle(subtitle)
                .imageAttachment(attachment)
                .hasMediaAttachment(true)
                .build();
    }

    /**
     * Constructs a new builder to create a header with a thumbnail
     *
     * @param title      the title of this header
     * @param subtitle   the subtitle of this header
     * @param attachment the attachment of this header
     * @return a non-null new header
     */
    @Builder(builderClassName = "ThumbnailProductHeaderBuilder", builderMethodName = "newProductHeaderWithThumbnailMessageBuilder")
    private static ProductHeader thumbnailBuilder(String title, String subtitle, byte[] attachment) {
        return ProductHeader.newRawProductHeaderBuilder()
                .title(title)
                .subtitle(subtitle)
                .thumbnailAttachment(attachment)
                .hasMediaAttachment(true)
                .build();
    }

    /**
     * Constructs a new builder to create a header with a video
     *
     * @param title      the title of this header
     * @param subtitle   the subtitle of this header
     * @param attachment the attachment of this header
     * @return a non-null new header
     */
    @Builder(builderClassName = "VideoProductHeaderBuilder", builderMethodName = "newProductHeaderWithVideoMessageBuilder")
    private static ProductHeader videoBuilder(String title, String subtitle, VideoMessage attachment) {
        return ProductHeader.newRawProductHeaderBuilder()
                .title(title)
                .subtitle(subtitle)
                .videoAttachment(attachment)
                .hasMediaAttachment(true)
                .build();
    }

    /**
     * Returns the type of attachment of this message
     *
     * @return a non-null attachment type
     */
    public AttachmentType attachmentType() {
        if (documentAttachment != null)
            return AttachmentType.DOCUMENT_MESSAGE;
        if (imageAttachment != null)
            return AttachmentType.IMAGE_MESSAGE;
        if (thumbnailAttachment != null)
            return AttachmentType.THUMBNAIL;
        if (videoAttachment != null)
            return AttachmentType.VIDEO_MESSAGE;
        return AttachmentType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various types of attachment that a product header can have
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
        DOCUMENT_MESSAGE(3),

        /**
         * Image attachment
         */
        IMAGE_MESSAGE(4),

        /**
         * Jpeg attachment
         */
        THUMBNAIL(6),

        /**
         * Video attachment
         */
        VIDEO_MESSAGE(7);

        @Getter
        private final int index;

        @JsonCreator
        public static AttachmentType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(AttachmentType.NONE);
        }
    }
}
