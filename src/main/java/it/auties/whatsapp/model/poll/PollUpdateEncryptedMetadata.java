package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * A model class that represents the cypher data to decode a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
public record PollUpdateEncryptedMetadata(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte @NonNull [] payload,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte @NonNull [] iv
) implements ProtobufMessage {

}
