package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.BytesDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

import static it.auties.whatsapp.binary.BinaryArray.of;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderMessageKey {
    private static final byte[] WHISPER_GROUP = "WhisperGroup".getBytes(StandardCharsets.UTF_8);

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int iteration;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = BytesDeserializer.class)
    private byte[] seed;

    private byte[] cipherKey;

    private byte[] iv;

    public SenderMessageKey(int iteration, byte[] seed) {
        var derivative = of(Hkdf.deriveSecrets(seed, WHISPER_GROUP, 48));
        this.iteration = iteration;
        this.seed = seed;
        this.iv = derivative.slice(16, 32).data();
        this.cipherKey = derivative.slice(32).data();
    }
}
