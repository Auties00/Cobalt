package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.EncryptedMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;

@ProtobufMessage(name = "Message.EncReactionMessage")
public record EncryptedReactionMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageKey targetMessageKey,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] encPayload,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte[] encIv
) implements ServerMessage, EncryptedMessage {
    public String secretName() {
        return "Enc Reaction";
    }

    @Override
    public MessageType type() {
        return MessageType.ENCRYPTED_REACTION;
    }
}
