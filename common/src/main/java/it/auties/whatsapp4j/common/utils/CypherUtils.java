package it.auties.whatsapp4j.common.utils;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.protobuf.message.model.MediaMessage;
import it.auties.whatsapp4j.common.protobuf.message.model.MediaMessageType;
import it.auties.whatsapp4j.common.protobuf.model.media.MediaConnection;
import it.auties.whatsapp4j.common.protobuf.model.media.MediaUpload;
import it.auties.whatsapp4j.common.response.JsonResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.interfaces.EdDSAPrivateKey;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.security.interfaces.XECPrivateKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
    public @NonNull KeyPair randomKeyPair() {
        return KeyPairGenerator.getInstance(CURVE)
                .generateKeyPair();
    }

    @SneakyThrows
    public @NonNull BinaryArray calculateSharedSecret(byte @NonNull [] rawPublicKey, @NonNull PrivateKey privateKey) {
        var keyFactory = KeyFactory.getInstance(CURVE);
        var publicKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_X25519), rawPublicKey);
        var publicKeySpec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());
        var publicKey = keyFactory.generatePublic(publicKeySpec);

        var keyAgreement = KeyAgreement.getInstance(XDH);
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return BinaryArray.forArray(keyAgreement.generateSecret());
    }

    @SneakyThrows
    public byte @NonNull [] raw(@NonNull PublicKey publicKey) {
        var x25519PublicKeyParameters = (X25519PublicKeyParameters) PublicKeyFactory.createKey(publicKey.getEncoded());
        return x25519PublicKeyParameters.getEncoded();
    }

    @SneakyThrows
    public byte @NonNull [] raw(@NonNull PrivateKey privateKey) {
        var xecPrivateKey = (XECPrivateKey) privateKey;
        return xecPrivateKey.getScalar()
                .orElseThrow(() -> new IllegalArgumentException("Cannot serialize a private key with no scalar value"));
    }

    @SneakyThrows
    public @NonNull BinaryArray hmacSha256(@NonNull BinaryArray plain, @NonNull BinaryArray key) {
        var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key.data(), HMAC_SHA256));
        return BinaryArray.forArray(localMac.doFinal(plain.data()));

    }

    @SneakyThrows
    public @NonNull BinaryArray hkdfExtract(@NonNull BinaryArray input) {
        return hkdfExtract(input, null);
    }

    @SneakyThrows
    public @NonNull BinaryArray hkdfExtract(@NonNull BinaryArray input, byte[] key) {
        var hmac = Mac.getInstance(HMAC_SHA256);
        var salt = new SecretKeySpec(Objects.requireNonNullElse(key, new byte[hmac.getMacLength()]), HKDF);
        hmac.init(salt);
        return BinaryArray.forArray(hmac.doFinal(input.data()));
    }

    @SneakyThrows
    public @NonNull BinaryArray hkdfExtractAndExpand(@NonNull BinaryArray input, int size) {
        return hkdfExtractAndExpand(input, null, size);
    }

    @SneakyThrows
    public @NonNull BinaryArray hkdfExtractAndExpand(@NonNull BinaryArray input, byte[] data, int size) {
        return hkdfExpand(hkdfExtract(input), data, size);
    }

    @SneakyThrows
    public @NonNull BinaryArray hkdfExpand(@NonNull BinaryArray input, byte[] data, int size) {
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

        return BinaryArray.forArray(hkdfOutput).cut(size);
    }

    @SneakyThrows
    public @NonNull BinaryArray aesDecrypt(@NonNull BinaryArray encrypted, @NonNull BinaryArray secretKey) {
        return aesDecrypt(encrypted.cut(BLOCK_SIZE), encrypted, secretKey);
    }

    @SneakyThrows
    public @NonNull BinaryArray aesDecrypt(@NonNull BinaryArray iv, @NonNull BinaryArray encrypted, @NonNull BinaryArray secretKey) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(secretKey.data(), AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));
        return BinaryArray.forArray(cipher.doFinal(encrypted.slice(BLOCK_SIZE).data()));
    }

    @SneakyThrows
    public @NonNull BinaryArray aesEncrypt(byte @NonNull [] decrypted, @NonNull BinaryArray encKey) {
        return aesEncrypt(BinaryArray.random(BLOCK_SIZE), decrypted, encKey, true);
    }

    @SneakyThrows
    public @NonNull BinaryArray aesEncrypt(@NonNull BinaryArray iv, byte @NonNull [] decrypted, @NonNull BinaryArray encKey, boolean withIv) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(encKey.data(), AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));

        var result = BinaryArray.forArray(cipher.doFinal(decrypted));
        return withIv ? iv.append(result) : result;
    }

    @SneakyThrows
    public byte @NonNull [] sha256(byte @NonNull [] data) {
        var digest = MessageDigest.getInstance(SHA256);
        return digest.digest(data);
    }

    @SneakyThrows
    public @NonNull BinaryArray sha256(@NonNull BinaryArray data) {
        return BinaryArray.forArray(sha256(data.data()));
    }

    @SneakyThrows
    public byte @NonNull [] mediaDecrypt(@NonNull MediaMessage mediaMessage) {
        return WhatsappUtils.readEncryptedMedia(mediaMessage.url())
                .map(media ->  mediaDecrypt(mediaMessage, media))
                .orElse(new byte[0]);
    }

    @SneakyThrows
    private byte @NonNull [] mediaDecrypt(@NonNull MediaMessage mediaMessage, @NonNull BinaryArray data) {
        var expandedMediaKey = hkdfExtractAndExpand(BinaryArray.forArray(mediaMessage.mediaKey()), mediaMessage.type().key(), 112);
        var iv = expandedMediaKey.slice(0, BLOCK_SIZE);
        var cypherKey = expandedMediaKey.slice(BLOCK_SIZE, 48);
        var macKey = expandedMediaKey.slice(48, 80);

        var file = data.cut(-10);
        var mac = data.slice(-10);

        var hmacValidation = hmacSha256(iv.append(file), macKey).cut(10);
        Validate.isTrue(hmacValidation.equals(mac), "Cannot login: Hmac validation failed!", SecurityException.class);

        return aesDecrypt(iv, file, cypherKey).data();
    }

    @SneakyThrows
    public @NonNull MediaUpload mediaEncrypt(byte @NonNull [] file, @NonNull MediaMessageType type) {
        var mediaKey = BinaryArray.random(32);
        var expandedMediaKey = hkdfExtractAndExpand(mediaKey, type.key(), 112);

        var iv = expandedMediaKey.slice(0, BLOCK_SIZE);
        var cypherKey = expandedMediaKey.slice(BLOCK_SIZE, 48);
        var macKey = expandedMediaKey.slice(48, 80);

        var enc = aesEncrypt(iv, file, cypherKey, false);
        var mac = hmacSha256(iv.append(enc), macKey).cut(10);
        var encFile = enc.append(mac).data();

        var fileSha256 = sha256(file);
        var fileEncSha256 = sha256(encFile);
        var sidecar = mediaSidecar(file, macKey);

        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(fileEncSha256);
        var uri = URI.create("%s/%s?auth=%s&token=%s".formatted(type.url(), token, WhatsappDataManager.singletonInstance().mediaConnection().auth(), token));

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encFile))
                .build();

        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        var body = JsonResponse.fromJson(response.body());
        var encodedUrl = body.getString("url")
                .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot upload media, missing url response %s".formatted(body)));
        var directPath = body.getString("direct_path")
                .orElseThrow(() -> new IllegalArgumentException("WhatsappAPI: Cannot upload media, missing direct path response %s".formatted(body)));

        return new MediaUpload(encodedUrl, directPath, mediaKey, encFile, fileSha256, fileEncSha256, sidecar, type);

    }

    @SneakyThrows
    private byte @NonNull [] mediaSidecar(byte @NonNull [] file, @NonNull BinaryArray macKey) {
        var input = new ByteArrayInputStream(file);
        var output = new ByteArrayOutputStream();
        var chunk = new byte[80];
        while (input.read(chunk) != -1) {
            var sign = hmacSha256(BinaryArray.forArray(chunk), macKey);
            output.write(sign.cut(10).data());
        }

        return output.toByteArray();
    }
}