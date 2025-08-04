package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import java.util.Arrays;
import java.util.function.Function;

import static it.auties.whatsapp.util.SignalConstants.CURRENT_VERSION;
import static it.auties.whatsapp.util.SignalConstants.SIGNATURE_LENGTH;

@ProtobufMessage(name = "SenderKeyMessage")
public final class SenderKeyMessage {
    private int version;

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    final Integer id;

    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    final Integer iteration;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    final byte[] cipherText;

    private byte[] signature;

    public SenderKeyMessage(int version, Integer id, Integer iteration, byte[] cipherText, Function<byte[], byte[]> signatureFunction) {
        this.version = version;
        this.id = id;
        this.iteration = iteration;
        this.cipherText = cipherText;
        this.signature = computeSignature(signatureFunction);
    }

    private byte[] computeSignature(Function<byte[], byte[]> signatureFunction) {
        var messageLength = SenderKeyMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength];
        serialized[0] = serializedVersion();
        SenderKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        return signatureFunction.apply(serialized);
    }

    SenderKeyMessage(int id, int iteration, byte[] cipherText) {
        this.id = id;
        this.iteration = iteration;
        this.cipherText = cipherText;
    }

    public static SenderKeyMessage ofSerialized(byte[] serialized) {
        var signature = Arrays.copyOfRange(serialized, serialized.length - SIGNATURE_LENGTH, serialized.length);
        var result = SenderKeyMessageSpec.decode(ProtobufInputStream.fromBytes(serialized, 1, serialized.length - 1 - SIGNATURE_LENGTH));
        result.version = Byte.toUnsignedInt(serialized[0]) >> 4;
        result.signature = signature;
        return result;
    }

    public byte[] serialized() {
        var messageLength = SenderKeyMessageSpec.sizeOf(this);
        var serialized = new byte[1 + messageLength + SIGNATURE_LENGTH];
        serialized[0] = serializedVersion();
        SenderKeyMessageSpec.encode(this, ProtobufOutputStream.toBytes(serialized, 1));
        if(signature == null || signature.length != SIGNATURE_LENGTH) {
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
