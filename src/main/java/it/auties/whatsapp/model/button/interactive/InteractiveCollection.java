package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.button.InteractiveMessageContent;

/**
 * A model class that represents a business collection
 */
@ProtobufMessage(name = "Message.InteractiveMessage.CollectionMessage")
public record InteractiveCollection(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid business,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String id,
        @ProtobufProperty(index = 3, type = ProtobufType.INT32)
        int version
) implements InteractiveMessageContent {


    @Override
    public Type contentType() {
        return Type.COLLECTION;
    }
}
