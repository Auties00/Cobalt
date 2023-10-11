package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a native flow button
 */
@ProtobufMessageName("Message.InteractiveMessage.NativeFlowMessage.NativeFlowButton")
public record InteractiveButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<String> parameters
) implements ProtobufMessage {
    public InteractiveButton(String name) {
        this(name, Optional.empty());
    }
}
