package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.media.*;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@UtilityClass
public class Medias implements JacksonProvider {
    private static final int SIZE = 32;
    private static final int RANDOM_FILE_NAME_LENGTH = 8;
    private static final Map<String, Path> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/x-m4v", "m4v",
            "video/quicktime", "mov",
            "audio/mpeg", "mp3",
            "audio/x-wav", "wav"
    );

    public MediaFile upload(byte[] file, MediaMessageType type, WhatsappStore store) {
        var client = HttpClient.newHttpClient();
        var auth = URLEncoder.encode(store.mediaConnection().auth(), StandardCharsets.UTF_8);
        return getHosts(store)
                .stream()
                .map(host -> upload(file, type, client, auth, host))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot upload media: no suitable host found"));
    }

    private Optional<MediaFile> upload(byte[] file, MediaMessageType type, HttpClient client, String auth, String host) {
        try {
            var fileSha256 = Sha256.calculate(file);
            var keys = MediaKeys.random(type.whatsappName());
            var encryptedMedia = AesCbc.encrypt(keys.iv(), file, keys.cipherKey());
            var hmac = calculateMac(encryptedMedia, keys);
            var encrypted = Bytes.of(encryptedMedia)
                    .append(hmac)
                    .toByteArray();
            var fileEncSha256 = Sha256.calculate(encrypted);
            var token = Bytes.of(fileEncSha256).toBase64();
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s"
                    .formatted(host, type.uploadPath(), token, auth, token));
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
            var upload = JSON.readValue(response.body(), MediaUpload.class);
            return of(new MediaFile(fileSha256, fileEncSha256, keys.mediaKey(), file.length, upload.directPath(), upload.url()));
        }catch (Throwable ignored){
            return empty();
        }
    }

    public byte[] download(AttachmentProvider provider, WhatsappStore store) {
        return getDownloadUrls(provider, store)
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
            Validate.isTrue(Arrays.equals(sha256, provider.fileEncSha256()),
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
        var hmacInput = Bytes.of(keys.iv())
                .append(encryptedMedia)
                .toByteArray();
        return Bytes.of(Hmac.calculateSha256(hmacInput, keys.macKey()))
                .cut(10)
                .toByteArray();
    }

    private List<String> getDownloadUrls(AttachmentProvider provider, WhatsappStore store){
        if(provider.url() != null){
            return List.of(provider.url());
        }

        var fileEncSha256 = Base64.getEncoder().encode(provider.fileEncSha256());
        return getHosts(store)
                .stream()
                .map(host -> "https://%s%s&hash=%s&mms-type=%s&__wa-mms=".formatted(host, provider.directPath(), fileEncSha256, provider.name()))
                .toList();
    }

    private List<String> getHosts(WhatsappStore store) {
        return Optional.ofNullable(store.mediaConnection())
                .map(MediaConnection::hosts)
                .orElse(List.of("mmg.whatsapp.net"));
    }

    public Optional<String> getMimeType(byte[] media){
        try {
            return Optional.ofNullable(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media)));
        }catch (IOException exception){
            return Optional.empty();
        }
    }

    private Optional<String> getMediaExtension(byte[] media){
        var guess = getMimeType(media);
        if(guess.isEmpty()){
            return Optional.empty();
        }

        var extension = MIME_TO_EXTENSION.get(guess.get());
        return Optional.ofNullable(extension);
    }

    @SneakyThrows
    public Optional<Integer> getDuration(byte[] file) {
        var input = createTempFile(file, true);
        if(input.isEmpty()){
            return Optional.empty();
        }

        try {
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s"
                            .formatted(input.get()));
            if(process.waitFor() != 0){
                return Optional.empty();
            }

            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return Optional.of(Math.round(Float.parseFloat(result)));
        }catch (Throwable throwable){
            return Optional.empty();
        }
    }

    @SneakyThrows
    public Optional<MediaDimensions> getDimensions(byte[] file) {
        var input = createTempFile(file, true);
        if(input.isEmpty()){
            return Optional.empty();
        }

        try {
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -select_streams v -show_entries stream=width,height -of json %s"
                            .formatted(input.get()));
            if(process.waitFor() != 0){
                return Optional.empty();
            }

            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var ffprobe = JSON.readValue(result, FfprobeResult.class);
            if(ffprobe.streams() == null || ffprobe.streams().isEmpty()){
                return Optional.empty();
            }


            return Optional.of(ffprobe.streams().get(0));
        }catch (Throwable throwable){
            return Optional.empty();
        }
    }

    private record FfprobeResult(@JsonProperty("streams") List<MediaDimensions> streams){

    }

    @SneakyThrows
    public Optional<byte[]> getThumbnail(byte[] file, Format format) {
        return switch (format){
            case JPG, PNG -> {
                var originalImage = ImageIO.read(new ByteArrayInputStream(file));
                var type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
                var resizedImage = new BufferedImage(SIZE, SIZE, type);
                var graphics = resizedImage.createGraphics();
                graphics.drawImage(originalImage, 0, 0, SIZE, SIZE, null);
                graphics.dispose();
                graphics.setComposite(AlphaComposite.Src);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                var outputStream = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, format.name().toLowerCase(), outputStream);
                yield Optional.of(outputStream.toByteArray());
            }

            case VIDEO -> {
                var input = createTempFile(file, true);
                if(input.isEmpty()){
                    yield Optional.empty();
                }

                var output = createTempFile(file, false);
                if(output.isEmpty()){
                    yield Optional.empty();
                }

                try {
                    var process = Runtime.getRuntime()
                            .exec("ffmpeg -ss 00:00:00 -i %s -y -vf scale=%s:-1 -vframes 1 -f image2 %s"
                                    .formatted(input.get(), SIZE, output.get()));
                    if(process.waitFor() != 0){
                        yield Optional.empty();
                    }

                    yield Optional.of(Files.readAllBytes(output.get()));
                }catch (Throwable throwable){
                    yield Optional.empty();
                } finally {
                    Files.delete(output.get());
                }
            }

            case FILE -> Optional.empty();
        };
    }

    @SneakyThrows
    private Optional<Path> createTempFile(byte[] media, boolean useCache){
        var extension = getMediaExtension(media);
        if(extension.isEmpty()){
            return Optional.empty();
        }

        var hex = Bytes.of(media).toHex();
        var cached = CACHE.get(hex);
        if(useCache && cached != null){
            return Optional.of(cached);
        }

        var name = Bytes.ofRandom(RANDOM_FILE_NAME_LENGTH).toHex();
        var input = Files.createTempFile(name, ".%s".formatted(extension.get()));
        if(useCache) {
            Files.write(input, media);
            CACHE.put(hex, input);
        }

        return Optional.of(input);
    }

    public enum Format {
        PNG,
        JPG,
        FILE,
        VIDEO
    }
}
