package it.auties.whatsapp4j.utils;

import at.favre.lib.crypto.HKDF;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

@UtilityClass
public class CypherUtils {
    private final Curve25519 CURVE_25519 = Curve25519.getInstance(Curve25519.JAVA);
    private final String HMAC_SHA256 = "HmacSHA256";
    private final String AES = "AES";
    private final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private final int BLOCK_SIZE = 16;

    public @NotNull Curve25519KeyPair calculateRandomKeyPair(){
        return CURVE_25519.generateKeyPair();
    }

    public @NotNull BytesArray calculateSharedSecret(byte[] publicKey, byte[] privateKey){
        return BytesArray.forArray(CURVE_25519.calculateAgreement(publicKey, privateKey));
    }

    public @NotNull BytesArray hmacSha256(@NotNull BytesArray plain, @NotNull BytesArray key) throws GeneralSecurityException {
        final var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key.data(), HMAC_SHA256));
        return BytesArray.forArray(localMac.doFinal(plain.data()));
    }

    public @NotNull BytesArray hkdfExpand(@NotNull BytesArray input, int size) {
        return BytesArray.forArray(HKDF.fromHmacSha256().expand(HKDF.fromHmacSha256().extract(null, input.data()), null, size));
    }

    public @NotNull BytesArray aesDecrypt(@NotNull BytesArray encrypted, @NotNull BytesArray secretKey) throws GeneralSecurityException{
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(secretKey.data(), AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(encrypted.cut(BLOCK_SIZE).data()));
        return BytesArray.forArray(cipher.doFinal(encrypted.slice(BLOCK_SIZE).data()));
    }

    public @NotNull BytesArray aesEncrypt(byte[] decrypted, @NotNull BytesArray encKey) throws GeneralSecurityException{
        final var iv = BytesArray.random(BLOCK_SIZE);
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(encKey.data(), AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));
        return iv.merged(BytesArray.forArray(cipher.doFinal(decrypted)));
    }
}