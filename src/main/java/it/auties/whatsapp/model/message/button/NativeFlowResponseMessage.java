package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.INT32;
import static it.auties.protobuf.base.ProtobufType.STRING;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("NativeFlowResponseMessage")
public final class NativeFlowResponseMessage implements ButtonMessage {
    @ProtobufProperty(index = 1, name = "name", type = STRING)
    private String name;

    @ProtobufProperty(index = 2, name = "paramsJson", type = STRING)
    private String paramsJson;

    @ProtobufProperty(index = 3, name = "version", type = INT32)
    private int version;

    @Override
    public MessageType type() {
        return MessageType.NATIVE_FLOW_RESPONSE;
    }
}
