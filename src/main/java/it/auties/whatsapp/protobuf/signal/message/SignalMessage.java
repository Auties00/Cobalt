package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.annotation.ProtobufIgnore;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.SignalHelper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class SignalMessage implements SignalProtocolMessage {
    @JsonProperty("0")
    @ProtobufIgnore
    private int version;

    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    private byte @NonNull [] ephemeralPublicKey;

    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int counter;

    @JsonProperty("3")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte @NonNull [] ciphertext;

    @JsonProperty("5")
    @ProtobufIgnore
    private byte[] signature;

    @JsonProperty("6")
    @ProtobufIgnore
    private byte[] serialized;

    public SignalMessage(byte[] ephemeralPublicKey, int counter, int previousCounter, byte[] ciphertext, Function<byte[], byte[]> signer) {
        this.version = CURRENT_VERSION;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.counter = counter;
        this.previousCounter = previousCounter;
        this.ciphertext = ciphertext;
        var encodedMessage = BinaryArray.of(SignalHelper.serialize(version))
                .append(ProtobufEncoder.encode(this));
        this.signature = signer.apply(encodedMessage.data());
        this.serialized = encodedMessage.append(signature)
                .data();
    }
    public static SignalMessage ofSerialized(byte[] serialized) {
        try {
            return ProtobufDecoder.forType(SignalMessage.class)
                    .decode(copyOfRange(serialized, 1, serialized.length - MAC_LENGTH))
                    .version(SignalHelper.deserialize(serialized[0]))
                    .signature(copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length))
                    .serialized(serialized);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode SignalMessage", exception);
        }
    }
}
