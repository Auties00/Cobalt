package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.Optional;

/**
 * A model class that represents a message that refers to a message sent by the device paired with
 * the active WhatsappWeb session.
 */
@ProtobufMessage(name = "Message.DeviceSentMessage")
public record DeviceSentMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid destinationJid,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        MessageContainer message,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Optional<String> phash
) implements ServerMessage {

    @Override
    public MessageType type() {
        return MessageType.DEVICE_SENT;
    }
}
