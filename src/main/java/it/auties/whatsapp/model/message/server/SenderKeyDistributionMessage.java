package it.auties.whatsapp.model.message.server;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.message.model.ServerMessage;

import java.util.Arrays;
import java.util.Objects;

/**
 * A model class that represents a message sent by WhatsappWeb for security purposes. Whatsapp
 * follows the Signal Standard, for more information about this message visit <a
 * href="https://archive.kaidan.im/libsignal-protocol-c-docs/html/struct___textsecure_____sender_key_distribution_message.html">their
 * documentation</a>
 */
@ProtobufMessage(name = "Message.SenderKeyDistributionMessage")
public final class SenderKeyDistributionMessage implements ServerMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String groupId;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] data;

    SenderKeyDistributionMessage(String groupId, byte[] data) {
        this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null");
        this.data = Objects.requireNonNull(data, "data cannot be null");
    }

    public String groupId() {
        return groupId;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public Type type() {
        return Type.SENDER_KEY_DISTRIBUTION;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SenderKeyDistributionMessage that
                && Objects.equals(groupId, that.groupId)
                && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, Arrays.hashCode(data));
    }

    @Override
    public String toString() {
        return "SenderKeyDistributionMessage[" +
                "groupId=" + groupId + ", " +
                "data=" + Arrays.toString(data) + ']';
    }
}