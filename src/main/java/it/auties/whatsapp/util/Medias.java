package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.media.*;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
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
    private static final int THUMBNAIL_SIZE = 32;
    private static final int RANDOM_FILE_NAME_LENGTH = 8;
    private static final Map<String, Path> CACHE = new ConcurrentHashMap<>();
    public static final int PROFILE_PIC_SIZE = 640;

    public MediaFile upload(byte[] file, MediaMessageType type, WhatsappStore store) {
        var client = HttpClient.newHttpClient();
        var auth = URLEncoder.encode(store.mediaConnection().auth(), StandardCharsets.UTF_8);
        var hosts = getHosts(store);
        return hosts.stream()
                .map(host -> upload(file, type, client, auth, host))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot upload media: no suitable host found: %s".formatted(hosts)));
    }

    private Optional<MediaFile> upload(byte[] file, MediaMessageType type, HttpClient client, String auth, String host) {
        try {
            var fileSha256 = Sha256.calculate(file);
            var keys = MediaKeys.random(type.keyName());
            var encryptedMedia = AesCbc.encrypt(keys.iv(), file, keys.cipherKey());
            var hmac = calculateMac(encryptedMedia, keys);
            var encrypted = Bytes.of(encryptedMedia)
                    .append(hmac)
                    .toByteArray();
            var fileEncSha256 = Sha256.calculate(encrypted);
            var token = Base64.getUrlEncoder().withoutPadding().encodeToString(fileEncSha256);
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s"
                    .formatted(host, type.path(), token, auth, token));
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

    @SneakyThrows
    public int getDuration(byte[] file, boolean video) {
        if(!video){
            try {
                var audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(file));
                var format = audioInputStream.getFormat();
                var audioFileLength = file.length;
                var frameSize = format.getFrameSize();
                var frameRate = format.getFrameRate();
                return (int) (audioFileLength / (frameSize * frameRate));
            }catch (UnsupportedAudioFileException exception){
                return getDuration(file, true);
            }
        }

        try {
            var input = createTempFile(file, true);
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s"
                            .formatted(input));
            if(process.waitFor() != 0){
                return 0;
            }

            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return (int) Float.parseFloat(result);
        }catch (Throwable throwable){
            return 0;
        }
    }

    @SneakyThrows
    public MediaDimensions getDimensions(byte[] file, boolean video) {
        if(!video) {
            var originalImage = ImageIO.read(new ByteArrayInputStream(file));
            return new MediaDimensions(originalImage.getWidth(), originalImage.getHeight());
        }

        record FfprobeResult(List<MediaDimensions> streams){

        }

        try {
            var input = createTempFile(file, true);
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -select_streams v -show_entries stream=width,height -of json %s"
                            .formatted(input));
            if(process.waitFor() != 0){
                return MediaDimensions.DEFAULT;
            }

            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var ffprobe = JSON.readValue(result, FfprobeResult.class);
            if(ffprobe.streams() == null || ffprobe.streams().isEmpty()){
                return MediaDimensions.DEFAULT;
            }


            return ffprobe.streams().get(0);
        }catch (Throwable throwable){
            return MediaDimensions.DEFAULT;
        }
    }

    @SneakyThrows
    public byte[] getThumbnail(byte[] file, Format format) {
        return switch (format){
            case JPG, PNG -> {
                var image = ImageIO.read(new ByteArrayInputStream(file));
                var resizedImage = getResizedImage(image, THUMBNAIL_SIZE);
                var outputStream = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, format.name().toLowerCase(), outputStream);
                yield outputStream.toByteArray();
            }

            case VIDEO -> {
                var input = createTempFile(file, true);
                var output = createTempFile(file, false);
                try {
                    var process = Runtime.getRuntime()
                            .exec("ffmpeg -ss 00:00:00 -i %s -y -vf scale=%s:-1 -vframes 1 -f image2 %s"
                                    .formatted(input, THUMBNAIL_SIZE, output));
                    if(process.waitFor() != 0){
                        yield null;
                    }

                    yield Files.readAllBytes(output);
                }catch (Throwable throwable){
                    yield null;
                } finally {
                    Files.delete(output);
                }
            }

            case FILE -> null;
        };
    }

    @SneakyThrows
    public byte[] getProfilePic(byte[] file){
        var originalImage = ImageIO.read(new ByteArrayInputStream(file));
        var size = Math.min(originalImage.getWidth(), originalImage.getHeight());
        var subImage = originalImage.getSubimage(0, 0, size, size);
        var actual = getResizedImage(subImage, PROFILE_PIC_SIZE);
        var outputStream = new ByteArrayOutputStream();
        var writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpeg").next();
        writer.getDefaultWriteParam().setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writer.getDefaultWriteParam().setCompressionQuality(0.5F);
        writer.setOutput(outputStream);
        writer.write(actual);
        writer.dispose();
        return outputStream.toByteArray();
    }

    private static BufferedImage getResizedImage(BufferedImage originalImage, int size) {
        var type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        var resizedImage = new BufferedImage(size, size, type);
        var graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
        graphics.dispose();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return resizedImage;
    }

    @SneakyThrows
    private Path createTempFile(byte[] media, boolean useCache){
        var hex = Bytes.of(media).toHex();
        var cached = CACHE.get(hex);
        if(useCache && cached != null){
            return cached;
        }

        var name = Bytes.ofRandom(RANDOM_FILE_NAME_LENGTH).toHex();
        var input = Files.createTempFile(name, "");
        if(useCache) {
            Files.write(input, media);
            CACHE.put(hex, input);
        }

        return input;
    }

    public enum Format {
        PNG,
        JPG,
        FILE,
        VIDEO
    }
}
