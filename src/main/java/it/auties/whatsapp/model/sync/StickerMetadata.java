package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.media.AttachmentProvider;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents the metadata for a sticker
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("StickerMetadata")
public final class StickerMetadata implements ProtobufMessage, AttachmentProvider {
    /**
     * The url of the sticker
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String mediaUrl;

    /**
     * The sha256 of the media
     */
    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] mediaSha256;

    /**
     * The sha256 of the encrypted media
     */
    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] mediaEncryptedSha256;

    /**
     * The media key of the sticker
     */
    @ProtobufProperty(index = 4, type = BYTES)
    private byte[] mediaKey;

    /**
     * The mime type of the sticker
     */
    @ProtobufProperty(index = 5, type = STRING)
    private String mimetype;

    /**
     * The height of the sticker
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private int height;

    /**
     * The width of the sticker
     */
    @ProtobufProperty(index = 7, type = UINT32)
    private int width;

    /**
     * The direct path to the sticker
     */
    @ProtobufProperty(index = 8, type = STRING)
    private String mediaDirectPath;

    /**
     * The size of the sticker file
     */
    @ProtobufProperty(index = 9, type = UINT64)
    private long mediaSize;

    /**
     * The weight of the sticker
     */
    @ProtobufProperty(index = 10, type = FLOAT)
    private float weight;

    /**
     * The timestamp in seconds when the sticker was last sent
     */
    @ProtobufProperty(index = 11, type = INT64)
    private long lastStickerSentSeconds;

    /**
     * Returns when the sticker was last sent
     *
     * @return an optional
     */
    public ZonedDateTime lastStickerSent() {
        return Clock.parseSeconds(lastStickerSentSeconds);
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.IMAGE;
    }
}