package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;

/**
 * A model class that represents a message that refers to a message sent by the device paired with
 * the active WhatsappWeb session to dataSync.
 */
@ProtobufMessage(name = "Message.DeviceSyncMessage")
public record DeviceSyncMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
        byte[] serializedXmlBytes
) implements ServerMessage {
    @Override
    public MessageType type() {
        return MessageType.DEVICE_SYNC;
    }
}
