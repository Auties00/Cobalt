package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.model.ProtobufMessage;

/**
 * A model class that represents additional metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Currently empty
 */
@ProtobufMessageName("Message.PollUpdateMessageMetadata")
public record PollUpdateMessageMetadata(

) implements ProtobufMessage {

}
