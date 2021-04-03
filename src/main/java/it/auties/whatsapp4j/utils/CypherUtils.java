package it.auties.whatsapp4j.utils;

import at.favre.lib.crypto.HKDF;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.model.WhatsappMediaConnection;
import it.auties.whatsapp4j.model.WhatsappMediaMessage;
import it.auties.whatsapp4j.model.WhatsappMediaMessageType;
import it.auties.whatsapp4j.model.WhatsappMediaUpload;
import it.auties.whatsapp4j.response.model.JsonResponse;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * This utility class provides helper functionality to easily encrypt and decrypt data
 * This class should only be used for WhatsappWeb's WebSocket binary operations
 */
@UtilityClass
public class CypherUtils {
    private final HttpClient CLIENT = HttpClient.newHttpClient();
    private final Curve25519 CURVE_25519 = Curve25519.getInstance(Curve25519.JAVA);
    private final String HMAC_SHA256 = "HmacSHA256";
    private final String AES = "AES";
    private final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private final String SHA256 = "SHA-256";
    private final int BLOCK_SIZE = 16;

    public @NotNull Curve25519KeyPair calculateRandomKeyPair(){
        return CURVE_25519.generateKeyPair();
    }

    public @NotNull BinaryArray calculateSharedSecret(byte @NotNull [] publicKey, byte @NotNull [] privateKey){
        return BinaryArray.forArray(CURVE_25519.calculateAgreement(publicKey, privateKey));
    }

    @SneakyThrows
    public @NotNull BinaryArray hmacSha256(@NotNull BinaryArray plain, @NotNull BinaryArray key) {
        final var localMac = Mac.getInstance(HMAC_SHA256);
        localMac.init(new SecretKeySpec(key.data(), HMAC_SHA256));
        return BinaryArray.forArray(localMac.doFinal(plain.data()));
    }

    @SneakyThrows
    public @NotNull BinaryArray hkdfExpand(@NotNull BinaryArray input, int size) {
        return hkdfExpand(input, null, size);
    }

    @SneakyThrows
    public @NotNull BinaryArray hkdfExpand(@NotNull BinaryArray input, byte @Nullable [] data, int size) {
        return BinaryArray.forArray(HKDF.fromHmacSha256().extractAndExpand(null, input.data(), data, size));
    }

    @SneakyThrows
    public @NotNull BinaryArray aesDecrypt(@NotNull BinaryArray encrypted, @NotNull BinaryArray secretKey) {
        return aesDecrypt(encrypted.cut(BLOCK_SIZE), encrypted, secretKey);
    }

    @SneakyThrows
    public @NotNull BinaryArray aesDecrypt(@NotNull BinaryArray iv, @NotNull BinaryArray encrypted, @NotNull BinaryArray secretKey) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(secretKey.data(), AES);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));
        return BinaryArray.forArray(cipher.doFinal(encrypted.slice(BLOCK_SIZE).data()));
    }

    @SneakyThrows
    public @NotNull BinaryArray aesEncrypt(byte @NotNull [] decrypted, @NotNull BinaryArray encKey) {
        return aesEncrypt(BinaryArray.random(BLOCK_SIZE), decrypted, encKey, true);
    }

    @SneakyThrows
    public @NotNull BinaryArray aesEncrypt(@NotNull BinaryArray iv, byte @NotNull [] decrypted, @NotNull BinaryArray encKey, boolean withIv) {
        final var cipher = Cipher.getInstance(AES_ALGORITHM);
        final var keySpec = new SecretKeySpec(encKey.data(), AES);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv.data()));

        var result = BinaryArray.forArray(cipher.doFinal(decrypted));
        return withIv ? iv.merged(result) : result;
    }

    @SneakyThrows
    public byte @NotNull [] sha256(byte @NotNull [] data){
        final var digest = MessageDigest.getInstance(SHA256);
        return digest.digest(data);
    }

    @SneakyThrows
    public ByteBuffer mediaDecrypt(@NotNull WhatsappMediaMessage mediaMessage) {
        var message = mediaMessage.info().getMessage();
        var url = WhatsappUtils.readMediaUrl(message);
        var data = WhatsappUtils.readEncryptedMedia(url).orElse(null);
        if(data == null){
            return BinaryArray.empty().toBuffer();
        }

        var mediaKey = WhatsappUtils.readMediaKey(message);
        var expandedMediaKey = hkdfExpand(mediaKey, mediaMessage.type().key(), 112);
        var iv = expandedMediaKey.slice(0, BLOCK_SIZE);
        var cypherKey = expandedMediaKey.slice(BLOCK_SIZE, 48);
        var macKey = expandedMediaKey.slice(48, 80);

        var file = data.cut(-10);
        var mac = data.slice(-10);

        var hmacValidation = hmacSha256(iv.merged(file), macKey).cut(10);
        Validate.isTrue(hmacValidation.equals(mac), "Cannot login: Hmac validation failed!", SecurityException.class);

        return aesDecrypt(iv, file, cypherKey).toBuffer();
    }

    @SneakyThrows
    public @NotNull WhatsappMediaUpload mediaEncrypt(@NotNull WhatsappMediaConnection connection, byte @NotNull [] file, @NotNull WhatsappMediaMessageType type) {
        var mediaKey = BinaryArray.random(32);
        var expandedMediaKey = hkdfExpand(mediaKey, type.key(), 112);

        var iv = expandedMediaKey.slice(0, BLOCK_SIZE);
        var cypherKey = expandedMediaKey.slice(BLOCK_SIZE, 48);
        var macKey = expandedMediaKey.slice(48, 80);

        var enc = aesEncrypt(iv, file, cypherKey, false);
        var mac = hmacSha256(iv.merged(enc), macKey).cut(10);
        var encFile = enc.merged(mac).data();

        var fileSha256 = sha256(file);
        var fileEncSha256 = sha256(encFile);
        var sidecar = type.isStreamable() ? new byte[0] : streamableMediaSidecar(file, macKey);

        var token = Base64.getUrlEncoder().withoutPadding().encodeToString(fileEncSha256);
        var uri = URI.create("%s/%s?auth=%s&token=%s".formatted(type.url(), token, connection.auth(), token));

        var request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(encFile))
                .build();

        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        var body = JsonResponse.fromJson(response.body());
        var encodedUrl = body.getString("url").orElseThrow(() -> new RuntimeException("WhatsappAPI: Cannot upload media, missing url response %s".formatted(body)));
        var directPath = body.getString("direct_path").orElseThrow(() -> new RuntimeException("WhatsappAPI: Cannot upload media, missing direct path response %s".formatted(body)));

        return new WhatsappMediaUpload(encodedUrl, directPath, mediaKey, encFile, fileSha256, fileEncSha256, sidecar, type);
    }

    @SneakyThrows
    public byte @NotNull [] streamableMediaSidecar(byte @NotNull [] file, @NotNull BinaryArray macKey) {
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