package it.auties.whatsapp.model.poll;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Arrays;
import java.util.Objects;

/**
 * A model class that represents the cypher data to decode a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@ProtobufMessage(name = "PollEncValue")
public final class PollUpdateEncryptedMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] payload;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    final byte[] iv;

    PollUpdateEncryptedMetadata(byte[] payload, byte[] iv) {
        this.payload = Objects.requireNonNull(payload, "payload cannot be null");
        this.iv = Objects.requireNonNull(iv, "iv cannot be null");
    }

    public byte[] payload() {
        return payload;
    }

    public byte[] iv() {
        return iv;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PollUpdateEncryptedMetadata that
                && Arrays.equals(payload, that.payload)
                && Arrays.equals(iv, that.iv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(payload), Arrays.hashCode(iv));
    }

    @Override
    public String toString() {
        return "PollUpdateEncryptedMetadata[" +
                "payload=" + Arrays.toString(payload) +
                ", iv=" + Arrays.toString(iv) +
                ']';
    }
}