package com.github.auties00.cobalt.util;

import com.aspose.words.Document;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.SaveFormat;
import com.github.auties00.cobalt.exception.HmacValidationException;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.exception.MediaUploadException;
import com.github.auties00.cobalt.model.media.*;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import com.github.kokorin.jaffree.ffmpeg.PipeOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

public final class Medias {
    private static final String WEB_ORIGIN_VALUE = "https://web.whatsapp.com";
    private static final int WAVEFORM_SAMPLES = 64;
    private static final int PROFILE_PIC_SIZE = 640;
    private static final String DEFAULT_HOST = "mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;
    private static final int MAC_LENGTH = 10;

    private Medias() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static byte[] getProfilePic(InputStream inputStream) {
        try {
            try (inputStream) {
                var inputImage = ImageIO.read(inputStream);
                var scaledImage = inputImage.getScaledInstance(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, Image.SCALE_SMOOTH);
                var outputImage = new BufferedImage(PROFILE_PIC_SIZE, PROFILE_PIC_SIZE, BufferedImage.TYPE_INT_RGB);
                var graphics2D = outputImage.createGraphics();
                graphics2D.drawImage(scaledImage, 0, 0, null);
                graphics2D.dispose();
                try (var outputStream = Streams.newByteArrayOutputStream()) {
                    ImageIO.write(outputImage, "jpg", outputStream);
                    return outputStream.toByteArray();
                }
            }
        } catch (Throwable exception) {
            throw new RuntimeException("Cannot get profile pic", exception);
        }
    }

