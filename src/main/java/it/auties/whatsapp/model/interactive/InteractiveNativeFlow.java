package it.auties.whatsapp.model.interactive;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;
import it.auties.whatsapp.model.message.button.InteractiveMessageContentType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;


/**
 * A model class that represents a native flow
 * <a href="https://docs.360dialog.com/partner/messaging/interactive-messages/beta-receive-whatsapp-payments-via-stripe">Here</a>> is an explanation on how to use this kind of message
 */
public record InteractiveNativeFlow(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT, repeated = true)
        List<InteractiveButton> buttons,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String parameters,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {
    @Override
    public InteractiveMessageContentType contentType() {
        return InteractiveMessageContentType.NATIVE_FLOW;
    }
}
