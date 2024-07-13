package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents additional metadata about a {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage(name = "PollAdditionalMetadata")
public record PollAdditionalMetadata(
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        boolean pollInvalidated
) {

}