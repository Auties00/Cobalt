package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;

import java.util.List;


/**
 * A model class that represents a native flow
 * <a href="https://docs.360dialog.com/partner/messaging/interactive-messages/beta-receive-whatsapp-payments-via-stripe">Here</a>> is an explanation on how to use this kind of message
 */
@ProtobufMessage(name = "Message.InteractiveMessage.NativeFlowMessage")
public record InteractiveNativeFlow(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<InteractiveButton> buttons,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String parameters,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {
    @Override
    public Type contentType() {
        return Type.NATIVE_FLOW;
    }
}
