package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.protocol.SignalProtos;
import org.whispersystems.libsignal.util.ByteUtil;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.Arrays;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class PreKeySignalMessage implements SignalProtocolMessage{
    private static int CURRENT_VERSION = 3;

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
    private byte[] serializedMessage;

    @JsonProperty("5")
    @JsonPropertyDescription("uint32")
    private int registrationId;

    @JsonProperty("6")
    @JsonPropertyDescription("uint32")
    private int signedPreKeyId;

    private byte[] serialized;

    public PreKeySignalMessage(byte[] serialized) {
        try {
            this.version = Byte.toUnsignedInt(serialized[0]) >> 4;
            var decoded = ProtobufDecoder.forType(getClass())
                    .decode(Arrays.copyOfRange(serialized, 1, serialized.length));
            this.serialized = serialized;
            this.registrationId = decoded.registrationId();
            this.preKeyId = decoded.preKeyId();
            this.signedPreKeyId = decoded.signedPreKeyId() == 0 ? -1 : decoded.signedPreKeyId();
            this.baseKey = Arrays.copyOfRange(decoded.baseKey(), 1, decoded.baseKey().length);
            this.identityKey = Arrays.copyOfRange(decoded.identityKey(), 1, decoded.identityKey().length);
            this.serializedMessage = decoded.serializedMessage();
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode PreKeySignalMessage", exception);
        }
    }

    public PreKeySignalMessage(int messageVersion, int registrationId, int preKeyId, int signedPreKeyId, byte[] baseKey, byte[] identityKey, SignalMessage message) {
        this.version = messageVersion;
        this.registrationId = registrationId;
        this.preKeyId = preKeyId;
        this.signedPreKeyId = signedPreKeyId;
        this.baseKey = baseKey;
        this.identityKey = identityKey;
        this.serializedMessage = ProtobufEncoder.encode(message);
        var versionBytes = (byte) (version() << 4 | CURRENT_VERSION);
        var messageBytes = ProtobufEncoder.encode(this);
        this.serialized = BinaryArray.of(versionBytes).append(messageBytes).data();
    }

    public SignalMessage signalMessage(){
        return new SignalMessage(serializedMessage);
    }
}
