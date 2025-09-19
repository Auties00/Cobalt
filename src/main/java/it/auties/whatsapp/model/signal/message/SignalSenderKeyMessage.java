package it.auties.whatsapp.model.signal.message;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.model.signal.key.SignalPrivateKey;
import it.auties.whatsapp.model.signal.key.SignalPublicKey;

import java.util.Arrays;

import static it.auties.whatsapp.model.signal.SignalProtocol.CURRENT_VERSION;

@ProtobufMessage(name = "SenderKeyMessage")
public final class SignalSenderKeyMessage {
    private static final Integer SIGNATURE_LENGTH = 64;

    private Integer version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] cipherText;

    private byte[] signature;

    SignalSenderKeyMessage(Integer id, Integer iteration, byte[] cipherText) {
        // Don't set the version, it will be set by ofSerialized
        this.id = id;
        this.iteration = iteration;
        this.cipherText = cipherText;
    }

    public SignalSenderKeyMessage(Integer version, Integer id, Integer iteration, byte[] cipherText, SignalPrivateKey signaturePrivateKey) {
        this.version = version;
        this.id = id;
        this.iteration = iteration;
        this.cipherText = cipherText;
        var messageLength = SenderKeyMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength];
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SenderKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        this.signature = Curve25519.sign(signaturePrivateKey.encodedPoint(), serialized, null);
    }

    public static SignalSenderKeyMessage ofSerialized(byte[] serialized) {
        var signature = Arrays.copyOfRange(serialized, serialized.length - SIGNATURE_LENGTH, serialized.length);
        var result = SenderKeyMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1 - SIGNATURE_LENGTH));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        result.signature = signature;
        return result;
    }

    public byte[] serialized() {
        var messageLength = SenderKeyMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength + SIGNATURE_LENGTH];
        if(version == null) {
            throw new InternalError();
        }
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SenderKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        if(signature == null || signature.length != SIGNATURE_LENGTH) {
            throw new InternalError();
        }
        System.arraycopy(signature, 0, serialized, 1 + messageLength, signature.length);
        return serialized;
    }

    public boolean verifySignature(SignalPublicKey key) {
        if(signature == null || signature.length != SIGNATURE_LENGTH) {
            throw new InternalError();
        }
        var messageLength = SenderKeyMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength + SIGNATURE_LENGTH];
        if(version == null) {
            throw new InternalError();
        }
        serialized[0] = (byte) (version << 4 | CURRENT_VERSION);
        SenderKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        return Curve25519.verifySignature(key.encodedPoint(), serialized, signature);
    }

    public Integer version() {
        if(version == null) {
            throw new InternalError();
        }

        return version;
    }

    public Integer id() {
        return id;
    }

    public Integer iteration() {
        return iteration;
    }

    public byte[] cipherText() {
        return cipherText;
    }
}
