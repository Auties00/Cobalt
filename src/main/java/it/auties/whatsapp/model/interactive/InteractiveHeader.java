package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;


/**
 * A model class that represents the header of a product
 */
public record InteractiveHeader(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String title,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> subtitle,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<DocumentMessage> attachmentDocument,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<ImageMessage> attachmentImage,
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        boolean mediaAttachment,
        @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
        Optional<InteractiveHeaderThumbnail> attachmentThumbnail,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<VideoOrGifMessage> attachmentVideo
) implements ProtobufMessage {
    @ProtobufBuilder(className = "InteractiveHeaderSimpleBuilder")
    static InteractiveHeader simpleBuilder(@NonNull String title, @Nullable String subtitle, @Nullable InteractiveHeaderAttachment attachment) {
        var builder = new InteractiveHeaderBuilder()
                .title(title)
                .subtitle(subtitle);
        switch (attachment) {
            case DocumentMessage documentMessage -> builder.attachmentDocument(documentMessage);
            case ImageMessage imageMessage -> builder.attachmentImage(imageMessage);
            case InteractiveHeaderThumbnail productHeaderThumbnail -> builder.attachmentThumbnail(productHeaderThumbnail);
            case VideoOrGifMessage videoMessage -> builder.attachmentVideo(videoMessage);
            case null -> {}
        }
        builder.mediaAttachment(attachment != null);
        return builder.build();
    }

    /**
     * Returns the type of attachment of this message
     *
     * @return a non-null attachment type
     */
    public InteractiveHeaderAttachmentType attachmentType() {
        return attachment()
                .map(InteractiveHeaderAttachment::interactiveHeaderType)
                .orElse(InteractiveHeaderAttachmentType.NONE);
    }

    /**
     * Returns the attachment of this message if present
     *
     * @return a non-null attachment type
     */
    public Optional<? extends InteractiveHeaderAttachment> attachment() {
        if (attachmentDocument.isPresent()) {
            return attachmentDocument;
        }

        if (attachmentImage.isPresent()) {
            return attachmentImage;
        }

        if (attachmentThumbnail.isPresent()) {
            return attachmentThumbnail;
        }

        return attachmentVideo;
    }
}