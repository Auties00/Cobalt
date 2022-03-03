package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.media.AttachmentProvider;
import it.auties.whatsapp.protobuf.media.MediaKeys;
import it.auties.whatsapp.protobuf.media.MediaUpload;
import it.auties.whatsapp.protobuf.message.model.MediaMessageType;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

@UtilityClass
public class Medias implements JacksonProvider {
    public MediaUpload upload(byte[] file, MediaMessageType type, WhatsappStore store) {
        var client = HttpClient.newHttpClient();
        var auth = URLEncoder.encode(store.mediaConnection().auth(), StandardCharsets.UTF_8);
        return store.mediaConnection().hosts()
                .stream()
                .map(host -> upload(file, type, client, auth, host))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot upload media: no suitable host found"));
    }

    private Optional<MediaUpload> upload(byte[] file, MediaMessageType type, HttpClient client, String auth, String host) {
        try {
            var keys = MediaKeys.random(type.whatsappName());
            var encryptedMedia = AesCbc.encrypt(keys.iv(), file, keys.cipherKey());
            var hmac = calculateMac(encryptedMedia, keys);
            var encrypted = Bytes.of(encryptedMedia)
                    .append(hmac)
                    .toByteArray();
            var sha256 = Base64.getEncoder().encodeToString(Sha256.calculate(encrypted).toByteArray());
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(host, type.uploadPath(), sha256, auth, sha256));
            var request = HttpRequest.newBuilder()
                    .POST(ofByteArray(encrypted))
                    .uri(uri)
                    .header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .header("Origin", "https://web.whatsapp.com")
                    .build();
            var response = client.send(request, ofString());
            Validate.isTrue(response.statusCode() == 200,
                    "Invalid status code: %s", response.statusCode());
            return ofNullable(JACKSON.readValue(response.body(), MediaUpload.class));
        }catch (Throwable ignored){
            return empty();
        }
    }

    public byte[] download(AttachmentProvider provider, WhatsappStore store) {
        return collectDownloadUrls(provider, store)
                .stream()
                .map(url -> download(provider, url))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot download encrypted media: no suitable host found"));
    }

    private Optional<byte[]> download(AttachmentProvider provider, String url) {
        try(var connection = new URL(url).openStream()){
            var stream = Bytes.of(connection.readAllBytes());

            var sha256 = Sha256.calculate(stream.toByteArray());
            Validate.isTrue(Arrays.equals(sha256.toByteArray(), provider.fileEncSha256()),
                    "Cannot decode media: Invalid sha256 signature",
                    SecurityException.class);

            var encryptedMedia = stream.cut(-10)
                    .toByteArray();
            var mediaMac = stream.slice(-10)
                    .toByteArray();

            var keys = MediaKeys.of(provider.key(), provider.keyName());
            var hmac = calculateMac(encryptedMedia, keys);
            Validate.isTrue(Arrays.equals(hmac, mediaMac),
                    "Cannot decode media: Hmac validation failed",
                    SecurityException.class);

            var decrypted = AesCbc.decrypt(keys.iv(), encryptedMedia, keys.cipherKey());
            Validate.isTrue(provider.fileLength() <= 0 || provider.fileLength() == decrypted.length,
                    "Cannot decode media: invalid size");

            return Optional.ofNullable(decrypted);
        } catch (Throwable ignored) {
            return empty();
        }
    }

    private byte[] calculateMac(byte[] encryptedMedia, MediaKeys keys) {
        var hmacInput = Bytes.of(keys.iv()).append(encryptedMedia).toByteArray();
        return Hmac.calculateSha256(hmacInput, keys.macKey())
                .cut(10)
                .toByteArray();
    }

    private List<String> collectDownloadUrls(AttachmentProvider provider, WhatsappStore store){
        if(provider.url() != null){
            return List.of(provider.url());
        }

        var fileEncSha256 = Base64.getEncoder().encode(provider.fileEncSha256());
        return store.mediaConnection()
                .hosts()
                .stream()
                .map(host -> "https://%s%s&hash=%s&mms-type=%s&__wa-mms=".formatted(host, provider.directPath(), fileEncSha256, provider.name()))
                .toList();
    }
}
