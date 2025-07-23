package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents the header of a product
 */
@ProtobufMessage(name = "Message.InteractiveMessage.Header")
public final class InteractiveHeader {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String subtitle;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final DocumentMessage attachmentDocument;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final ImageMessage attachmentImage;

    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean mediaAttachment;

    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    final InteractiveHeaderThumbnail attachmentThumbnail;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final VideoOrGifMessage attachmentVideo;

    InteractiveHeader(String title, String subtitle, DocumentMessage attachmentDocument,
                      ImageMessage attachmentImage, boolean mediaAttachment,
                      InteractiveHeaderThumbnail attachmentThumbnail, VideoOrGifMessage attachmentVideo) {
        this.title = title;
        this.subtitle = subtitle;
        this.attachmentDocument = attachmentDocument;
        this.attachmentImage = attachmentImage;
        this.mediaAttachment = mediaAttachment;
        this.attachmentThumbnail = attachmentThumbnail;
        this.attachmentVideo = attachmentVideo;
    }

    @ProtobufBuilder(className = "InteractiveHeaderSimpleBuilder")
    static InteractiveHeader simpleBuilder(String title, String subtitle, InteractiveHeaderAttachment attachment) {
        var builder = new InteractiveHeaderBuilder()
                .title(title)
                .subtitle(subtitle);
        switch (attachment) {
            case DocumentMessage documentMessage -> builder.attachmentDocument(documentMessage);
            case ImageMessage imageMessage -> builder.attachmentImage(imageMessage);
            case InteractiveHeaderThumbnail productHeaderThumbnail ->
                    builder.attachmentThumbnail(productHeaderThumbnail);
            case VideoOrGifMessage videoMessage -> builder.attachmentVideo(videoMessage);
            case null -> {}
        }
        builder.mediaAttachment(attachment != null);
        return builder.build();
    }

    public Optional<String>  title() {
        return Optional.ofNullable(title);
    }

    public Optional<String> subtitle() {
        return Optional.ofNullable(subtitle);
    }

    public Optional<DocumentMessage> attachmentDocument() {
        return Optional.ofNullable(attachmentDocument);
    }

    public Optional<ImageMessage> attachmentImage() {
        return Optional.ofNullable(attachmentImage);
    }

    public boolean mediaAttachment() {
        return mediaAttachment;
    }

    public Optional<InteractiveHeaderThumbnail> attachmentThumbnail() {
        return Optional.ofNullable(attachmentThumbnail);
    }

    public Optional<VideoOrGifMessage> attachmentVideo() {
        return Optional.ofNullable(attachmentVideo);
    }

    /**
     * Returns the type of attachment of this message
     *
     * @return a non-null attachment type
     */
    public InteractiveHeaderAttachment.Type attachmentType() {
        return attachment()
                .map(InteractiveHeaderAttachment::interactiveHeaderType)
                .orElse(InteractiveHeaderAttachment.Type.NONE);
    }

    /**
     * Returns the attachment of this message if present
     *
     * @return a non-null attachment type
     */
    public Optional<? extends InteractiveHeaderAttachment> attachment() {
        if (attachmentDocument != null) {
            return Optional.of(attachmentDocument);
        }

        if (attachmentImage != null) {
            return Optional.of(attachmentImage);
        }

        if (attachmentThumbnail != null) {
            return Optional.of(attachmentThumbnail);
        }

        return Optional.ofNullable(attachmentVideo);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveHeader that
                && Objects.equals(title, that.title)
                && Objects.equals(subtitle, that.subtitle)
                && Objects.equals(attachmentDocument, that.attachmentDocument)
                && Objects.equals(attachmentImage, that.attachmentImage)
                && mediaAttachment == that.mediaAttachment
                && Objects.equals(attachmentThumbnail, that.attachmentThumbnail)
                && Objects.equals(attachmentVideo, that.attachmentVideo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, subtitle, attachmentDocument, attachmentImage,
                mediaAttachment, attachmentThumbnail, attachmentVideo);
    }

    @Override
    public String toString() {
        return "InteractiveHeader[" +
                "title=" + title +
                ", subtitle=" + subtitle +
                ", attachmentDocument=" + attachmentDocument +
                ", attachmentImage=" + attachmentImage +
                ", mediaAttachment=" + mediaAttachment +
                ", attachmentThumbnail=" + attachmentThumbnail +
                ", attachmentVideo=" + attachmentVideo + ']';
    }
}