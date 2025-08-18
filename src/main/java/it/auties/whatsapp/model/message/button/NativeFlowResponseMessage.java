package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.MessageType;

@ProtobufMessage(name = "Message.InteractiveResponseMessage.NativeFlowResponseMessage")
public record NativeFlowResponseMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String paramsJson,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements ButtonMessage {
    @Override
    public MessageType type() {
        return MessageType.NATIVE_FLOW_RESPONSE;
    }
}
