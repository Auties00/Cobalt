package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A model class that represents an option in a
 * {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
public record PollOption(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String name
) implements ProtobufMessage {

}
