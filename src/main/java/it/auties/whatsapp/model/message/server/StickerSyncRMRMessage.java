package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class StickerSyncRMRMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = STRING, repeated = true)
    private List<String> hash;

    @ProtobufProperty(index = 2, type = STRING)
    private String rmrSource;

    @ProtobufProperty(index = 3, type = INT64)
    private long requestTimestamp;

    @Override
    public MessageType type() {
        return MessageType.STICKER_SYNC;
    }

    public static class StickerSyncRMRMessageBuilder {
        public StickerSyncRMRMessageBuilder hash(List<String> hash) {
            if (this.hash == null)
                this.hash = new ArrayList<>();
            this.hash.addAll(hash);
            return this;
        }
    }
}
