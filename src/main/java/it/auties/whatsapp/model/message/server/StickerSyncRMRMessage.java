package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.List;
import java.util.Objects;

@ProtobufMessage(name = "Message.StickerSyncRMRMessage")
public final class StickerSyncRMRMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> hash;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String rmrSource;

    @ProtobufProperty(index = 3, type = ProtobufType.INT64)
    final long requestTimestamp;

    StickerSyncRMRMessage(List<String> hash, String rmrSource, long requestTimestamp) {
        this.hash = Objects.requireNonNullElse(hash, List.of());
        this.rmrSource = Objects.requireNonNull(rmrSource, "rmrSource cannot be null");
        this.requestTimestamp = requestTimestamp;
    }

    public List<String> hash() {
        return hash;
    }

    public String rmrSource() {
        return rmrSource;
    }

    public long requestTimestamp() {
        return requestTimestamp;
    }

    @Override
    public Type type() {
        return Type.STICKER_SYNC;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StickerSyncRMRMessage that
                && Objects.equals(hash, that.hash)
                && Objects.equals(rmrSource, that.rmrSource)
                && requestTimestamp == that.requestTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, rmrSource, requestTimestamp);
    }

    @Override
    public String toString() {
        return "StickerSyncRMRMessage[" +
                "hash=" + hash + ", " +
                "rmrSource=" + rmrSource + ", " +
                "requestTimestamp=" + requestTimestamp + ']';
    }
}