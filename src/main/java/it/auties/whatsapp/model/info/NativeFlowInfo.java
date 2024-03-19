package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonBody;

/**
 * A model class that holds the information related to a native flow.
 */
@ProtobufMessageName("Message.ButtonsMessage.Button.NativeFlowInfo")
public record NativeFlowInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String parameters
) implements Info, ButtonBody, ProtobufMessage {
    @Override
    public Type bodyType() {
        return Type.NATIVE_FLOW;
    }
}
