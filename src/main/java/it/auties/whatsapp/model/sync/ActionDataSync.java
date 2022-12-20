package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("SyncActionData")
public class ActionDataSync
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] index;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = ActionValueSync.class)
    private ActionValueSync value;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte[] padding;

    @ProtobufProperty(index = 4, type = INT32)
    private Integer version;

    public MessageIndexInfo messageIndex() {
        var jsonIndex = new String(index, StandardCharsets.UTF_8);
        return MessageIndexInfo.ofJson(jsonIndex);
    }
}