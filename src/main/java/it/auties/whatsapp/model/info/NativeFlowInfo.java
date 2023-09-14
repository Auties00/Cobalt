package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.ButtonBody;
import it.auties.whatsapp.model.button.base.ButtonBodyType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that holds the information related to a native flow.
 */
public record NativeFlowInfo(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String name,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        @NonNull
        String parameters
) implements Info, ButtonBody, ProtobufMessage {
    @Override
    public ButtonBodyType bodyType() {
        return ButtonBodyType.NATIVE_FLOW;
    }
}
