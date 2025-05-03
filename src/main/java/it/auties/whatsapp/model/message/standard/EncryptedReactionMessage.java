package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ChatMessageKey;
import it.auties.whatsapp.model.message.model.EncryptedMessage;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.Arrays;
import java.util.Objects;

@ProtobufMessage(name = "Message.EncReactionMessage")
public final class EncryptedReactionMessage implements ServerMessage, EncryptedMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey targetMessageKey;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] encPayload;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] encIv;

    EncryptedReactionMessage(ChatMessageKey targetMessageKey, byte[] encPayload, byte[] encIv) {
        this.targetMessageKey = Objects.requireNonNull(targetMessageKey, "targetMessageKey cannot be null");
        this.encPayload = Objects.requireNonNull(encPayload, "encPayload cannot be null");
        this.encIv = Objects.requireNonNull(encIv, "encIv cannot be null");
    }

    public ChatMessageKey targetMessageKey() {
        return targetMessageKey;
    }

    public byte[] encPayload() {
        return encPayload;
    }

    public byte[] encIv() {
        return encIv;
    }

    public String secretName() {
        return "Enc Reaction";
    }

    @Override
    public Type type() {
        return Type.ENCRYPTED_REACTION;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EncryptedReactionMessage that
                && Objects.equals(targetMessageKey, that.targetMessageKey)
                && Arrays.equals(encPayload, that.encPayload)
                && Arrays.equals(encIv, that.encIv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetMessageKey, Arrays.hashCode(encPayload), Arrays.hashCode(encIv));
    }

    @Override
    public String toString() {
        return "EncryptedReactionMessage[" +
                "targetMessageKey=" + targetMessageKey +
                ", encPayload=" + Arrays.toString(encPayload) +
                ", encIv=" + Arrays.toString(encIv) +
                ']';
    }
}