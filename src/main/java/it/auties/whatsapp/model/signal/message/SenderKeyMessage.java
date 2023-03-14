package it.auties.whatsapp.model.signal.message;

import it.auties.bytes.Bytes;
import it.auties.curve25519.Curve25519;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.serialization.performance.Protobuf;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Spec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Spec.Signal.SIGNATURE_LENGTH;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class SenderKeyMessage implements ProtobufMessage {
    private int version;

    @ProtobufProperty(index = 1, type = UINT32)
    private Integer id;

    @ProtobufProperty(index = 2, type = UINT32)
    private Integer iteration;

    @ProtobufProperty(index = 3, type = BYTES)
    private byte @NonNull [] cipherText;

    private byte[] signingKey;

    private byte[] signature;

    private byte[] serialized;

    public SenderKeyMessage(int id, int iteration, byte @NonNull [] cipherText, byte @NonNull [] signingKey) {
        this.version = Spec.Signal.CURRENT_VERSION;
        this.id = id;
        this.iteration = iteration;
        this.cipherText = cipherText;
        this.signingKey = signingKey;
        var serialized = Bytes.of(BytesHelper.versionToBytes(version)).append(Protobuf.writeMessage(this));
        this.signature = Curve25519.sign(signingKey, serialized.toByteArray(), true);
        this.serialized = serialized.append(signature).toByteArray();
    }

    public static SenderKeyMessage ofSerialized(byte[] serialized) {
        var buffer = Bytes.of(serialized);
        return Protobuf.readMessage(buffer.slice(1, -SIGNATURE_LENGTH).toByteArray(), SenderKeyMessage.class)
                .version(BytesHelper.bytesToVersion(serialized[0]))
                .signature(buffer.slice(-SIGNATURE_LENGTH).toByteArray())
                .serialized(serialized);
    }
}
