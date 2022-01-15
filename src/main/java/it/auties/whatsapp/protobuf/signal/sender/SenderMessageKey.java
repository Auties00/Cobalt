package it.auties.whatsapp.protobuf.signal.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.crypto.Hkdf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

import static java.util.Arrays.copyOfRange;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderMessageKey {
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int iteration;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] seed;

    private byte[] cipherKey;

    private byte[] iv;

    public SenderMessageKey(int iteration, byte[] seed) {
        var derivative = Hkdf.deriveSecrets(seed,
                "WhisperGroup".getBytes(StandardCharsets.UTF_8));
        this.iteration = iteration;
        this.seed = seed;
        this.iv = copyOfRange(derivative[0], 0, 16);
        var cipherKey = new byte[32];
        System.arraycopy(derivative[0], 16, cipherKey, 0, 16);
        System.arraycopy(derivative[1], 0, cipherKey, 16, 16);
        this.cipherKey = cipherKey;
    }
}
