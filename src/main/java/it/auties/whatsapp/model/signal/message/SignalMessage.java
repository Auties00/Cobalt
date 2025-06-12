package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.crypto.Hmac;

import java.util.Arrays;

import static it.auties.whatsapp.util.SignalConstants.*;

@ProtobufMessage(name = "SignalMessage")
public final class SignalMessage {
    private int version;

    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final byte[] ephemeralPublicKey;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer counter;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final Integer previousCounter;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] ciphertext;

    private byte[] signature;

    public static SignalMessage ofSigned(int version, byte[] ephemeralPublicKey, Integer counter, Integer previousCounter, byte[] ciphertext, byte[] identityPublicKey, byte[] signalRemoteIdentityKey, byte[] hmacKey) {
        if(identityPublicKey == null || identityPublicKey.length != KEY_LENGTH) {
            throw new IllegalArgumentException("Invalid identityPublicKey");
        }

        if(signalRemoteIdentityKey == null || signalRemoteIdentityKey.length != KEY_LENGTH + 1 || signalRemoteIdentityKey[0] != KEY_TYPE) {
            throw new IllegalArgumentException("Invalid signalRemoteIdentityKey");
        }

        var message = new SignalMessage(ephemeralPublicKey, counter, previousCounter, ciphertext);
        message.version = version;
        var messageLength = SignalMessageSpec.sizeOf(message);
        var macInput = new byte[1 + identityPublicKey.length + signalRemoteIdentityKey.length + 1 + messageLength];
        macInput[0] = KEY_TYPE;
        System.arraycopy(identityPublicKey, 0, macInput, 1, identityPublicKey.length);
        System.arraycopy(signalRemoteIdentityKey, 0, macInput, 1 + identityPublicKey.length, signalRemoteIdentityKey.length);
        macInput[1 + identityPublicKey.length + signalRemoteIdentityKey.length] = message.serializedVersion();
        SignalMessageSpec.encode(message, ProtobufOutputStream.toBytes(macInput, 1 + identityPublicKey.length + signalRemoteIdentityKey.length + 1));
        var signature = Hmac.calculateSha256(macInput, hmacKey);
        message.signature =  Arrays.copyOf(signature, MAC_LENGTH);
        return message;
    }

    SignalMessage(byte[] ephemeralPublicKey, Integer counter, Integer previousCounter, byte[] ciphertext) {
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        var signature = Arrays.copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length);
        var result = SignalMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1 - MAC_LENGTH));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        result.signature = signature;
        return result;
    }

    public byte[] serialized() {
        var messageLength = SignalMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength + MAC_LENGTH];
        serialized[0] = serializedVersion();
        SignalMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        if(signature == null || signature.length != MAC_LENGTH) {
            throw new InternalError();
        }

        System.arraycopy(signature, 0, serialized, 1 + messageLength, signature.length);
        return serialized;
    }

    public int version() {
        if(version == 0) {
            throw new InternalError();
        }

        return version;
    }

    public byte serializedVersion() {
        if(version == 0) {
            throw new InternalError();
        }

        return (byte) (version << 4 | CURRENT_VERSION);
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
}
