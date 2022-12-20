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
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("EncReactionMessage")
public final class EncryptedReactionMessage
        implements ServerMessage {
    private static final String ENC_REACTION = "Enc Reaction";

    @ProtobufProperty(index = 1, name = "targetMessageKey", type = ProtobufType.MESSAGE)
    private MessageKey targetMessageKey;

    @ProtobufProperty(index = 2, name = "encPayload", type = ProtobufType.BYTES)
    private byte[] encPayload;

    @ProtobufProperty(index = 3, name = "encIv", type = ProtobufType.BYTES)
    private byte[] encIv;

    public String secretName() {
        return ENC_REACTION;
    }

    @Override
    public MessageType type() {
        return MessageType.ENCRYPTED_REACTION;
    }
}
