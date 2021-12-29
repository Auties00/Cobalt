package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.util.SignalKeyDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.IOException;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static java.util.Arrays.copyOfRange;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class SignalPreKeyMessage implements SignalProtocolMessage{
    private static int CURRENT_VERSION = 3;

    private int version;

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int preKeyId;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = SignalKeyDeserializer.class)
    private byte[] baseKey;

    @JsonProperty("3")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = SignalKeyDeserializer.class)
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
                    .version(Byte.toUnsignedInt(serialized[0]) >> 4)
                    .serialized(serialized);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode PreKeySignalMessage", exception);
        }
    }

    public SignalPreKeyMessage(int messageVersion, int registrationId, int preKeyId, int signedPreKeyId, byte[] baseKey, byte[] identityKey, SignalMessage message) {
        this.version = messageVersion;
        this.registrationId = registrationId;
        this.preKeyId = preKeyId;
        this.signedPreKeyId = signedPreKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedSignalMessage = encode(message);
        var versionBytes = (byte) (version() << 4 | CURRENT_VERSION);
        var messageBytes = encode(this);
        this.serialized = BinaryArray.of(versionBytes).append(messageBytes).data();
    }

    public SignalMessage signalMessage(){
        return SignalMessage.ofSerialized(serializedSignalMessage);
    }
}
