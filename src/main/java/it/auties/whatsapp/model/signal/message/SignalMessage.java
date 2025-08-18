package it.auties.whatsapp.model.signal.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.Arrays;
import java.util.Objects;

import static it.auties.whatsapp.util.SignalConstants.MAC_LENGTH;

@ProtobufMessage(name = "SignalMessage")
public final class SignalMessage extends SignalProtocolMessage<SignalMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    private final byte[] ephemeralPublicKey;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    private final Integer counter;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    private final Integer previousCounter;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    private final byte[] ciphertext;

    private byte[] signature;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SignalMessage(byte[] ephemeralPublicKey, Integer counter, Integer previousCounter, byte[] ciphertext, byte[] signature) {
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        this.signature = signature;
    }


    public SignalMessage(byte[] ephemeralPublicKey, Integer counter, Integer previousCounter, byte[] ciphertext) {
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        var data = Arrays.copyOfRange(serialized, 1, serialized.length - MAC_LENGTH);
        var signature = Arrays.copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length);
        return SignalMessageSpec.decode(data)
                .setVersion(Bytes.bytesToVersion(serialized[0]))
                .setSerialized(serialized)
                .setSignature(signature);
    }

    @Override
    public byte[] serialized() {
        if (serialized == null) {
            var encodedMessage = Bytes.concat(serializedVersion(), SignalMessageSpec.encode(this));
            this.serialized = Bytes.concat(encodedMessage, Objects.requireNonNull(signature, "Message wasn't signed"));
        }

        return serialized;
    }

    public byte[] ephemeralPublicKey() {
        return ephemeralPublicKey;
    }

    public Integer counter() {
        return counter;
    }

    public Integer previousCounter() {
        return previousCounter;
    }

    public byte[] ciphertext() {
        return ciphertext;
    }

    public byte[] signature() {
        return signature;
    }

    public SignalMessage setSignature(byte[] signature) {
        this.signature = signature;
        return this;
    }
}
