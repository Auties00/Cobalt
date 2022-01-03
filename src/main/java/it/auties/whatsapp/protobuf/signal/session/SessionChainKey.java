package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.util.BytesDeserializer;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.experimental.Accessors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SessionChainKey {
    private static final String HMAC = "HmacSHA256";
    private static final String WHISPER = "WhisperMessageKeys";
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int index;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    @JsonDeserialize(using = BytesDeserializer.class)
    private byte[] key;

    private byte[] cipherKey;
    private byte[] macKey;
    private byte[] iv;

    @JsonCreator
    public SessionChainKey(@JsonProperty("1") int index, @JsonProperty("2") byte[] key) {
        this.index = index;
        this.key = key;
        var inputKeyMaterial = getBaseMaterial(MESSAGE_KEY_SEED);
        var keyMaterialBytes = Hkdf.deriveSecrets(inputKeyMaterial, WHISPER.getBytes(StandardCharsets.UTF_8), 80);
        this.cipherKey = Arrays.copyOfRange(keyMaterialBytes, 0, 32);
        this.macKey = Arrays.copyOfRange(keyMaterialBytes, 32, 64);
        this.iv = Arrays.copyOfRange(keyMaterialBytes, 64, 80);
    }

    @JsonCreator
    public SessionChainKey(@JsonProperty("1") int index, @JsonProperty("2") byte[] cipherKey, @JsonProperty("3") byte[] macKey, @JsonProperty("4") byte[] iv) {
        this.index = index;
        this.cipherKey = cipherKey;
        this.macKey = macKey;
        this.iv = iv;
    }

    public SessionChainKey next() {
        return new SessionChainKey(index + 1, getBaseMaterial(CHAIN_KEY_SEED));
    }

    @SneakyThrows
    private byte[] getBaseMaterial(byte[] seed) {
        Validate.isTrue(key != null, "This operation is not supported", UnsupportedOperationException.class);
        var mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(seed);
    }
}
