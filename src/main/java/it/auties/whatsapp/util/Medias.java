package it.auties.whatsapp.util;

import com.aspose.words.Document;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.SaveFormat;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.media.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public final class Medias {
    public static final String WEB_ORIGIN = "https://web.whatsapp.com";
    private static final String MOBILE_ANDROID_USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.57 Mobile Safari/537.36";
    private static final int WAVEFORM_SAMPLES = 64;
    private static final int PROFILE_PIC_SIZE = 640;
    private static final String DEFAULT_HOST = "mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;

    public static byte[] getProfilePic(byte[] file) {
        try {
            try (var inputStream = new ByteArrayInputStream(file)) {
                var inputImage = ImageIO.read(inputStream);
                var scaledImage = inputImage.getScaledInstance(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, Image.SCALE_SMOOTH);
                var outputImage = new BufferedImage(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, BufferedImage.TYPE_INT_RGB);
                var graphics2D = outputImage.createGraphics();
                graphics2D.drawImage(scaledImage, 0, 0, null);
                graphics2D.dispose();
                try (var outputStream = new ByteArrayOutputStream()) {
                    ImageIO.write(outputImage, "jpg", outputStream);
                    return outputStream.toByteArray();
                }
            }
        } catch (Throwable exception) {
            return file;
        }
    }

    @SafeVarargs
    public static CompletableFuture<byte[]> downloadAsync(URI uri, URI proxy, Map.Entry<String, String>... headers) {
        return downloadAsync(uri, proxy, MOBILE_ANDROID_USER_AGENT, headers);
    }

    @SafeVarargs
    public static CompletableFuture<byte[]> downloadAsync(URI uri, URI proxy, String userAgent, Map.Entry<String, String>... headers) {
        if (uri == null) {
            return CompletableFuture.completedFuture(null);
        }

        var request = HttpRequest.newBuilder()
                .uri(uri);
        if(userAgent != null) {
            request.header("User-Agent", userAgent);
        }
        for(var header : headers) {
            request.header(header.getKey(), header.getValue());
        }
        try(var client = createHttpClient(proxy)) {
            return client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray())
                    .thenApplyAsync(HttpResponse::body);
        }
    }

    public static CompletableFuture<MediaFile> upload(byte[] file, AttachmentType type, MediaConnection mediaConnection, URI proxy, String userAgent) {
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        var uploadData = type.inflatable() ? Bytes.compress(file) : file;
        var mediaFile = prepareMediaFile(type, uploadData);
        var path = type.path()
                .orElseThrow(() -> new UnsupportedOperationException(type + " cannot be uploaded"));
        var token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Objects.requireNonNullElse(mediaFile.fileEncSha256(), mediaFile.fileSha256()));
        var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(DEFAULT_HOST, path, token, auth, token));
        var body = Objects.requireNonNullElse(mediaFile.encryptedFile(), file);
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if(userAgent != null) {
            request.header("User-Agent", userAgent);
        }
        request.header("Content-Type", "application/octet-stream")
                .header("Accept", "application/json")
                .headers("Origin", WEB_ORIGIN)
                .build();
        try(var client = createHttpClient(proxy)) {
            return client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray()).thenApplyAsync(response -> {
                var upload = Json.readValue(response.body(), MediaUpload.class);
                return new MediaFile(
                        mediaFile.encryptedFile(),
                        mediaFile.fileSha256(),
                        mediaFile.fileEncSha256(),
                        mediaFile.mediaKey(),
                        mediaFile.fileLength(),
                        upload.directPath(),
                        upload.url(),
                        upload.handle(),
                        mediaFile.timestamp()
                );
            });
        }
    }

    private static MediaFile prepareMediaFile(AttachmentType type, byte[] uploadData) {
        var fileSha256 = Sha256.calculate(uploadData);
        if (type.keyName().isEmpty()) {
            return new MediaFile(null, fileSha256, null, null, uploadData.length, null, null, null, null);
        }

        var keys = MediaKeys.random(type.keyName().orElseThrow());
        var encryptedMedia = AesCbc.encrypt(keys.iv(), uploadData, keys.cipherKey());
        var hmac = calculateMac(encryptedMedia, keys);
        var encrypted = Bytes.concat(encryptedMedia, hmac);
        var fileEncSha256 = Sha256.calculate(encrypted);
        return new MediaFile(encrypted, fileSha256, fileEncSha256, keys.mediaKey(), uploadData.length, null, null, null, Clock.nowSeconds());
    }

    private static byte[] calculateMac(byte[] encryptedMedia, MediaKeys keys) {
        var hmacInput = Bytes.concat(keys.iv(), encryptedMedia);
        var hmac = Hmac.calculateSha256(hmacInput, keys.macKey());
        return Arrays.copyOf(hmac, 10);
    }

    public static CompletableFuture<Optional<byte[]>> downloadAsync(MutableAttachmentProvider<?> provider, URI proxy) {
       return provider.mediaUrl()
                .or(() -> provider.mediaDirectPath().map(Medias::createMediaUrl))
                .map(url -> {
                    var request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .build();
                    try(var client = createHttpClient(proxy)) {
                        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                                .thenApplyAsync(response -> handleResponse(provider, response.body()));
                    }
                })
                .orElseGet(() -> CompletableFuture.failedFuture(new NoSuchElementException("Missing url and path from media")));
    }

    public static String createMediaUrl(String directPath) {
        return "https://%s%s".formatted(DEFAULT_HOST, directPath);
    }

    private static Optional<byte[]> handleResponse(MutableAttachmentProvider<?> provider, byte[] body) {
        var sha256 = Sha256.calculate(body);
        if(provider.mediaEncryptedSha256().isPresent() && !Arrays.equals(sha256, provider.mediaEncryptedSha256().get())) {
            throw new IllegalArgumentException("Cannot decode media: Invalid sha256 signature");
        }
        var encryptedMedia = Arrays.copyOf(body, body.length - 10);
        var mediaMac = Arrays.copyOfRange(body, body.length - 10, body.length);
        var keyName = provider.attachmentType().keyName();
        if (keyName.isEmpty()) {
            return Optional.of(encryptedMedia);
        }

        var mediaKey = provider.mediaKey();
        if (mediaKey.isEmpty()) {
            return Optional.of(encryptedMedia);
        }

        var keys = MediaKeys.of(mediaKey.get(), keyName.get());
        var hmac = calculateMac(encryptedMedia, keys);
        if(!Arrays.equals(hmac, mediaMac)) {
            throw new HmacValidationException("media_decryption");
        }
        var decrypted = AesCbc.decrypt(keys.iv(), encryptedMedia, keys.cipherKey());
        return Optional.of(decrypted);
    }

    public static Optional<String> getMimeType(String name) {
        return getExtension(name)
                .map(extension -> Path.of("bogus%s".formatted(extension)))
                .flatMap(Medias::getMimeType);
    }

    private static Optional<String> getExtension(String name) {
        if (name == null) {
            return Optional.empty();
        }
        var index = name.lastIndexOf(".");
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(index));
    }

    public static Optional<String> getMimeType(Path path) {
        try {
            return Optional.ofNullable(Files.probeContentType(path));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static Optional<String> getMimeType(byte[] media) {
        try {
            return Optional.ofNullable(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(media)));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static OptionalInt getPagesCount(byte[] file) {
        try {
            var document = new Document(new ByteArrayInputStream(file));
            return OptionalInt.of(document.getPageCount());
        } catch (Throwable ignored) {
            return OptionalInt.empty();
        }
    }

    public static int getDuration(byte[] file) {
        try {
            var result = FFprobe.atPath()
                    .setShowEntries("format=duration")
                    .setSelectStreams(StreamType.VIDEO)
                    .setInput(new ByteArrayInputStream(file))
                    .execute();
            return Math.round(result.getFormat().getDuration());
        }catch (Throwable throwable) {
            return 0;
        }
    }

    public static MediaDimensions getDimensions(byte[] file, boolean video) {
        try {
            if (!video) {
                var originalImage = ImageIO.read(new ByteArrayInputStream(file));
                return new MediaDimensions(originalImage.getWidth(), originalImage.getHeight());
            }


            var result = FFprobe.atPath()
                    .setShowEntries("stream=width,height")
                    .setSelectStreams(StreamType.VIDEO)
                    .setInput(new ByteArrayInputStream(file))
                    .execute();
            return result.getStreams()
                    .stream()
                    .filter(entry -> entry.getCodecType() == StreamType.VIDEO)
                    .findFirst()
                    .map(stream -> new MediaDimensions(stream.getWidth(), stream.getHeight()))
                    .orElseGet(MediaDimensions::defaultDimensions);
        } catch (Exception throwable) {
            return MediaDimensions.defaultDimensions();
        }
    }

    public static Optional<byte[]> getDocumentThumbnail(byte[] file) {
        try(var stream = new ByteArrayOutputStream()) {
            var document = new Document(new ByteArrayInputStream(file));
            var options = new ImageSaveOptions(SaveFormat.JPEG);
            document.save(stream, options);
            return Optional.of(stream.toByteArray());
        }catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static Optional<byte[]> getImageThumbnail(byte[] file, boolean jpg) {
        try {
            var image = ImageIO.read(new ByteArrayInputStream(file));
            if (image == null) {
                return Optional.empty();
            }
            var type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
            var resizedImage = new BufferedImage(THUMBNAIL_SIZE, THUMBNAIL_SIZE, type);
            var graphics = resizedImage.createGraphics();
            graphics.drawImage(image, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE, null);
            graphics.dispose();
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, jpg ? "jpg" : "png", outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static Optional<byte[]> getVideoThumbnail(byte[] file) {
        try(var outputStream = new ByteArrayOutputStream()) {
            FFmpeg.atPath()
                    .addInput(PipeInput.pumpFrom(new ByteArrayInputStream(file)))
                    .setFilter(StreamType.VIDEO, "scale=%s:-1".formatted(THUMBNAIL_SIZE))
                    .addOutput(PipeOutput.pumpTo(outputStream)
                            .setFrameCount(StreamType.VIDEO, 1L)
                            .setFormat("image2")
                            .disableStream(StreamType.AUDIO)
                            .disableStream(StreamType.SUBTITLE))
                    .execute();
            return Optional.of(outputStream.toByteArray());
        }catch (IOException exception) {
            return Optional.empty();
        }
    }

    public static Optional<byte[]> getAudioWaveForm(byte[] audioData) {
        try {
            var rawData = toFloatArray(audioData);
            var blockSize = rawData.length / WAVEFORM_SAMPLES;
            var filteredData = IntStream.range(0, WAVEFORM_SAMPLES)
                    .map(i -> blockSize * i)
                    .map(blockStart -> IntStream.range(0, blockSize)
                            .map(j -> (int) Math.abs(rawData[blockStart + j]))
                            .sum())
                    .mapToObj(sum -> sum / blockSize)
                    .toList();
            var multiplier = Math.pow(Collections.max(filteredData), -1);
            var normalizedData = filteredData.stream()
                    .map(data -> (byte) Math.abs(100 * data * multiplier))
                    .toList();
            var waveform = new byte[normalizedData.size()];
            for (var i = 0; i < normalizedData.size(); i++) {
                waveform[i] = normalizedData.get(i);
            }
            return Optional.of(waveform);
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static float[] toFloatArray(byte[] audioData) {
        var rawData = new float[audioData.length / 4];
        ByteBuffer.wrap(audioData)
                .asFloatBuffer()
                .get(rawData);
        return rawData;
    }

    private static HttpClient createHttpClient(URI proxy) {
        var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS);
        if (proxy != null) {
            builder.proxy(Proxies.toProxySelector(proxy));
            builder.authenticator(Proxies.toAuthenticator(proxy));
        }
        return builder.build();
    }
}
