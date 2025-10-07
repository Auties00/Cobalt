package com.github.auties00.cobalt.util;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.exception.HmacValidationException;
import com.github.auties00.cobalt.exception.MediaDownloadException;
import com.github.auties00.cobalt.exception.MediaUploadException;
import com.github.auties00.cobalt.model.media.AttachmentType;
import com.github.auties00.cobalt.model.media.MediaConnection;
import com.github.auties00.cobalt.model.media.MutableAttachmentProvider;

import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

public final class Medias {
    private static final String WEB_ORIGIN_VALUE = "https://web.whatsapp.com";
    private static final int WAVEFORM_SAMPLES = 64;
    private static final int PROFILE_PIC_SIZE = 640;
    private static final String DEFAULT_HOST = "mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;
    private static final int MAC_LENGTH = 10;
    private static final int BUFFER_LENGTH = 8192;

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
                try (var outputStream = new UnsafeByteArrayOutputStream()) {
                    ImageIO.write(outputImage, "jpg", outputStream);
                    return outputStream.toByteArray();
                }
            }
        } catch (Throwable exception) {
            throw new RuntimeException("Cannot get profile pic", exception);
        }
    }

    // TODO: Validate that we can just use a random token or at least omit it
    public static MediaUpload upload(InputStream file, AttachmentType type, MediaConnection mediaConnection, String userAgent) {
        var path = type.path()
                .orElseThrow(() -> new MediaUploadException(type + " cannot be uploaded"));
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var timestamp = Clock.nowSeconds();
            var mediaKeys = type.keyName()
                    .map(MediaKeys::random)
                    .orElse(null);
            var uploadStream = createUploadStream(file, type, mediaKeys);
            var token = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(Bytes.random(32));
            var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(DEFAULT_HOST, path, token, auth, token));
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> uploadStream));
            if (userAgent != null) {
                requestBuilder.header("User-Agent", userAgent);
            }
            var request = requestBuilder.header("Content-Type", "application/octet-stream")
                    .header("Accept", "application/json")
                    .headers("Origin", WEB_ORIGIN_VALUE)
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode() != 200) {
                throw new MediaUploadException("Cannot upload media: status code " + response.statusCode());
            }

            var jsonObject = JSON.parseObject(response.body());
            if(jsonObject == null) {
                throw new MediaUploadException("Cannot parse upload response: " + new String(response.body()));
            }

            var directPath = jsonObject.getString("direct_path");
            var url = jsonObject.getString("url");
            var handle = jsonObject.getString("handle");
            return new MediaUpload(
                    uploadStream.fileSha256(),
                    uploadStream.fileEncSha256(),
                    mediaKeys != null ? mediaKeys.mediaKey() : null,
                    uploadStream.fileLength(),
                    directPath,
                    url,
                    handle,
                    timestamp
            );
        } catch (InterruptedException | IOException | GeneralSecurityException exception) {
            throw new MediaUploadException(exception);
        }
    }

    private record MediaKeys(byte[] mediaKey, IvParameterSpec iv, SecretKeySpec cipherKey, SecretKeySpec macKey, byte[] ref) {
        private static final int EXPANDED_SIZE = 112;
        private static final int KEY_LENGTH = 32;
        private static final int IV_LENGTH = 16;

        public static MediaKeys random(String type) {
            return of(Bytes.random(32), type);
        }

        public static MediaKeys of(byte[] key, String type) {
            try {
                var keyName = type.getBytes(StandardCharsets.UTF_8);
                var hkdf = KDF.getInstance("HKDF-SHA256");
                var params = HKDFParameterSpec.ofExtract()
                        .addIKM(new SecretKeySpec(key, "AES"))
                        .thenExpand(keyName, EXPANDED_SIZE);
                var expanded = hkdf.deriveData(params);
                var iv = new IvParameterSpec(expanded, 0, IV_LENGTH);
                var cipherKey = new SecretKeySpec(expanded, IV_LENGTH,  KEY_LENGTH, "AES");
                var macKey = new SecretKeySpec(expanded, IV_LENGTH + KEY_LENGTH, KEY_LENGTH, "HmacSHA256");
                var ref = Arrays.copyOfRange(expanded, IV_LENGTH + KEY_LENGTH + KEY_LENGTH + KEY_LENGTH, expanded.length);
                return new MediaKeys(key, iv, cipherKey, macKey, ref);
            }catch (GeneralSecurityException exception) {
                throw new RuntimeException("Cannot generate media keys", exception);
            }
        }
    }

    public record MediaUpload(byte[] fileSha256, byte[] fileEncSha256, byte[] mediaKey, long fileLength, String directPath, String url, String handle, Long timestamp) {

    }

    private static MediaUploadInputStream createUploadStream(InputStream input, AttachmentType type, MediaKeys mediaKeys) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        if(!type.inflatable() && !type.cipherable()) {
            return new PlaintextMediaUploadInputStream(input);
        }else if(type.cipherable() && !type.inflatable()) {
            return new CiphertextMediaUploadInputStream(input, mediaKeys);
        }else if(!type.cipherable()) {
            return new PlaintextDeflatedMediaUploadInputStream(input);
        }else {
            return new CiphertextDeflatedMediaUploadInputStream(input, mediaKeys);
        }
    }

    private abstract static class MediaUploadInputStream extends InputStream {
        public abstract long fileLength();
        public abstract byte[] fileSha256();
        public abstract byte[] fileEncSha256();
    }

    private static class PlaintextMediaUploadInputStream extends MediaUploadInputStream {
        private final InputStream plaintextInput;
        private final MessageDigest plaintextDigest;
        private long plaintextLength;

        private PlaintextMediaUploadInputStream(InputStream plaintextInput) throws NoSuchAlgorithmException {
            this.plaintextInput = plaintextInput;
            this.plaintextDigest = MessageDigest.getInstance("SHA-256");
            this.plaintextLength = 0;
        }

        @Override
        public int read() throws IOException {
            var ch = plaintextInput.read();
            if (ch != -1) {
                plaintextDigest.update((byte)ch);
                plaintextLength++;
            }
            return ch;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var result = plaintextInput.read(b, off, len);
            if (result != -1) {
                plaintextDigest.update(b, off, result);
                plaintextLength += result;
            }
            return result;
        }

        @Override
        public void close() throws IOException {
            plaintextInput.close();
        }

        @Override
        public long fileLength() {
            return plaintextLength;
        }

        @Override
        public byte[] fileSha256() {
            return plaintextDigest.digest();
        }

        @Override
        public byte[] fileEncSha256() {
            return null;
        }
    }

    private static final class PlaintextDeflatedMediaUploadInputStream extends PlaintextMediaUploadInputStream {
        private final Deflater deflater;

        private PlaintextDeflatedMediaUploadInputStream(InputStream plaintextInput) throws NoSuchAlgorithmException {
            var deflater = new Deflater();
            super(new DeflaterInputStream(plaintextInput, deflater, BUFFER_LENGTH));
            this.deflater = deflater;
        }

        @Override
        public void close() throws IOException {
            super.close();
            deflater.close();
        }
    }

    private static class CiphertextMediaUploadInputStream extends MediaUploadInputStream {
        private final InputStream plaintextInput;
        private final MessageDigest plaintextDigest;
        private final MessageDigest ciphertextDigest;
        private final Mac ciphertextMac;
        private final Cipher cipher;
        private final byte[] plaintextBuffer;
        private final byte[] ciphertextBuffer;
        private final byte[] outputBuffer;

        private long plaintextLength;
        private boolean finalized;
        private int outputPosition;
        private int outputLimit;

        private CiphertextMediaUploadInputStream(InputStream plaintextInput, MediaKeys mediaKeys) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
            this.plaintextInput = plaintextInput;
            this.plaintextDigest = MessageDigest.getInstance("SHA-256");
            this.ciphertextDigest = MessageDigest.getInstance("SHA-256");
            this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, mediaKeys.cipherKey(), mediaKeys.iv());

            this.ciphertextMac = Mac.getInstance("HmacSHA256");
            ciphertextMac.init(mediaKeys.macKey());
            ciphertextMac.update(mediaKeys.iv().getIV());

            this.plaintextBuffer = new byte[BUFFER_LENGTH];
            var blockSize = cipher.getBlockSize();
            this.ciphertextBuffer = new byte[BUFFER_LENGTH + blockSize];
            this.outputBuffer = new byte[BUFFER_LENGTH];
            this.plaintextLength = 0;
        }

        @Override
        public int read() throws IOException {
            ensureDataAvailable();
            if (outputPosition >= outputLimit) {
                return -1;
            }

            return outputBuffer[outputPosition++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureDataAvailable();
            if (outputPosition >= outputLimit) {
                return -1;
            }

            var available = outputLimit - outputPosition;
            var toRead = Math.min(len, available);
            System.arraycopy(outputBuffer, outputPosition, b, off, toRead);
            outputPosition += toRead;
            return toRead;
        }

        private void ensureDataAvailable() throws IOException{
            try {
                while (outputPosition >= outputLimit && !finalized) {
                    this.outputPosition = 0;
                    this.outputLimit = 0;

                    var plaintextRead = plaintextInput.read(plaintextBuffer, 0, plaintextBuffer.length);
                    if (plaintextRead == -1) {
                        plaintextInput.close();

                        var finalCiphertextLen = cipher.doFinal(ciphertextBuffer, 0);
                        processChunk(finalCiphertextLen);

                        var mac = ciphertextMac.doFinal();
                        ciphertextDigest.update(mac, 0, MAC_LENGTH);

                        var macSpace = outputBuffer.length - outputLimit;
                        var macToCopy = Math.min(MAC_LENGTH, macSpace);
                        System.arraycopy(mac, 0, outputBuffer, outputLimit, macToCopy);
                        outputLimit += macToCopy;

                        finalized = true;
                        break;
                    }

                    plaintextDigest.update(plaintextBuffer, 0, plaintextRead);
                    plaintextLength += plaintextRead;

                    var ciphertextLen = cipher.update(plaintextBuffer, 0, plaintextRead, ciphertextBuffer, 0);
                    processChunk(ciphertextLen);
                }
            }catch (GeneralSecurityException exception) {
                throw new IOException("Cannot encrypt data", exception);
            }
        }

        private void processChunk(int length) {
            if (length <= 0) {
                return;
            }

            ciphertextDigest.update(ciphertextBuffer, 0, length);
            ciphertextMac.update(ciphertextBuffer, 0, length);
            var toCopy = Math.min(length, outputBuffer.length);
            System.arraycopy(ciphertextBuffer, 0, outputBuffer, 0, toCopy);
            outputLimit = toCopy;
        }

        @Override
        public void close() throws IOException {
            plaintextInput.close();
        }

        @Override
        public long fileLength() {
            return plaintextLength;
        }

        @Override
        public byte[] fileSha256() {
            return plaintextDigest.digest();
        }

        @Override
        public byte[] fileEncSha256() {
            return ciphertextDigest.digest();
        }
    }

    private static final class CiphertextDeflatedMediaUploadInputStream extends CiphertextMediaUploadInputStream {
        private final Deflater deflater;

        private CiphertextDeflatedMediaUploadInputStream(InputStream plaintextInput, MediaKeys mediaKeys) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException {
            var deflater = new Deflater();
            super(new DeflaterInputStream(plaintextInput, deflater, BUFFER_LENGTH), mediaKeys);
            this.deflater = deflater;
        }

        @Override
        public void close() throws IOException {
            super.close();
            deflater.close();
        }
    }

    private static void computeUploadMac(MediaKeys mediaKeys, byte[] ciphertext, int ciphertextOffset) throws NoSuchAlgorithmException, InvalidKeyException {
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

        try(var client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
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
                        expectedPlaintextSha256
                );
            } else {
                return new MediaValidationInputStream(
                        rawInputStream,
                        payloadLength,
                        expectedPlaintextSha256
                );
            }
        } catch (GeneralSecurityException | IOException | InterruptedException throwable) {
            throw new MediaDownloadException(throwable);
        }
    }

    private static final class MediaDecryptionInputStream extends InputStream {
        private final InputStream rawInputStream;
        private final byte[] ciphertextBuffer;
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
                                           byte[] mediaKey, byte[] expectedPlaintextSha256) throws GeneralSecurityException {
            this.rawInputStream = rawInputStream;
            this.ciphertextBuffer = new byte[BUFFER_LENGTH];
            this.plaintextBuffer = new byte[BUFFER_LENGTH];
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

            var available = bufferLimit - bufferOffset;
            var toRead = Math.min(len, available);
            System.arraycopy(plaintextBuffer, bufferOffset, b, off, toRead);
            bufferOffset += toRead;
            return toRead;
        }

        private boolean ensureDataAvailable() throws IOException {
            try {
                while (bufferOffset >= bufferLimit && !finished) {
                    if (remainingCiphertext == 0) {
                        bufferOffset = 0;
                        bufferLimit = cipher.doFinal(plaintextBuffer, 0);

                        // Validate MAC
                        var expectedCiphertextMac = new byte[MAC_LENGTH];
                        var expectedCiphertextMacOffset = 0;
                        while (expectedCiphertextMacOffset < expectedCiphertextSha256.length) {
                            var read = rawInputStream.read(expectedCiphertextMac, expectedCiphertextMacOffset, MAC_LENGTH - expectedCiphertextMacOffset);
                            if(read == -1) {
                                throw new IOException("Unexpected end of stream");
                            }

                            expectedCiphertextMacOffset += read;
                        }

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
                        var toRead = remainingCiphertext < ciphertextBuffer.length ? Math.toIntExact(remainingCiphertext) : ciphertextBuffer.length;
                        var read = rawInputStream.read(ciphertextBuffer, 0, toRead);
                        if (read == 0) {
                            throw new IOException("Unexpected end of stream: expected " + remainingCiphertext + " more bytes");
                        }

                        remainingCiphertext -= read;

                        if (ciphertextDigest != null) {
                            ciphertextDigest.update(ciphertextBuffer, 0, read);
                        }
                        mac.update(ciphertextBuffer, 0, read);

                        bufferOffset = 0;
                        bufferLimit = cipher.update(ciphertextBuffer, 0, read, plaintextBuffer, 0);
                    }

                    if (plaintextDigest != null && bufferLimit > 0) {
                        plaintextDigest.update(plaintextBuffer, bufferOffset, bufferLimit - bufferOffset);
                    }
                }

                if (finished && bufferOffset >= bufferLimit && !validated) {
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
            rawInputStream.close();
        }
    }

    private static final class MediaValidationInputStream extends InputStream {
        private final InputStream rawInputStream;
        private final MessageDigest plaintextDigest;
        private final byte[] expectedPlaintextSha256;
        private long remainingBytes;
        private boolean validated = false;

        private MediaValidationInputStream(InputStream rawInputStream, int payloadLength,
                                           byte[] expectedPlaintextSha256) throws NoSuchAlgorithmException {
            this.rawInputStream = rawInputStream;
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

            var b = rawInputStream.read();
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

            var toRead = (int) Math.min(len, remainingBytes);
            var read = rawInputStream.read(b, off, toRead);
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
            rawInputStream.close();
        }
    }

    public static String createMediaUrl(String directPath) {
        return "https://%s%s".formatted(DEFAULT_HOST, directPath);
    }

    // TODO: Implement a stream that guesses the mime type and based on that computes metadata
}
