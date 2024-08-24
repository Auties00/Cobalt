package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;


/**
 * A model class that represents the cypher data to decode a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@ProtobufMessage(name = "PollEncValue")
public record PollUpdateEncryptedMetadata(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] payload,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] iv
) {

}
