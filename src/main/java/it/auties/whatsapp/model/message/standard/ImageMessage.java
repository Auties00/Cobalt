package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.InteractiveAnnotation;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;
import static it.auties.whatsapp.model.message.model.MediaMessageType.IMAGE;
import static it.auties.whatsapp.util.Medias.Format.JPG;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a message holding an image inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawImageMessageBuilder")
@Jacksonized
@Accessors(fluent = true)
public final class ImageMessage extends MediaMessage {
    /**
     * The upload url of the encoded image that this object wraps
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String mediaUrl;

    /**
     * The mime type of the image that this object wraps.
     * Most of the seconds this is {@link MediaMessageType#defaultMimeType()}
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String mimetype;

    /**
     * The caption of this message
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String caption;

    /**
     * The sha256 of the decoded image that this object wraps
     */
    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaSha256;

    /**
     * The unsigned size of the decoded image that this object wraps
     */
    @ProtobufProperty(index = 5, type = UINT64)
    private long mediaSize;

    /**
     * The unsigned height of the decoded image that this object wraps
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private Integer height;

    /**
     * The unsigned width of the decoded image that this object wraps
     */
    @ProtobufProperty(index = 7, type = UINT32)
    private Integer width;

    /**
     * The media key of the image that this object wraps
     */
    @ProtobufProperty(index = 8, type = BYTES)
    private byte[] mediaKey;

    /**
     * The sha256 of the encoded image that this object wraps
     */
    @ProtobufProperty(index = 9, type = BYTES)
    private byte[] mediaEncryptedSha256;

    /**
     * Interactive annotations
     */
    @ProtobufProperty(index = 10, type = MESSAGE, concreteType = InteractiveAnnotation.class, repeated = true)
    private List<InteractiveAnnotation> interactiveAnnotations;

    /**
     * The direct path to the encoded image that this object wraps
     */
    @ProtobufProperty(index = 11, type = STRING)
    private String mediaDirectPath;

    /**
     * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link ImageMessage#mediaKey()}
     */
    @ProtobufProperty(index = 12, type = UINT64)
    private long mediaKeyTimestamp;

    /**
     * The thumbnail for this image message encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    /**
     * The sidecar for the first sidecar
     */
    @ProtobufProperty(index = 18, type = BYTES)
    private byte[] firstScanSidecar;

    /**
     * The length of the first scan
     */
    @ProtobufProperty(index = 19, type = UINT32)
    private Integer firstScanLength;

    /**
     * Experiment Group Id
     */
    @ProtobufProperty(index = 20, type = UINT32)
    private Integer experimentGroupId;

    /**
     * The sidecar for the scans of the decoded image
     */
    @ProtobufProperty(index = 21, type = BYTES)
    private byte[] scansSidecar;

    /**
     * The length of each scan of the decoded image
     */
    @ProtobufProperty(index = 22, type = UINT32, repeated = true)
    private List<Integer> scanLengths;

    /**
     * The sha256 of the decoded image in medium quality
     */
    @ProtobufProperty(index = 23, type = BYTES)
    private byte[] midQualityFileSha256;

    /**
     * The sha256 of the encoded image in medium quality
     */
    @ProtobufProperty(index = 24, type = BYTES)
    private byte[] midQualityFileEncSha256;

    /**
     * Constructs a new builder to create a ImageMessage.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
     *
     * @param mediaConnection the media connection to use to upload this message
     * @param media           the non-null image that the new message wraps
     * @param mimeType        the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
     * @param caption         the caption of the new message
     * @param thumbnail       the thumbnail of the document that the new message wraps
     * @param contextInfo     the context info that the new message wraps
     * @return a non-null new message
     */
    @Builder(builderClassName = "SimpleImageBuilder", builderMethodName = "newImageMessageBuilder")
    private static ImageMessage builder(@NonNull MediaConnection mediaConnection, byte @NonNull [] media,
                                        String mimeType, String caption, byte[] thumbnail, ContextInfo contextInfo) {
        var dimensions = Medias.getDimensions(media, false);
        var upload = Medias.upload(media, IMAGE, mediaConnection);
        return ImageMessage.newRawImageMessageBuilder()
                .mediaSha256(upload.fileSha256())
                .mediaEncryptedSha256(upload.fileEncSha256())
                .mediaKey(upload.mediaKey())
                .mediaKeyTimestamp(Clock.now())
                .mediaUrl(upload.url())
                .mediaDirectPath(upload.directPath())
                .mediaSize(upload.fileLength())
                .mimetype(requireNonNullElse(mimeType, IMAGE.defaultMimeType()))
                .caption(caption)
                .width(dimensions.width())
                .height(dimensions.height())
                .thumbnail(thumbnail != null ?
                        thumbnail :
                        Medias.getThumbnail(media, JPG).orElse(null))
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Returns the media type of the image that this object wraps
     *
     * @return {@link MediaMessageType#IMAGE}
     */
    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.IMAGE;
    }

    public static abstract class ImageMessageBuilder<C extends ImageMessage, B extends ImageMessageBuilder<C, B>>
            extends MediaMessageBuilder<C, B> {
        public B interactiveAnnotations(List<InteractiveAnnotation> interactiveAnnotations) {
            if (this.interactiveAnnotations == null)
                this.interactiveAnnotations = new ArrayList<>();
            this.interactiveAnnotations.addAll(interactiveAnnotations);
            return self();
        }

        public B scanLengths(List<Integer> scanLengths) {
            if (this.scanLengths == null)
                this.scanLengths = new ArrayList<>();
            this.scanLengths.addAll(scanLengths);
            return self();
        }
    }
}
