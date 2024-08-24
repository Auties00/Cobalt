package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;

/**
 * A model class that represents a selected option in a {@link it.auties.whatsapp.model.message.standard.PollCreationMessage}
 */
@ProtobufMessage
public record SelectedPollOption(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String name
) {

}
