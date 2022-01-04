package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionChainKey {
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int index;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] key;

    private byte[] cipherKey;
    private byte[] macKey;
    private byte[] iv;

    @JsonCreator
    public SessionChainKey(@JsonProperty("1") int index, @JsonProperty("2") byte[] key) {
        this.index = index;
        this.key = key;
        var mac = Hmac.calculate(MESSAGE_KEY_SEED, key);
        var keyMaterialBytes = Hkdf.deriveSecrets(mac.data(),
                "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        this.cipherKey = keyMaterialBytes[0];
        this.macKey = keyMaterialBytes[1];
        this.iv = copyOfRange(keyMaterialBytes[2], 0, 16);
    }

    @JsonCreator
    public SessionChainKey(@JsonProperty("1") int index, @JsonProperty("2") byte[] cipherKey, @JsonProperty("3") byte[] macKey, @JsonProperty("4") byte[] iv) {
        this.index = index;
        this.cipherKey = cipherKey;
        this.macKey = macKey;
        this.iv = iv;
    }

    public SessionChainKey next() {
        return new SessionChainKey(index + 1,
                Hmac.calculate(CHAIN_KEY_SEED, key).data());
    }
}
