package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.crypto.SignalHelper;
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
    private static final int MAC_LENGTH = 8;

    private int version;

    @JsonProperty("1")
    @JsonPropertyDescription("bytes")
    private byte[] ephemeralPublicKey;

    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int counter;

    @JsonProperty("3")
    @JsonPropertyDescription("uint32")
    private int previousCounter;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] ciphertext;

    private byte[] signature;

    private byte[] serialized;

    public static SignalMessage ofSerialized(byte[] serialized) {
        try {
            return ProtobufDecoder.forType(SignalMessage.class)
                    .decode(copyOfRange(serialized, 1, serialized.length - MAC_LENGTH))
                    .version(SignalHelper.deserialize(serialized[0]))
                    .signature(copyOfRange(serialized, serialized.length - MAC_LENGTH, serialized.length))
                    .serialized(copyOfRange(serialized, 0, serialized.length - MAC_LENGTH));
        } catch (IOException exception) {
            throw new RuntimeException("Cannot decode SignalMessage", exception);
        }
    }

    public byte[] serialized() {
        if(serialized == null){
            var encodedObject = encode(this);
            this.serialized = BinaryArray.of(serializedVersion())
                    .append(encodedObject)
                    .data();
        }

        return serialized;
    }
}
