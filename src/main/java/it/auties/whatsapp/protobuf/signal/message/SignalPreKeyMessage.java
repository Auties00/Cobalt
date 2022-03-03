package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.bytes.Bytes;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.crypto.SignalHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.Objects;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class SignalPreKeyMessage implements SignalProtocolMessage{
    private int version;

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int preKeyId;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] baseKey;

    @JsonProperty("3")
    @JsonPropertyDescription("bytes")
    private byte[] identityKey;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] serializedSignalMessage;

    @JsonProperty("5")
    @JsonPropertyDescription("uint32")
    private int registrationId;

    @JsonProperty("6")
    @JsonPropertyDescription("uint32")
    private int signedPreKeyId;

    private byte[] serialized;

    public static SignalPreKeyMessage ofSerialized(byte[] serialized) {
        try {
            return ProtobufDecoder.forType(SignalPreKeyMessage.class)
                    .decode(copyOfRange(serialized, 1, serialized.length))
                    .version(SignalHelper.deserialize(serialized[0]))
                    .serialized(serialized);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode PreKeySignalMessage", exception);
        }
    }

    public SignalMessage signalMessage(){
        return SignalMessage.ofSerialized(serializedSignalMessage);
    }

    public byte[] serialized() {
        return Objects.requireNonNullElseGet(serialized,
                () -> this.serialized = serialize());
    }

    private byte[] serialize() {
        return Bytes.of(serializedVersion())
                .append(encode(this))
                .toByteArray();
    }
}
