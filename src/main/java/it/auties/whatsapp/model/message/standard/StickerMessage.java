package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.NoSuchElementException;
import java.util.Objects;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;
import static it.auties.whatsapp.model.message.model.MediaMessageType.STICKER;
import static it.auties.whatsapp.util.Medias.Format.PNG;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a message holding a sticker inside
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newRawStickerMessage", buildMethodName = "create")
@Jacksonized
@Accessors(fluent = true)
public final class StickerMessage extends MediaMessage {
    /**
     * The upload url of the encoded sticker that this object wraps
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String url;

    /**
     * The sha256 of the decoded sticker that this object wraps
     */
    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] fileSha256;

    /**
     * The sha256 of the encoded sticker that this object wraps
     */
    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] fileEncSha256;

    /**
     * The media key of the sticker that this object wraps
     */
    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] key;

    /**
     * The mime type of the sticker that this object wraps.
     * Most of the endTimeStamp this is {@link MediaMessageType#defaultMimeType()}
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String mimetype;

    /**
     * The unsigned height of the decoded sticker that this object wraps
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private Integer height;

    /**
     * The unsigned width of the decoded sticker that this object wraps
     */
    @ProtobufProperty(index = 7, type = UINT32)
    private Integer width;

    /**
     * The direct path to the encoded sticker that this object wraps
     */
    @ProtobufProperty(index = 8, type = STRING)
    private String directPath;

    /**
     * The unsigned size of the decoded sticker that this object wraps
     */
    @ProtobufProperty(index = 9, type = UINT64)
    private long fileLength;

    /**
     * The timestamp, that is the endTimeStamp elapsed since {@link java.time.Instant#EPOCH}, for {@link StickerMessage#key()}
     */
    @ProtobufProperty(index = 10, type = UINT64)
    private long mediaKeyTimestamp;

    /**
     * The length of the first frame
     */
    @ProtobufProperty(index = 11, type = UINT32)
    private Integer firstFrameLength;

    /**
     * The sidecar for the first frame
     */
    @ProtobufProperty(index = 12, type = BYTES)
    private byte[] firstFrameSidecar;

    /**
     * Determines whether this sticker message is animated
     */
    @ProtobufProperty(index = 13, type = BOOLEAN)
    private boolean animated;

    /**
     * The thumbnail for this sticker message encoded as png in an array of bytes
     */
    @ProtobufProperty(index = 16, type = BYTES)
    private byte[] thumbnail;

    /**
     * Constructs a new builder to create a StickerMessage.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
     *
     * @param storeId     the id of the store where this message will be stored
     * @param media       the non-null sticker that the new message wraps
     * @param mimeType    the mime type of the new message, by default {@link MediaMessageType#defaultMimeType()}
     * @param thumbnail   the thumbnail of the sticker that the new message wraps as a png
     * @param animated    whether the sticker that the new message wraps is animated
     * @param contextInfo the context info that the new message wraps
     * @return a non-null new message
     */
    @Builder(builderClassName = "SimpleStickerMessageBuilder", builderMethodName = "newStickerMessage", buildMethodName = "create")
    private static StickerMessage builder(int storeId, byte @NonNull [] media, String mimeType, byte[] thumbnail,
                                          boolean animated, ContextInfo contextInfo) {
        var store = Store.findStoreById(storeId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Cannot create sticker message, invalid store id: %s".formatted(storeId)));
        var upload = Medias.upload(media, STICKER, store);
        return StickerMessage.newRawStickerMessage()
                .storeId(storeId)
                .fileSha256(upload.fileSha256())
                .fileEncSha256(upload.fileEncSha256())
                .key(upload.mediaKey())
                .mediaKeyTimestamp(Clock.now())
                .url(upload.url())
                .directPath(upload.directPath())
                .fileLength(upload.fileLength())
                .mimetype(requireNonNullElse(mimeType, STICKER.defaultMimeType()))
                .thumbnail(thumbnail != null ?
                        thumbnail :
                        Medias.getThumbnail(media, PNG))
                .animated(animated)
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::new))
                .create();
    }

    /**
     * Returns the media type of the sticker that this object wraps
     *
     * @return {@link MediaMessageType#STICKER}
     */
    @Override
    public MediaMessageType type() {
        return MediaMessageType.STICKER;
    }
}
