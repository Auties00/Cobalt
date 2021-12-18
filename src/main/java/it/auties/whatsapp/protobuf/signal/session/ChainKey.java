package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.crypto.Hkdf;
import lombok.*;
import lombok.experimental.Accessors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ChainKey {
    private static final String HMAC = "HmacSHA256";

    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int index;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] key;

    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    public ChainKey nextChainKey() {
        var nextKey = getBaseMaterial(CHAIN_KEY_SEED);
        return new ChainKey(index + 1, nextKey);
    }

    public MessageKeys messageKeys() {
        var inputKeyMaterial = getBaseMaterial(MESSAGE_KEY_SEED);
        var keyMaterialBytes = Hkdf.deriveSecrets(inputKeyMaterial, "WhisperMessageKeys".getBytes(), DerivedMessageSecrets.SIZE);
        var keyMaterial = new DerivedMessageSecrets(keyMaterialBytes);
        return new MessageKeys(keyMaterial.cipherKey(), keyMaterial.macKey(), keyMaterial.iv(), index);
    }

    @SneakyThrows
    private byte[] getBaseMaterial(byte[] seed) {
        var mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(seed);
    }
}
