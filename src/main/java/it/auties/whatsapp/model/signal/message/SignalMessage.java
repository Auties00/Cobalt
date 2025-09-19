package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static it.auties.whatsapp.model.signal.SignalProtocol.CURRENT_VERSION;

@ProtobufMessage(name = "SignalMessage")
public final class SignalMessage {
    private static final Integer MAC_LENGTH = 8;

    private Integer version;

    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    final SignalPublicKey senderRatchetKey;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer counter;

    @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
    final Integer previousCounter;

    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    final byte[] ciphertext;

    private byte[] mac;

    SignalMessage(SignalPublicKey senderRatchetKey, Integer counter, Integer previousCounter, byte[] ciphertext) {
        // Don't set the version, it will be set by ofSerialized
        this.senderRatchetKey = senderRatchetKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        var mac = Arrays.copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length);
        var result = SignalMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1 - MAC_LENGTH));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        result.mac = mac;
        return result;
    }

    public SignalMessage(Integer version, SignalPublicKey senderRatchetKey, Integer counter, Integer previousCounter, byte[] ciphertext,
                         SignalPublicKey localIdentityKey, SignalPublicKey remoteIdentityKey, byte[] macKey) {
        this.version = version;

        this.senderRatchetKey = senderRatchetKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;

        var messageLength = SignalMessageSpec.sizeOf(this);
        var macInput = new byte[SignalPublicKey.length() + SignalPublicKey.length() + 1 + messageLength];

        var offset = localIdentityKey.writePoint(macInput, 0);
        offset = remoteIdentityKey.writePoint(macInput, SignalPublicKey.length());
        macInput[offset++] = (byte) (version << 4 | CURRENT_VERSION);
        SignalMessageSpec.encode(this, ProtobufOutputStream.toBytes(macInput, offset));

        try {
            var hmacSHA256 = Mac.getInstance("HmacSHA256");
            var keySpec = new SecretKeySpec(macKey, "HmacSHA256");
            hmacSHA256.init(keySpec);
            var mac = hmacSHA256.doFinal(macInput);
            this.mac = Arrays.copyOf(mac, MAC_LENGTH);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }

    public byte[] serialized() {
        var messageLength = SignalMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength + MAC_LENGTH];
        if(version == null) {
            throw new InternalError();
        }
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SignalMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        if(mac == null || mac.length != MAC_LENGTH) {
            throw new InternalError();
        }
        System.arraycopy(mac, 0, serialized, 1 + messageLength, mac.length);
        return serialized;
    }

    public Integer version() {
        if(version == null) {
            throw new InternalError();
        }

        return version;
    }

    public SignalPublicKey senderRatchetKey() {
        return senderRatchetKey;
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
        return mac;
    }
}
