package it.auties.whatsapp.util;

import com.aspose.words.Document;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.SaveFormat;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.media.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.Deflater;

public final class Medias {
    private static final String WEB_ORIGIN = "https://web.whatsapp.com";
    private static final int WAVEFORM_SAMPLES = 64;
    private static final int PROFILE_PIC_SIZE = 640;
    private static final String DEFAULT_HOST = "mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;
    private static final int MAC_LENGTH = 10;

    public static OutputStream getProfilePic(InputStream inputStream) {
        try {
            try (inputStream) {
                var inputImage = ImageIO.read(inputStream);
                var scaledImage = inputImage.getScaledInstance(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, Image.SCALE_SMOOTH);
                var outputImage = new BufferedImage(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, BufferedImage.TYPE_INT_RGB);
                var graphics2D = outputImage.createGraphics();
                graphics2D.drawImage(scaledImage, 0, 0, null);
                graphics2D.dispose();
                var outputStream = Streams.newByteArrayOutputStream();
                ImageIO.write(outputImage, "jpg", outputStream);
                return outputStream;
            }
        } catch (Throwable exception) {
            throw new RuntimeException("Cannot get profile pic", exception);
        }
    }

    public static MediaFile upload(byte[] file, AttachmentType type, MediaConnection mediaConnection, URI proxy, String userAgent) {
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        var uploadData = compressUpload(file, type);
        var mediaFile = prepareMediaFile(type, uploadData);
        var path = type.path()
                .orElseThrow(() -> new UnsupportedOperationException(type + " cannot be uploaded"));
        var token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Objects.requireNonNullElse(mediaFile.fileEncSha256(), mediaFile.fileSha256()));
        var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(DEFAULT_HOST, path, token, auth, token));
        var body = Objects.requireNonNullElse(mediaFile.encryptedFile(), file);
        var requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> Streams.newInputStream(body)));
        if (userAgent != null) {
            requestBuilder.header("User-Agent", userAgent);
        }
        var request = requestBuilder.header("Content-Type", "application/octet-stream")
                .header("Accept", "application/json")
                .headers("Origin", WEB_ORIGIN)
                .build();
        try (var client = createHttpClient(proxy)) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            var upload = MediaUpload.ofJson(response.body())
                    .orElseThrow(() -> new IllegalArgumentException("Cannot parse upload response: " + new String(response.body())));
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
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException("Cannot upload media", exception);
        }
    }

    // TODO: Explore if this can become a stream
    private static byte[] compressUpload(byte[] uncompressed, AttachmentType type) {
        if (!type.inflatable()) {
            return uncompressed;
        }

        var deflater = new Deflater();
        deflater.setInput(uncompressed);
        deflater.finish();
        try (var result = Streams.newByteArrayOutputStream()) {
            var buffer = new byte[8192];
            while (!deflater.finished()) {
                var length = deflater.deflate(buffer);
                result.write(buffer, 0, length);
            }
            deflater.end();
            return result.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static MediaFile prepareMediaFile(AttachmentType type, byte[] uploadData) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(uploadData);
            var fileSha256 = digest.digest();

            var keyName = type.keyName()
                    .orElse(null);
            if (keyName == null) {
                return new MediaFile(null, fileSha256, null, null, uploadData.length, null, null, null, null);
            }

            var keys = MediaKeys.random(keyName);
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(keys.cipherKey(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(keys.iv()));
            var encryptedLength = cipher.getOutputSize(uploadData.length);
            var encrypted = new byte[encryptedLength + MAC_LENGTH];
            if (cipher.doFinal(uploadData, 0, uploadData.length, encrypted, 0) != encryptedLength) {
                throw new IllegalArgumentException("Ciphertext length mismatch");
            }

            var mac = Mac.getInstance("HmacSHA256");
            var macKey = new SecretKeySpec(keys.macKey(), "HmacSHA256");
            mac.init(macKey);
            mac.update(keys.iv());
            mac.update(encrypted, 0, encryptedLength);
            var encryptedMac = mac.doFinal();
            System.arraycopy(encryptedMac, 0, encrypted, encryptedLength, MAC_LENGTH);

            digest.update(encrypted);
            var fileEncSha256 = digest.digest();

            return new MediaFile(encrypted, fileSha256, fileEncSha256, keys.mediaKey(), uploadData.length, null, null, null, Clock.nowSeconds());
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public static byte[] download(MutableAttachmentProvider provider, URI proxy) {
        var url = provider.mediaUrl()
                .or(() -> provider.mediaDirectPath().map(Medias::createMediaUrl))
                .orElse(null);
        if (url == null) {
            throw new RuntimeException(new IllegalArgumentException("Missing url or direct path from media"));
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try (var client = createHttpClient(proxy)) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            var payloadLength = (int) response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> new IllegalArgumentException("Unknown content length"));
            try (var payload = response.body()) {
                var ciphertext = payload.readNBytes(payloadLength - MAC_LENGTH);
                if (provider.mediaEncryptedSha256().isEmpty()) {
                    var expectedCiphertextSha256 = provider.mediaEncryptedSha256().get();
                    var sha256Digest = MessageDigest.getInstance("SHA-256");
                    var actualCiphertextSha256 = sha256Digest.digest(ciphertext);
                    if (!Arrays.equals(expectedCiphertextSha256, actualCiphertextSha256)) {
                        throw new HmacValidationException("media_decryption");
                    }
                }

                var keyName = provider.attachmentType().keyName();
                if (keyName.isEmpty()) {
                    return ciphertext;
                }

                var mediaKey = provider.mediaKey();
                if (mediaKey.isEmpty()) {
                    return ciphertext;
                }

                var keys = MediaKeys.of(mediaKey.get(), keyName.get());
                var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                var keySpec = new SecretKeySpec(keys.cipherKey(), "AES");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(keys.iv()));
                if (cipher.doFinal(ciphertext, 0, ciphertext.length, ciphertext, 0) != ciphertext.length) {
                    throw new IllegalStateException("Unexpected plaintext length");
                }

                var expectedCiphertextMac = payload.readNBytes(MAC_LENGTH);
                var localMac = Mac.getInstance("HmacSHA256");
                localMac.init(new SecretKeySpec(keys.macKey(), "HmacSHA256"));
                localMac.update(keys.iv());
                localMac.update(ciphertext);
                var actualCiphertextMac = localMac.doFinal();
                if (!Arrays.equals(expectedCiphertextMac, 0, MAC_LENGTH, actualCiphertextMac, 0, MAC_LENGTH)) {
                    throw new HmacValidationException("media_decryption");
                }

                return ciphertext;
            }
        }catch (Throwable throwable) {
            throw new RuntimeException("Cannot download media", throwable);
        }
    }

    public static <T> T download(MutableAttachmentProvider provider, URI proxy, Function<InputStream, T> decoder) {
        var url = provider.mediaUrl()
                .or(() -> provider.mediaDirectPath().map(Medias::createMediaUrl))
                .orElse(null);
        if (url == null) {
            throw new RuntimeException(new IllegalArgumentException("Missing url or direct path from media"));
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try (var client = createHttpClient(proxy)) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            var length = (int) response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> new IllegalArgumentException("Unknown content length"));
            try (var inputStream = response.body()) {
                var keyName = provider.attachmentType()
                        .keyName()
                        .orElseThrow(() -> new IllegalArgumentException("Missing key name for media"));
                var mediaKey = provider.mediaKey()
                        .orElseThrow(() -> new IllegalArgumentException("Missing media key for media"));
                var keys = MediaKeys.of(mediaKey, keyName);
                var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                var keySpec = new SecretKeySpec(keys.cipherKey(), "AES");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(keys.iv()));

                // Marvel of optimization
                final class AttachmentDecipherInputStream extends InputStream {
                    private final byte[] buffer;
                    private int offset, limit;
                    private long remaining;

                    private AttachmentDecipherInputStream() {
                        this.buffer = new byte[8192];
                        this.remaining = length - MAC_LENGTH;
                    }

                    @Override
                    public int read() throws IOException {
                        if (ensureDataAvailable()) {
                            return -1;
                        }

                        return buffer[offset++] & 0xFF;
                    }

                    private boolean ensureDataAvailable() throws IOException {
                        try {
                            while (offset >= limit) {
                                if (remaining == -1) {
                                    return true;
                                } else {
                                    this.offset = 0;
                                    if (remaining == 0) {
                                        this.limit = cipher.doFinal(buffer, 0);
                                        this.remaining = -1;
                                    } else {
                                        var readable = (int) Math.min(this.remaining, buffer.length);
                                        this.limit = inputStream.readNBytes(buffer, 0, readable);
                                        this.remaining -= limit;
                                        this.limit = cipher.update(buffer, 0, limit, buffer, 0);
                                    }
                                }
                            }

                            return false;
                        } catch (GeneralSecurityException exception) {
                            throw new IOException("Cannot decipher data", exception);
                        }
                    }
                }

                return decoder.apply(new AttachmentDecipherInputStream());
            } catch (Throwable exception) {
                throw new IllegalArgumentException("Cannot decipher media", exception);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createMediaUrl(String directPath) {
        return "https://%s%s".formatted(DEFAULT_HOST, directPath);
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
        } catch (Throwable exception) {
            return Optional.empty();
        }
    }

    public static Optional<String> getMimeType(byte[] file) {
        try (var inputStream = Streams.newInputStream(file)) {
            return Optional.ofNullable(URLConnection.guessContentTypeFromStream(inputStream));
        } catch (Throwable exception) {
            return Optional.empty();
        }
    }

    public static int getPagesCount(byte[] file) {
        try (var inputStream = Streams.newInputStream(file)) {
            var document = new Document(inputStream);
            return Math.max(document.getPageCount(), 1);
        } catch (Throwable ignored) {
            return 1;
        }
    }

    public static int getDuration(byte[] file) {
        try (var inputStream = Streams.newInputStream(file)) {
            var result = FFprobe.atPath()
                    .setShowEntries("format=duration")
                    .setSelectStreams(StreamType.VIDEO)
                    .setInput(inputStream)
                    .execute();
            return Math.round(result.getFormat().getDuration());
        } catch (Throwable throwable) {
            return 0;
        }
    }

    public static MediaDimensions getDimensions(byte[] file, boolean video) {
        try (var inputStream = Streams.newInputStream(file)) {
            if (!video) {
                var originalImage = ImageIO.read(inputStream);
                return new MediaDimensions(originalImage.getWidth(), originalImage.getHeight());
            }

            var result = FFprobe.atPath()
                    .setShowEntries("stream=width,height")
                    .setSelectStreams(StreamType.VIDEO)
                    .setInput(inputStream)
                    .execute();
            return result.getStreams()
                    .stream()
                    .filter(entry -> entry.getCodecType() == StreamType.VIDEO)
                    .findFirst()
                    .map(stream -> new MediaDimensions(stream.getWidth(), stream.getHeight()))
                    .orElseGet(MediaDimensions::defaultDimensions);
        } catch (Throwable throwable) {
            return MediaDimensions.defaultDimensions();
        }
    }

    public static byte[] getDocumentThumbnail(byte[] file) {
        try (
                var inputStream = Streams.newInputStream(file);
                var outputStream = Streams.newByteArrayOutputStream()
        ) {
            var document = new Document(inputStream);
            var options = new ImageSaveOptions(SaveFormat.JPEG);
            document.save(outputStream, options);
            return outputStream.toByteArray();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static byte[] getImageThumbnail(byte[] file, boolean jpg) {
        try (
                var inputStream = Streams.newInputStream(file);
                var outputStream = Streams.newByteArrayOutputStream()
        ) {
            var image = ImageIO.read(inputStream);
            if (image == null) {
                return null;
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
            ImageIO.write(resizedImage, jpg ? "jpg" : "png", outputStream);
            return outputStream.toByteArray();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static byte[] getVideoThumbnail(byte[] file) {
        try (
                var inputStream = Streams.newInputStream(file);
                var outputStream = Streams.newByteArrayOutputStream()
        ) {
            FFmpeg.atPath()
                    .addInput(PipeInput.pumpFrom(inputStream))
                    .setFilter(StreamType.VIDEO, "scale=%s:-1".formatted(THUMBNAIL_SIZE))
                    .addOutput(PipeOutput.pumpTo(outputStream)
                            .setFrameCount(StreamType.VIDEO, 1L)
                            .setFormat("image2")
                            .disableStream(StreamType.AUDIO)
                            .disableStream(StreamType.SUBTITLE))
                    .execute();
            return outputStream.toByteArray();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static byte[] getAudioWaveForm(byte[] audioData) {
        var rawDataBuffer = ByteBuffer.wrap(audioData)
                .asFloatBuffer();
        var rawSampleCount = rawDataBuffer.remaining();
        var blockSize = rawSampleCount / WAVEFORM_SAMPLES;
        var averagedAmplitudes = new float[WAVEFORM_SAMPLES];
        var maxAmplitude = 0f;
        for (var i = 0; i < WAVEFORM_SAMPLES; i++) {
            var blockStart = i * blockSize;
            var blockSum = 0.0;
            for (int j = 0; j < blockSize; j++) {
                blockSum += Math.abs(rawDataBuffer.get(blockStart + j));
            }
            averagedAmplitudes[i] = (float) (blockSum / blockSize);
            if (averagedAmplitudes[i] > maxAmplitude) {
                maxAmplitude = averagedAmplitudes[i];
            }
        }

        var waveform = new byte[WAVEFORM_SAMPLES];
        var multiplier = (maxAmplitude > 0f) ? 1.0f / maxAmplitude : 0f;
        for (var i = 0; i < WAVEFORM_SAMPLES; i++) {
            waveform[i] = (byte) (averagedAmplitudes[i] * multiplier * 100);
        }
        return waveform;
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
