package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.media.*;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Specification.Whatsapp;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

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
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@UtilityClass
public class Medias implements JacksonProvider {
    public static final int PROFILE_PIC_SIZE = 640;
    public static final String DEFAULT_HOST = "https://mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;
    private static final int RANDOM_FILE_NAME_LENGTH = 8;
    private static final Map<String, Path> CACHE = new ConcurrentHashMap<>();

    public Optional<byte[]> getPreview(URI imageUri) {
        try {
            if (imageUri == null) {
                return Optional.empty();
            }
            var bytes = imageUri.toURL().openConnection().getInputStream().readAllBytes();
            return getImage(bytes, Format.JPG, -1);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<byte[]> getImage(byte[] file, Format format, int dimensions) {
        try {
            if (dimensions <= 0) {
                return Optional.of(file);
            }
            var image = ImageIO.read(new ByteArrayInputStream(file));
            if (image == null) {
                return Optional.empty();
            }
            var resizedImage = getResizedImage(image, dimensions);
            var outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, format.name().toLowerCase(), outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private BufferedImage getResizedImage(BufferedImage originalImage, int size) {
        var type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        var resizedImage = new BufferedImage(size, size, type);
        var graphics = resizedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, size, size, null);
        graphics.dispose();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return resizedImage;
    }

    public MediaFile upload(byte[] file, MediaMessageType type, MediaConnection mediaConnection) {
        var client = HttpClient.newHttpClient();
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        var hosts = getHosts(mediaConnection);
        return hosts.stream()
                .map(host -> upload(file, type, client, auth, host))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot upload media: no suitable host found: %s".formatted(hosts)));
    }

    private List<String> getHosts(MediaConnection mediaConnection) {
        return Optional.ofNullable(mediaConnection).map(MediaConnection::hosts).orElse(List.of(DEFAULT_HOST));
    }

    private Optional<MediaFile> upload(byte[] file, MediaMessageType type, HttpClient client, String auth, String host) {
        try {
            var fileSha256 = Sha256.calculate(file);
            var keys = MediaKeys.random(type.keyName());
            var encryptedMedia = AesCbc.encrypt(keys.iv(), file, keys.cipherKey());
            var hmac = calculateMac(encryptedMedia, keys);
            var encrypted = Bytes.of(encryptedMedia).append(hmac).toByteArray();
            var fileEncSha256 = Sha256.calculate(encrypted);
            var token = Base64.getUrlEncoder().withoutPadding().encodeToString(fileEncSha256);
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(host, type.path(), token, auth, token));
            var request = HttpRequest.newBuilder()
                    .POST(ofByteArray(encrypted))
                    .uri(uri)
                    .header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .header("Origin", Whatsapp.WEB_ORIGIN)
                    .build();
            var response = client.send(request, ofString());
            Validate.isTrue(response.statusCode() == 200, "Invalid status countryCode: %s", response.statusCode());
            var upload = JSON.readValue(response.body(), MediaUpload.class);
            return of(new MediaFile(fileSha256, fileEncSha256, keys.mediaKey(), file.length, upload.directPath(), upload.url()));
        } catch (Throwable ignored) {
            return empty();
        }
    }

    private byte[] calculateMac(byte[] encryptedMedia, MediaKeys keys) {
        var hmacInput = Bytes.of(keys.iv()).append(encryptedMedia).toByteArray();
        return Bytes.of(Hmac.calculateSha256(hmacInput, keys.macKey())).cut(10).toByteArray();
    }

    public CompletableFuture<Optional<byte[]>> download(AttachmentProvider provider) {
        try {
            Validate.isTrue(provider.mediaUrl() != null || provider.mediaDirectPath() != null, "Missing url and path from media");
            var url = Objects.requireNonNullElseGet(provider.mediaUrl(), () -> createMediaUrl(provider.mediaDirectPath()));
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApplyAsync(response -> handleResponse(provider, response));
        } catch (Throwable error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    public String createMediaUrl(@NonNull String directPath) {
        return DEFAULT_HOST + directPath;
    }

    private Optional<byte[]> handleResponse(AttachmentProvider provider, HttpResponse<byte[]> response) {
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND || response.statusCode() == HttpURLConnection.HTTP_GONE) {
            return Optional.empty();
        }
        var stream = Bytes.of(response.body());
        var sha256 = Sha256.calculate(stream.toByteArray());
        Validate.isTrue(Arrays.equals(sha256, provider.mediaEncryptedSha256()), "Cannot decode media: Invalid sha256 signature", SecurityException.class);
        var encryptedMedia = stream.cut(-10).toByteArray();
        var mediaMac = stream.slice(-10).toByteArray();
        var keys = MediaKeys.of(provider.mediaKey(), provider.mediaName());
        var hmac = calculateMac(encryptedMedia, keys);
        Validate.isTrue(Arrays.equals(hmac, mediaMac), "media_decryption", HmacValidationException.class);
        var decrypted = AesCbc.decrypt(keys.iv(), encryptedMedia, keys.cipherKey());
        return Optional.of(decrypted);
    }

    public Optional<String> getMimeType(String name) {
        return getExtension(name).map(extension -> Path.of("bogus%s".formatted(extension)))
                .flatMap(Medias::getMimeType);
    }

    private Optional<String> getExtension(String name) {
        if (name == null) {
            return Optional.empty();
        }
        var index = name.lastIndexOf(".");
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(index));
    }

    public Optional<String> getMimeType(Path path) {
        try {
            return Optional.ofNullable(Files.probeContentType(path));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> getMimeType(byte[] media) {
        try {
            return Optional.ofNullable(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media)));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public int getDuration(byte[] file, boolean video) {
        if (!video) {
            try {
                var audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(file));
                var format = audioInputStream.getFormat();
                var audioFileLength = file.length;
                var frameSize = format.getFrameSize();
                var frameRate = format.getFrameRate();
                return (int) (audioFileLength / (frameSize * frameRate));
            } catch (UnsupportedAudioFileException | IOException exception) {
                return getDuration(file, true);
            }
        }
        try {
            var input = createTempFile(file, true);
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 %s".formatted(input));
            if (process.waitFor() != 0) {
                return 0;
            }
            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return (int) Float.parseFloat(result);
        } catch (Throwable throwable) {
            return 0;
        }
    }

    public MediaDimensions getDimensions(byte[] file, boolean video) {
        try {
            if(!video){
                var originalImage = ImageIO.read(new ByteArrayInputStream(file));
                return new MediaDimensions(originalImage.getWidth(), originalImage.getHeight());
            }

            var input = createTempFile(file, true);
            var process = Runtime.getRuntime()
                    .exec("ffprobe -v error -select_streams v -show_entries stream=width,height -of json %s".formatted(input));
            if (process.waitFor() != 0) {
                return MediaDimensions.DEFAULT;
            }
            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            var ffprobe = JSON.readValue(result, FfprobeResult.class);
            if (ffprobe.streams() == null || ffprobe.streams().isEmpty()) {
                return MediaDimensions.DEFAULT;
            }
            return ffprobe.streams().get(0);
        } catch (Throwable throwable) {
            return MediaDimensions.DEFAULT;
        }
    }

    @SneakyThrows
    private Path createTempFile(byte[] media, boolean useCache) {
        var hex = Bytes.of(media).toHex();
        var cached = CACHE.get(hex);
        if (useCache && cached != null && Files.exists(cached)) {
            return cached;
        }
        var name = Bytes.ofRandom(RANDOM_FILE_NAME_LENGTH).toHex();
        var input = Files.createTempFile(name, "");
        if (useCache) {
            Files.write(input, media);
            CACHE.put(hex, input);
        }
        return input;
    }

    public Optional<byte[]> getThumbnail(byte[] file, Format format) {
        return switch (format) {
            case JPG, PNG -> getImage(file, format, THUMBNAIL_SIZE);
            case VIDEO -> getVideo(file);
            case FILE -> Optional.empty(); // TODO: 04/06/2022 Implement a file thumbnail
        };
    }

    private Optional<byte[]> getVideo(byte[] file) {
        var input = createTempFile(file, true);
        var output = createTempFile(file, false);
        try {
            var process = Runtime.getRuntime()
                    .exec("ffmpeg -ss 00:00:00 -i %s -y -vf scale=%s:-1 -vframes 1 -f image2 %s".formatted(input, Medias.THUMBNAIL_SIZE, output));
            if (process.waitFor() != 0) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(output));
        } catch (Throwable throwable) {
            return Optional.empty();
        } finally {
            try {
                Files.delete(output);
            } catch (IOException ignored) {}
        }
    }

    public byte[] getProfilePic(byte[] file) {
        try {
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
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot generate profile pic", exception);
        }
    }

    public enum Format {
        PNG,
        JPG,
        FILE,
        VIDEO
    }

    private record FfprobeResult(List<MediaDimensions> streams) {

    }
}
