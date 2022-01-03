package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.IOException;

import static it.auties.protobuf.encoder.ProtobufEncoder.encode;
import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class SignalMessage implements SignalProtocolMessage {
    private static final int CURRENT_VERSION = 3;
    private static final int MAC_LENGTH = 8;
    private static final String HMAC = "HmacSHA256";

    private int version;

    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    private byte[] ratchetKey;

    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int counter;

    @JsonProperty("3")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] ciphertext;

    private byte[] serialized;

    public static SignalMessage ofSerialized(byte[] serialized) {
        try {
            return ProtobufDecoder.forType(SignalMessage.class)
                    .decode(copyOfRange(serialized, 1, serialized.length - MAC_LENGTH))
                    .version(Byte.toUnsignedInt(serialized[0]) >> 4)
                    .serialized(serialized);
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode SignalMessage", exception);
        }
    }

    // This does not include the signature(length = 8)
    public byte[] serialized() {
        if(serialized == null){
            var versionBytes = (byte) (version() << 4 | CURRENT_VERSION);
            var messageBytes = encode(this);
            this.serialized = BinaryArray.of(versionBytes)
                    .append(messageBytes)
                    .data();
        }

        return serialized;
    }
}
