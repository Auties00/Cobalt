package it.auties.whatsapp.protobuf.signal.session;

import it.auties.whatsapp.binary.BinaryArray;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public record DerivedMessageSecrets(SecretKeySpec cipherKey, SecretKeySpec macKey, IvParameterSpec iv) {
    public static final int SIZE = 80;
    private static final int CIPHER_KEY_LENGTH = 32;
    private static final int MAC_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 16;
    private static final String AES = "AES";
    private static final String HMAC = "HmacSHA256";

    public DerivedMessageSecrets(BinaryArray data) {
        this(new SecretKeySpec(data.cut(CIPHER_KEY_LENGTH).data(), AES),
                new SecretKeySpec(data.slice(CIPHER_KEY_LENGTH, CIPHER_KEY_LENGTH + MAC_KEY_LENGTH).data(), HMAC),
                new IvParameterSpec(data.slice(CIPHER_KEY_LENGTH + MAC_KEY_LENGTH, CIPHER_KEY_LENGTH + MAC_KEY_LENGTH + IV_LENGTH).data()));
    }

    public DerivedMessageSecrets(byte[] data){
        this(BinaryArray.of(data));
    }
}
