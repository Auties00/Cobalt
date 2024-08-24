package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * A model class that represents additional metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Currently empty
 */
@ProtobufMessage(name = "Message.PollUpdateMessageMetadata")
public record PollUpdateMessageMetadata(

) {

}
