package it.auties.whatsapp.model.signal.message;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Protobuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.function.Function;

import static it.auties.protobuf.base.ProtobufType.BYTES;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.whatsapp.util.Spec.Signal.CURRENT_VERSION;
import static it.auties.whatsapp.util.Spec.Signal.MAC_LENGTH;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class SignalMessage implements SignalProtocolMessage {
    private int version;

    @ProtobufProperty(index = 1, type = BYTES)
    private byte @NonNull [] ephemeralPublicKey;

    @ProtobufProperty(index = 2, type = UINT32)
    private Integer counter;

    @ProtobufProperty(index = 3, type = UINT32)
    private Integer previousCounter;

    @ProtobufProperty(index = 4, type = BYTES)
    private byte @NonNull [] ciphertext;

    private byte[] signature;

    private byte[] serialized;

    public SignalMessage(byte @NonNull [] ephemeralPublicKey, int counter, int previousCounter, byte @NonNull [] ciphertext, Function<byte[], byte[]> signer) {
        this.version = CURRENT_VERSION;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        var encodedMessage = BytesHelper.concat(serializedVersion(), Protobuf.writeMessage(this));
        this.signature = signer.apply(encodedMessage);
        this.serialized = BytesHelper.concat(encodedMessage, signature);
    }

    public static SignalMessage ofSerialized(byte[] serialized) {
        var data = Arrays.copyOfRange(serialized, 1, serialized.length - MAC_LENGTH);
        var mac = Arrays.copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length);
        return Protobuf.readMessage(data, SignalMessage.class)
                .version(BytesHelper.bytesToVersion(serialized[0]))
                .signature(mac)
                .serialized(serialized);
    }
}
