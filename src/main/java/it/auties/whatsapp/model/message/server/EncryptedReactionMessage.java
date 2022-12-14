package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.model.ServerMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("EncReactionMessage")
public final class EncryptedReactionMessage implements ServerMessage {
    @ProtobufProperty(index = 1, name = "targetMessageKey", type = ProtobufType.MESSAGE)
    private MessageKey targetMessageKey;

    @ProtobufProperty(index = 2, name = "encPayload", type = ProtobufType.BYTES)
    private byte[] encPayload;

    @ProtobufProperty(index = 3, name = "encIv", type = ProtobufType.BYTES)
    private byte[] encIv;

    @Override
    public MessageType type() {
        return MessageType.ENCRYPTED_REACTION;
    }
}
