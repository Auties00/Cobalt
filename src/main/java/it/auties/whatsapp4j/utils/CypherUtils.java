package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

@UtilityClass
public class CypherUtils {
    private final Curve25519 CURVE_25519 = Curve25519.getInstance(Curve25519.JAVA);
    private final String HMAC_SHA256 = "HmacSHA256";
    private final String AES = "AES";
    private final String AES_NO_PADDING = "AES/CBC/NoPadding";
    private final int BLOCK_SIZE = 16;

    public Curve25519KeyPair calculateRandomKeyPair(){
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

    public @NotNull BytesArray hmacSha256(@NotNull BytesArray plain) throws GeneralSecurityException {
        return hmacSha256(plain, BytesArray.allocate(32));
    }

    public @NotNull BytesArray hkdfExpand(@NotNull BytesArray input, int size) throws GeneralSecurityException {
        var key = hmacSha256(input);
        var block = BytesArray.allocate(0);
        for(byte index = 1; block.size() < size; index++){
            block = block.merged(hmacSha256(block.merged(index), key));
        }

        return block.cut(size);
    }

    public @NotNull BytesArray aesDecrypt(@NotNull BytesArray encrypted, @NotNull BytesArray secretKey) throws GeneralSecurityException {
        final var iv = encrypted.slice(0, BLOCK_SIZE);
        final var cipher = Cipher.getInstance(AES_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.data(), AES), new IvParameterSpec(iv.data()));
        var encryptedWithoutIv = encrypted.slice(BLOCK_SIZE);
        return unpad(cipher.doFinal(encryptedWithoutIv.data()));
    }

    public @NotNull BytesArray unpad(@NotNull byte[] data){
        var wrap = BytesArray.forArray(data);
        var size = wrap.size() - wrap.lastByte();
        return size <= 0 ? BytesArray.allocate(0) : wrap.cut(size);
    }
}