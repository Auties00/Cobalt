package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * A model class that represents the selection of a row
 */
@ProtobufMessage(name = "Message.ListResponseMessage.SingleSelectReply")
public record SingleSelectReplyButton(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String rowId
) {

}