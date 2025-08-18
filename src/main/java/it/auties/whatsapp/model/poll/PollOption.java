package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents an option in a
 * {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage(name = "MsgOpaqueData.PollOption")
public record PollOption(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String name
) {

}
