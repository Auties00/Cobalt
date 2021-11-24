package it.auties.whatsapp.utils;

import it.auties.whatsapp.binary.BinaryArray;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.http.HttpClient;
import java.security.*;
import java.security.interfaces.XECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

/**
 * This utility class provides helper functionality to easily encrypt and decrypt data
 * This class should only be used for WhatsappWeb's WebSocket binary operations
 */
@UtilityClass
public class CypherUtils {
    private final HttpClient CLIENT = HttpClient.newHttpClient();
    private final String XDH = "XDH";
    private final String CURVE = "X25519";
    private final String HMAC_SHA256 = "HmacSHA256";
    private final String AES = "AES";
    private final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private final String SHA256 = "SHA-256";
    private final String HKDF = "HKDF-Salt";
    private final int BLOCK_SIZE = 16;

    @SneakyThrows
    public KeyPair randomKeyPair() {
        return KeyPairGenerator.getInstance(CURVE)
                .generateKeyPair();
    }

    @SneakyThrows
    public BinaryArray calculateSharedSecret(PublicKey publicKey, PrivateKey privateKey) {
        var keyAgreement = KeyAgreement.getInstance(XDH);
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return BinaryArray.of(keyAgreement.generateSecret());
    }

    @SneakyThrows
    public byte[] raw(@NonNull PublicKey publicKey) {
        var x25519PublicKeyParameters = (X25519PublicKeyParameters) PublicKeyFactory.createKey(publicKey.getEncoded());
        return x25519PublicKeyParameters.getEncoded();
    }

    @SneakyThrows
    public byte[] raw(@NonNull PrivateKey privateKey) {
        var xecPrivateKey = (XECPrivateKey) privateKey;
        return xecPrivateKey.getScalar()
                .orElseThrow(() -> new IllegalArgumentException("Cannot serialize a private key with no scalar value"));
    }

    @SneakyThrows
    public PublicKey toX509Encoded(byte @NonNull [] rawPublicKey) {
        var keyFactory = KeyFactory.getInstance(CURVE);
        var publicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), rawPublicKey);
        var publicKeySpec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());
        return keyFactory.generatePublic(publicKeySpec);
    }

    @SneakyThrows
    public PrivateKey toPKCS8Encoded(byte @NonNull [] rawPrivateKey) {
        var keyFactory = KeyFactory.getInstance("X25519");
        var privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), new DEROctetString(rawPrivateKey));
        var privateKey = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        return keyFactory.generatePrivate(privateKey);
    }

    @SneakyThrows
    public BinaryArray hmacSha256(@NonNull BinaryArray plain, @NonNull BinaryArray key) {
        var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key.data(), HMAC_SHA256));
        return BinaryArray.of(localMac.doFinal(plain.data()));

    }

    @SneakyThrows
    public BinaryArray hkdfExtract(@NonNull BinaryArray input) {
        return hkdfExtract(input, null);
    }

    @SneakyThrows
    public BinaryArray hkdfExtract(@NonNull BinaryArray input, byte[] key) {
        var hmac = Mac.getInstance(HMAC_SHA256);
        var salt = new SecretKeySpec(Objects.requireNonNullElse(key, new byte[hmac.getMacLength()]), HKDF);
        hmac.init(salt);
        return BinaryArray.of(hmac.doFinal(input.data()));
    }

    @SneakyThrows
    public BinaryArray hkdfExtractAndExpand(@NonNull BinaryArray input, int size) {
        return hkdfExtractAndExpand(input, null, size);
    }

    @SneakyThrows
    public BinaryArray hkdfExtractAndExpand(@NonNull BinaryArray input, byte[] data, int size) {
        return hkdfExpand(hkdfExtract(input), data, size);
    }

    @SneakyThrows
    public BinaryArray hkdfExpand(@NonNull BinaryArray input, byte[] data, int size) {
        var hmac = Mac.getInstance(HMAC_SHA256);
        var hmacLength = hmac.getMacLength();
        var inputSecret = new SecretKeySpec(input.data(), HMAC_SHA256);

        hmac.init(inputSecret);
        var rounds = (size + hmacLength - 1) / hmacLength;
        var hkdfOutput = new byte[rounds * hmacLength];
        var offset = 0;
        var tLength = 0;

        for (var i = 0; i < rounds ; i++) {
            hmac.update(hkdfOutput, Math.max(0, offset - hmacLength), tLength);
            hmac.update(Objects.requireNonNullElse(data, new byte[0]));
            hmac.update((byte)(i + 1));
            hmac.doFinal(hkdfOutput, offset);

            tLength = hmacLength;
            offset += hmacLength;
        }

        return BinaryArray.of(hkdfOutput).cut(size);
    }

    @SneakyThrows
    public BinaryArray aesDecrypt(@NonNull BinaryArray encrypted, @NonNull BinaryArray secretKey) {
        return aesDecrypt(encrypted.cut(BLOCK_SIZE), encrypted, secretKey);
    }

    @SneakyThrows
    public BinaryArray aesDecrypt(@NonNull BinaryArray iv, @NonNull BinaryArray encrypted, @NonNull BinaryArray secretKey) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(secretKey.data(), AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));
        return BinaryArray.of(cipher.doFinal(encrypted.slice(BLOCK_SIZE).data()));
    }

    @SneakyThrows
    public BinaryArray aesEncrypt(byte @NonNull [] decrypted, @NonNull BinaryArray encKey) {
        return aesEncrypt(BinaryArray.random(BLOCK_SIZE), decrypted, encKey, true);
    }

    @SneakyThrows
    public BinaryArray aesEncrypt(@NonNull BinaryArray iv, byte @NonNull [] decrypted, @NonNull BinaryArray encKey, boolean withIv) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(encKey.data(), AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));

        var result = BinaryArray.of(cipher.doFinal(decrypted));
        return withIv ? iv.append(result) : result;
    }

    @SneakyThrows
    public byte[] sha256(byte @NonNull [] data) {
        var digest = MessageDigest.getInstance(SHA256);
        return digest.digest(data);
    }

    @SneakyThrows
    public BinaryArray sha256(@NonNull BinaryArray data) {
        return BinaryArray.of(sha256(data.data()));
    }
}