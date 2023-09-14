package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.EncryptedMessage;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

public record EncryptedReactionMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        MessageKey targetMessageKey,
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte @NonNull [] encPayload,
        @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
        byte @NonNull [] encIv
) implements ServerMessage, EncryptedMessage {
    public String secretName() {
        return "Enc Reaction";
    }

    @Override
    public MessageType type() {
        return MessageType.ENCRYPTED_REACTION;
    }
}
