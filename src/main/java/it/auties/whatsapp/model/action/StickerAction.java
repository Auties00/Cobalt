package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.sync.PatchType;


/**
 * A model clas that represents a sticker
 */
@ProtobufMessage(name = "SyncActionValue.StickerAction")
public record StickerAction(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String url,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] fileEncSha256,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] mediaKey,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        String mimetype,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        int height,
        @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
        int width,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        String directPath,
        @ProtobufProperty(index = 8, type = ProtobufType.UINT64)
        long fileLength,
        @ProtobufProperty(index = 9, type = ProtobufType.BOOL)
        boolean favorite,
        @ProtobufProperty(index = 10, type = ProtobufType.UINT32)
        Integer deviceIdHint
) implements Action {
    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public int actionVersion() {
        throw new UnsupportedOperationException("Cannot send action");
    }

    /**
     * Always throws an exception as this action cannot be serialized
     *
     * @return an exception
     */
    @Override
    public PatchType actionType() {
        throw new UnsupportedOperationException("Cannot send action");
    }
}
