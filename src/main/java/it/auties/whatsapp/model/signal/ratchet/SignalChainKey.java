package it.auties.whatsapp.model.signal.ratchet;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@ProtobufMessage
public final class SignalChainKey {
    private static final byte[] MESSAGE_KEY_SEED = {0x01};
    private static final byte[] CHAIN_KEY_SEED = {0x02};

    @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
    private final int index;

    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    private final byte[] key;

    SignalChainKey(int index, byte[] key) {
        this.index = index;
        this.key = key;
    }

    public int index() {
        return index;
    }

    public byte[] key() {
        return key;
    }

    public SignalChainKey next() {
        byte[] nextKey = getBaseMaterial(CHAIN_KEY_SEED);
        return new SignalChainKey(index + 1, nextKey);
    }

    public SignalMessageKey toMessageKeys() {
        try {
            var inputKeyMaterial = getBaseMaterial(MESSAGE_KEY_SEED);
            var hkdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(new SecretKeySpec(inputKeyMaterial, "AES"))
                    .thenExpand("WhisperMessageKeys".getBytes(StandardCharsets.UTF_8), 80);
            var data = hkdf.deriveData(params);
            var cipherKey = Arrays.copyOfRange(data, 0, 32);
            var macKey = Arrays.copyOfRange(data, 32, 64);
            var iv = Arrays.copyOfRange(data, 64, 80);
            return new SignalMessageKey(index, cipherKey, macKey, iv);
        } catch (GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }

    private byte[] getBaseMaterial(byte[] seed) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(seed);
        } catch (GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }
}