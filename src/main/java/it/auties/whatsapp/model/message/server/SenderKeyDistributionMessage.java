package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;

/**
 * A model class that represents a message sent by WhatsappWeb for security purposes. Whatsapp
 * follows the Signal Standard, for more information about this message visit <a
 * href="https://archive.kaidan.im/libsignal-protocol-c-docs/html/struct___textsecure_____sender_key_distribution_message.html">their
 * documentation</a>
 */
@ProtobufMessage(name = "Message.SenderKeyDistributionMessage")
public record SenderKeyDistributionMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String groupId,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] data
) implements ServerMessage {
    @Override
    public MessageType type() {
        return MessageType.SENDER_KEY_DISTRIBUTION;
    }
}
