package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model that represents a chat disappear mode
 */
public record ChatDisappear(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        ChatDisappearType disappear
) implements ProtobufMessage {
}