package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.media.MediaConnection;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.*;
import static it.auties.whatsapp.model.message.model.MediaMessageType.DOCUMENT;
import static it.auties.whatsapp.util.Medias.Format.FILE;

/**
 * A model class that represents a message holding a document inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
public final class DocumentMessage extends MediaMessage {
    /**
     * The upload url of the encoded document that this object wraps
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String mediaUrl;

    /**
     * The mime type of the audio that this object wraps.
     * Most of the seconds this is {@link MediaMessageType#defaultMimeType()}
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String mimetype;

    /**
     * The title of the document that this object wraps
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String title;

    /**
     * The sha256 of the decoded media that this object wraps
     */
    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaSha256;

    /**
     * The unsigned size of the decoded media that this object wraps
     */
    @ProtobufProperty(index = 5, type = UINT64)
    private long mediaSize;

    /**
     * The unsigned length in pages of the document that this object wraps
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private Integer pageCount;

    /**
     * The media key of the document that this object wraps.
     */
    @ProtobufProperty(index = 7, type = BYTES)
    private byte[] mediaKey;

    /**
     * The name of the document that this object wraps
     */
    @ProtobufProperty(index = 8, type = STRING)
    private String fileName;

    /**
     * The sha256 of the encoded media that this object wraps
     */
    @ProtobufProperty(index = 9, type = BYTES)
    private byte[] mediaEncryptedSha256;

    /**
     * The direct path to the encoded media that this object wraps
     */
    @ProtobufProperty(index = 10, type = STRING)
    private String mediaDirectPath;

    /**
     * The timestamp, that is the seconds elapsed since {@link java.time.Instant#EPOCH}, for {@link DocumentMessage#mediaKey()}
     */
    @ProtobufProperty(index = 11, type = UINT64)
    private long mediaKeyTimestamp;

    /**
     * The thumbnail for this document encoded as jpeg in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    /**
     * Constructs a new builder to create a DocumentMessage.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
     *
     * @param mediaConnection the media connection to use to upload this message
     * @param media           the non-null document that the new message wraps
     * @param mimeType        the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
     * @param title           the title of the document that the new message wraps
     * @param pageCount       the number of pages of the document that the new message wraps
     * @param fileName        the name of the document that the new message wraps
     * @param thumbnail       the thumbnail of the document that the new message wraps
     * @param contextInfo     the context info that the new message wraps
     * @return a non-null new message
     */
    @Builder(builderClassName = "SimpleDocumentMessageBuilder", builderMethodName = "simpleBuilder")
    private static DocumentMessage customBuilder(@NonNull MediaConnection mediaConnection, byte @NonNull [] media,
                                           String mimeType, String title, int pageCount, String fileName,
                                           byte[] thumbnail, ContextInfo contextInfo) {
        var upload = Medias.upload(media, DOCUMENT, mediaConnection);
        return DocumentMessage.builder()
                .mediaSha256(upload.fileSha256())
                .mediaEncryptedSha256(upload.fileEncSha256())
                .mediaKey(upload.mediaKey())
                .mediaKeyTimestamp(Clock.now())
                .mediaUrl(upload.url())
                .mediaDirectPath(upload.directPath())
                .mediaSize(upload.fileLength())
                .mimetype(Optional.ofNullable(mimeType)
                        .or(() -> Medias.getMimeType(fileName))
                        .or(() -> Medias.getMimeType(media))
                        .orElse(DOCUMENT.defaultMimeType()))
                .fileName(fileName)
                .pageCount(pageCount)
                .title(title)
                .thumbnail(thumbnail != null ?
                        thumbnail :
                        Medias.getThumbnail(media, FILE).orElse(null))
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Returns the media type of the document that this object wraps
     *
     * @return {@link MediaMessageType#DOCUMENT}
     */
    @Override
    public MediaMessageType mediaType() {
        return MediaMessageType.DOCUMENT;
    }
}
