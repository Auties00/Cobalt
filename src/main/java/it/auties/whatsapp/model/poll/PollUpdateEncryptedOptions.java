package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * A model class that represents the cypher data to decode the votes of a user inside {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@ProtobufMessage(name = "Message.PollVoteMessage")
public record PollUpdateEncryptedOptions(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        List<byte[]> selectedOptions
) {

}