    // TODO: Make this take an InputStream somehow
    public static MediaFile upload(byte[] file, AttachmentType type, MediaConnection mediaConnection, String userAgent) {
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        var mediaFile = prepareUpload(file, type);
        var path = type.path()
                .orElseThrow(() -> new MediaUploadException(type + " cannot be uploaded"));
        var token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Objects.requireNonNullElse(mediaFile.fileEncSha256(), mediaFile.fileSha256()));
        var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(DEFAULT_HOST, path, token, auth, token));
        var requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(mediaFile.encryptedFile()));
        if (userAgent != null) {
            requestBuilder.header("User-Agent", userAgent);
        }
        var request = requestBuilder.header("Content-Type", "application/octet-stream")
                .header("Accept", "application/json")
                .headers("Origin", WEB_ORIGIN_VALUE)
                .build();
        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            var upload = MediaUpload.ofJson(response.body())
                    .orElseThrow(() -> new MediaUploadException("Cannot parse upload response: " + new String(response.body())));
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
            throw new MediaUploadException(exception);
        }
    }

    private static MediaFile prepareUpload(byte[] input, AttachmentType type) {
        try {
            var timestamp = Clock.nowSeconds();
            var digest = MessageDigest.getInstance("SHA-256");
            if(!type.inflatable() && !type.cipherable()) {
                var fileSha256 = digest.digest(input);
                return new MediaFile(input, fileSha256, null, null, input.length, null, null, null, timestamp);
            }else if(type.cipherable() && !type.inflatable()) {
                var fileSha256 = digest.digest(input);
                var keyName = type.keyName()
                        .orElseThrow(() -> new InternalError("No key name for cipherable media"));
                var mediaKeys = MediaKeys.random(keyName);
                var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, mediaKeys.cipherKey(), mediaKeys.iv());
                var encryptedLength = cipher.getOutputSize(input.length);
                var ciphertextFile = new byte[encryptedLength + MAC_LENGTH];
                if (cipher.doFinal(input, 0, input.length, ciphertextFile, 0) != encryptedLength) {
                    throw new InternalError("Ciphertext length mismatch");
                }
                computeMac(mediaKeys, ciphertextFile, encryptedLength);
                digest.update(ciphertextFile);
                var fileEncSha256 = digest.digest();
                return new MediaFile(ciphertextFile, fileSha256, fileEncSha256, mediaKeys.mediaKey(), input.length, null, null, null, timestamp);
            }else if(!type.cipherable()) {
                try(var deflater = new Deflater(3)) {
                    deflater.setInput(input);
                    deflater.finish();
                    var compressed = Streams.newByteArrayOutputStream();
                    var buffer = new byte[8192];
                    while (!deflater.finished()) {
                        var count = deflater.deflate(buffer);
                        compressed.write(buffer, 0, count);
                        digest.update(buffer, 0, count);
                    }
                    var plaintextFile = compressed.toByteArray();
                    var fileSha256 = digest.digest();
                    return new MediaFile(plaintextFile, fileSha256, null, null, plaintextFile.length, null, null, null, timestamp);
                }
            }else {
                try (var deflater = new Deflater(3)) {
                    var compressed = new DeflaterInputStream(new ByteArrayInputStream(input), deflater, 8192);
                    var keyName = type.keyName()
                            .orElseThrow(() -> new InternalError("No key name for cipherable media"));
                    var mediaKeys = MediaKeys.random(keyName);
                    var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, mediaKeys.cipherKey(), mediaKeys.iv());
                    var buffer = new byte[8192];
                    int read;
                    var totalLength = 0;
                    while ((read = compressed.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                        cipher.update(buffer, 0, read);
                        totalLength += read;
                    }
                    var fileSha256 = digest.digest();
                    var encryptedLength = cipher.getOutputSize(0);
                    var encrypted = new byte[encryptedLength + MAC_LENGTH];
                    if (cipher.doFinal(encrypted, 0) != encryptedLength) {
                        throw new InternalError("Ciphertext length mismatch");
                    }
                    computeMac(mediaKeys, encrypted, encryptedLength);
                    digest.update(encrypted);
                    var fileEncSha256 = digest.digest();
                    return new MediaFile(encrypted, fileSha256, fileEncSha256, mediaKeys.mediaKey(), totalLength, null, null, null, timestamp);
                }
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new MediaUploadException(exception);
        }
    }

    private static void computeMac(MediaKeys mediaKeys, byte[] ciphertext, int ciphertextOffset) throws NoSuchAlgorithmException, InvalidKeyException {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(mediaKeys.macKey());
        mac.update(mediaKeys.iv().getIV());
        mac.update(ciphertext, 0, ciphertextOffset);
        var encryptedMac = mac.doFinal();
        System.arraycopy(encryptedMac, 0, ciphertext, ciphertextOffset, MAC_LENGTH);
    }

    public static InputStream download(MutableAttachmentProvider provider) {
        var url = provider.mediaUrl()
                .or(() -> provider.mediaDirectPath().map(Medias::createMediaUrl))
                .orElse(null);
        if (url == null) {
            throw new MediaDownloadException("Missing url or direct path from media");
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try {
            var client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            var payloadLength = (int) response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElseThrow(() -> new MediaDownloadException("Unknown content length"));

            var rawInputStream = response.body();
            var expectedPlaintextSha256 = provider.mediaSha256().orElse(null);
            var keyName = provider.attachmentType().keyName().orElse(null);
            var mediaKey = provider.mediaKey().orElse(null);
            var hasCipher = keyName != null && mediaKey != null;

            if (hasCipher) {
                return new MediaDecryptionInputStream(
                        rawInputStream,
                        payloadLength,
                        provider,
                        keyName,
                        mediaKey,
                        expectedPlaintextSha256,
                        client
                );
            } else {
                return new MediaValidationInputStream(
                        rawInputStream,
                        payloadLength,
                        expectedPlaintextSha256,
                        client
                );
            }
        } catch (GeneralSecurityException | IOException | InterruptedException throwable) {
            throw new MediaDownloadException(throwable);
        }
    }

    private static final class MediaDecryptionInputStream extends InputStream {
        private final InputStream rawInputStream;
        private final HttpClient client;
        private final byte[] buffer;
        private final byte[] plaintextBuffer;
        private final MessageDigest plaintextDigest;
        private final MessageDigest ciphertextDigest;
        private final Mac mac;
        private final Cipher cipher;
        private final byte[] expectedPlaintextSha256;
        private final byte[] expectedCiphertextSha256;

        private int bufferOffset = 0;
        private int bufferLimit = 0;
        private long remainingCiphertext;
        private boolean finished = false;
        private boolean validated = false;

        private MediaDecryptionInputStream(InputStream rawInputStream, int payloadLength,
                                           MutableAttachmentProvider provider, String keyName,
                                           byte[] mediaKey, byte[] expectedPlaintextSha256,
                                           HttpClient client) throws GeneralSecurityException {
            this.rawInputStream = rawInputStream;
            this.client = client;
            this.buffer = new byte[8192];
            this.plaintextBuffer = new byte[8192];
            this.expectedPlaintextSha256 = expectedPlaintextSha256;
            this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

            var keys = MediaKeys.of(mediaKey, keyName);
            this.expectedCiphertextSha256 = provider.mediaEncryptedSha256().orElse(null);
            this.ciphertextDigest = expectedCiphertextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;

            this.mac = Mac.getInstance("HmacSHA256");
            this.mac.init(keys.macKey());
            this.mac.update(keys.iv().getIV());

            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.cipher.init(Cipher.DECRYPT_MODE, keys.cipherKey(), keys.iv());

            this.remainingCiphertext = payloadLength - MAC_LENGTH;
        }

        @Override
        public int read() throws IOException {
            if (ensureDataAvailable()) {
                return -1;
            }
            return plaintextBuffer[bufferOffset++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (ensureDataAvailable()) {
                return -1;
            }

            int available = bufferLimit - bufferOffset;
            int toRead = Math.min(len, available);
            System.arraycopy(plaintextBuffer, bufferOffset, b, off, toRead);
            bufferOffset += toRead;
            return toRead;
        }

        private boolean ensureDataAvailable() throws IOException {
            try {
                while (bufferOffset >= bufferLimit && !finished) {
                    if (remainingCiphertext <= 0) {
                        // Final decryption
                        bufferOffset = 0;
                        bufferLimit = cipher.doFinal(plaintextBuffer, 0);

                        // Validate MAC
                        var expectedCiphertextMac = rawInputStream.readNBytes(MAC_LENGTH);
                        if (ciphertextDigest != null) {
                            ciphertextDigest.update(expectedCiphertextMac);
                            var actualCipherTextSha256 = ciphertextDigest.digest();
                            if (!Arrays.equals(expectedCiphertextSha256, actualCipherTextSha256)) {
                                throw new MediaDownloadException("Ciphertext SHA256 hash doesn't match the expected value");
                            }
                        }
                        var actualCiphertextMac = mac.doFinal();
                        if (!Arrays.equals(expectedCiphertextMac, 0, MAC_LENGTH, actualCiphertextMac, 0, MAC_LENGTH)) {
                            throw new HmacValidationException("media_decryption");
                        }

                        finished = true;
                    } else {
                        // Read and decrypt chunk
                        int toRead = (int) Math.min(remainingCiphertext, buffer.length);
                        int read = rawInputStream.readNBytes(buffer, 0, toRead);
                        if (read == 0) {
                            throw new IOException("Unexpected end of stream: expected " + remainingCiphertext + " more bytes");
                        }

                        remainingCiphertext -= read;

                        if (ciphertextDigest != null) {
                            ciphertextDigest.update(buffer, 0, read);
                        }
                        mac.update(buffer, 0, read);

                        bufferOffset = 0;
                        bufferLimit = cipher.update(buffer, 0, read, plaintextBuffer, 0);
                    }

                    if (plaintextDigest != null && bufferLimit > 0) {
                        plaintextDigest.update(plaintextBuffer, bufferOffset, bufferLimit - bufferOffset);
                    }
                }

                if (finished && bufferOffset >= bufferLimit && !validated) {
                    // Final validation
                    if (plaintextDigest != null) {
                        var actualPlaintextSha256 = plaintextDigest.digest();
                        if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                            throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                        }
                    }
                    validated = true;
                }

                return finished && bufferOffset >= bufferLimit;
            } catch (GeneralSecurityException exception) {
                throw new IOException("Cannot decrypt data", exception);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                rawInputStream.close();
            } finally {
                client.close();
            }
        }
    }

    private static final class MediaValidationInputStream extends InputStream {
        private final InputStream rawInputStream;
        private final HttpClient client;
        private final MessageDigest plaintextDigest;
        private final byte[] expectedPlaintextSha256;
        private long remainingBytes;
        private boolean validated = false;

        private MediaValidationInputStream(InputStream rawInputStream, int payloadLength,
                                           byte[] expectedPlaintextSha256, HttpClient client) throws NoSuchAlgorithmException {
            this.rawInputStream = rawInputStream;
            this.client = client;
            this.expectedPlaintextSha256 = expectedPlaintextSha256;
            this.plaintextDigest = expectedPlaintextSha256 != null ? MessageDigest.getInstance("SHA-256") : null;
            this.remainingBytes = payloadLength;
        }

        @Override
        public int read() throws IOException {
            if (remainingBytes <= 0) {
                validateIfNeeded();
                return -1;
            }

            int b = rawInputStream.read();
            if (b == -1) {
                throw new IOException("Unexpected end of stream");
            }

            remainingBytes--;
            if (plaintextDigest != null) {
                plaintextDigest.update((byte) b);
            }

            if (remainingBytes == 0) {
                validateIfNeeded();
            }

            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remainingBytes <= 0) {
                validateIfNeeded();
                return -1;
            }

            int toRead = (int) Math.min(len, remainingBytes);
            int read = rawInputStream.read(b, off, toRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }

            remainingBytes -= read;
            if (plaintextDigest != null) {
                plaintextDigest.update(b, off, read);
            }

            if (remainingBytes == 0) {
                validateIfNeeded();
            }

            return read;
        }

        private void validateIfNeeded() {
            if (!validated && plaintextDigest != null) {
                var actualPlaintextSha256 = plaintextDigest.digest();
                if (!Arrays.equals(expectedPlaintextSha256, actualPlaintextSha256)) {
                    throw new MediaDownloadException("Plaintext SHA256 hash doesn't match the expected value");
                }
                validated = true;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                rawInputStream.close();
            } finally {
                client.close();
            }
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
}
