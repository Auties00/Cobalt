package it.auties.whatsapp.util;

import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.media.*;
import it.auties.whatsapp.util.Specification.Whatsapp;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.imageio.ImageIO;
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
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

public final class Medias {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.ALWAYS)
            .build();
    private static final int PROFILE_PIC_SIZE = 640;
    private static final String DEFAULT_HOST = "mmg.whatsapp.net";
    private static final int THUMBNAIL_SIZE = 32;
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.57 Mobile Safari/537.36";

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

    public static CompletableFuture<byte[]> downloadAsync(String imageUrl) {
        return downloadAsync(URI.create(imageUrl));
    }

    public static Optional<byte[]> download(URI imageUri) {
        return downloadAsync(imageUri)
                .thenApplyAsync(Optional::ofNullable)
                .exceptionally(ignored -> Optional.empty())
                .join();
    }

    public static CompletableFuture<byte[]> downloadAsync(URI imageUri) {
        return downloadAsync(imageUri, true);
    }

    public static CompletableFuture<byte[]> downloadAsync(URI imageUri, boolean userAgent) {
        try {
            if (imageUri == null) {
                return CompletableFuture.completedFuture(null);
            }

            var request = HttpRequest.newBuilder()
                    .uri(imageUri)
                    .GET();
            if (userAgent) {
                request.header("User-Agent", USER_AGENT);
            }
            return CLIENT.sendAsync(request.build(), BodyHandlers.ofByteArray()).thenCompose(response -> {
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    return userAgent ? downloadAsync(imageUri, false)
                            : CompletableFuture.failedFuture(new IllegalArgumentException("Erroneous status code: " + response.statusCode()));
                }

                return CompletableFuture.completedFuture(response.body());
            });
        } catch (Throwable exception) {
            return userAgent ? downloadAsync(imageUri, false) : CompletableFuture.failedFuture(exception);
        }
    }

    public static CompletableFuture<MediaFile> upload(byte[] file, AttachmentType type, MediaConnection mediaConnection) {
        var auth = URLEncoder.encode(mediaConnection.auth(), StandardCharsets.UTF_8);
        var uploadData = type.inflatable() ? BytesHelper.compress(file) : file;
        var mediaFile = prepareMediaFile(type, uploadData);
        var path = type.path().orElseThrow(() -> new UnsupportedOperationException(type + " cannot be uploaded"));
        var token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(Objects.requireNonNullElse(mediaFile.fileEncSha256(), mediaFile.fileSha256()));
        var uri = URI.create("https://%s/%s/%s?auth=%s&token=%s".formatted(DEFAULT_HOST, path, token, auth, token));
        var request = HttpRequest.newBuilder()
                .POST(ofByteArray(Objects.requireNonNullElse(mediaFile.encryptedFile(), file)))
                .uri(uri)
                .header("Content-Type", "application/octet-stream")
                .header("Accept", "application/json")
                .header("Origin", Whatsapp.WEB_ORIGIN)
                .build();
        return CLIENT.sendAsync(request, ofString()).thenApplyAsync(response -> {
            Validate.isTrue(response.statusCode() == 200, "Invalid status code: %s", response.statusCode());
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

    private static MediaFile prepareMediaFile(AttachmentType type, byte[] uploadData) {
        var fileSha256 = Sha256.calculate(uploadData);
        if(type.keyName().isEmpty()) {
            return new MediaFile(null, fileSha256, null, null, uploadData.length, null, null, null, null);
        }

        var keys = MediaKeys.random(type.keyName().orElseThrow());
        var encryptedMedia = AesCbc.encrypt(keys.iv(), uploadData, keys.cipherKey());
        var hmac = calculateMac(encryptedMedia, keys);
        var encrypted = BytesHelper.concat(encryptedMedia, hmac);
        var fileEncSha256 = Sha256.calculate(encrypted);
        return new MediaFile(encrypted, fileSha256, fileEncSha256, keys.mediaKey(), uploadData.length, null, null, null, Clock.nowSeconds());
    }

    private static byte[] calculateMac(byte[] encryptedMedia, MediaKeys keys) {
        var hmacInput = BytesHelper.concat(keys.iv(), encryptedMedia);
        var hmac = Hmac.calculateSha256(hmacInput, keys.macKey());
        return Arrays.copyOf(hmac, 10);
    }

    public static CompletableFuture<Optional<byte[]>> download(MutableAttachmentProvider<?> provider) {
        try {
            var url = provider.mediaUrl()
                    .or(() -> provider.mediaDirectPath().map(Medias::createMediaUrl))
                    .orElseThrow(() -> new NoSuchElementException("Missing url and path from media"));
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApplyAsync(response -> handleResponse(provider, response));
        } catch (Throwable error) {
            return CompletableFuture.failedFuture(new RuntimeException("Cannot download media", error));
        }
    }

    public static String createMediaUrl(String directPath) {
        return "https://%s%s".formatted(DEFAULT_HOST, directPath);
    }

    private static Optional<byte[]> handleResponse(MutableAttachmentProvider<?> provider, HttpResponse<byte[]> response) {
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND || response.statusCode() == HttpURLConnection.HTTP_GONE) {
            return Optional.empty();
        }

        var body = response.body();
        var sha256 = Sha256.calculate(body);
        Validate.isTrue(provider.mediaEncryptedSha256().isEmpty() || Arrays.equals(sha256, provider.mediaEncryptedSha256().get()),
                "Cannot decode media: Invalid sha256 signature", SecurityException.class);
        var encryptedMedia = Arrays.copyOf(body, body.length - 10);
        var mediaMac = Arrays.copyOfRange(body, body.length - 10, body.length);
        var keyName = provider.attachmentType().keyName();
        if(keyName.isEmpty()) {
            return Optional.of(encryptedMedia);
        }

        var mediaKey = provider.mediaKey().orElseThrow(() -> new NoSuchElementException("Missing media key"));
        var keys = MediaKeys.of(mediaKey, keyName.get());
        var hmac = calculateMac(encryptedMedia, keys);
        Validate.isTrue(Arrays.equals(hmac, mediaMac), "media_decryption", HmacValidationException.class);
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

    public static OptionalInt getPagesCount(byte[] file, String fileType) {
        try (var inputStream = new ByteArrayInputStream(file)) {
            return switch (fileType) {
                case "docx" -> {
                    var docx = new XWPFDocument(inputStream);
                    var pages = docx.getProperties().getExtendedProperties().getUnderlyingProperties().getPages();
                    docx.close();
                    yield OptionalInt.of(pages);
                }
                case "doc" -> {
                    var wordDoc = new HWPFDocument(inputStream);
                    var pages = wordDoc.getSummaryInformation().getPageCount();
                    wordDoc.close();
                    yield OptionalInt.of(pages);
                }
                case "ppt" -> {
                    var document = new HSLFSlideShow(inputStream);
                    var slides = document.getSlides().size();
                    document.close();
                    yield OptionalInt.of(slides);
                }
                case "pptx" -> {
                    var show = new XMLSlideShow(inputStream);
                    var slides = show.getSlides().size();
                    show.close();
                    yield OptionalInt.of(slides);
                }
                default -> OptionalInt.empty();
            };
        } catch (Throwable throwable) {
            return OptionalInt.empty();
        }
    }

    public static int getDuration(byte[] file) {
        var input = createTempFile(file);
        try {
            var process = Runtime.getRuntime()
                    .exec(new String[]{"ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", input.toString()});
            if (process.waitFor() != 0) {
                return 0;
            }
            var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return (int) Float.parseFloat(result);
        } catch (Throwable throwable) {
            return 0;
        } finally {
            try {
                Files.deleteIfExists(input);
            } catch (IOException ignored) {

            }
        }
    }

    public static MediaDimensions getDimensions(byte[] file, boolean video) {
        try {
            if (!video) {
                var originalImage = ImageIO.read(new ByteArrayInputStream(file));
                return new MediaDimensions(originalImage.getWidth(), originalImage.getHeight());
            }

            var input = createTempFile(file);
            try {
                var process = Runtime.getRuntime()
                        .exec(new String[]{"ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "stream=width,height", "-of", "json", input.toString()});
                if (process.waitFor() != 0) {
                    return MediaDimensions.defaultDimensions();
                }
                var result = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                var ffprobe = Json.readValue(result, FfprobeResult.class);
                if (ffprobe.streams() == null || ffprobe.streams().isEmpty()) {
                    return MediaDimensions.defaultDimensions();
                }
                return ffprobe.streams().get(0);
            } finally {
                Files.deleteIfExists(input);
            }
        } catch (Exception throwable) {
            return MediaDimensions.defaultDimensions();
        }
    }

    private static Path createTempFile(byte[] data) {
        try {
            var file = Files.createTempFile(UUID.randomUUID().toString(), "");
            if (data != null) {
                Files.write(file, data);
            }
            return file;
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create temp file", exception);
        }
    }

    public static Optional<byte[]> getThumbnail(byte[] file, String fileType) {
        return getThumbnail(file, Format.ofDocument(fileType));
    }

    public static Optional<byte[]> getThumbnail(byte[] file, Format format) {
        return switch (format) {
            case UNKNOWN -> Optional.empty();
            case JPG, PNG -> getImageThumbnail(file, format);
            case PDF -> getPdfThumbnail(file);
            case PPTX -> getPresentationThumbnail(file);
            case VIDEO -> getVideoThumbnail(file);
        };
    }

    private static Optional<byte[]> getImageThumbnail(byte[] file, Format format) {
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
            ImageIO.write(resizedImage, format.name().toLowerCase(), outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static Optional<byte[]> getVideoThumbnail(byte[] file) {
        var input = createTempFile(file);
        var output = createTempFile(null);
        try {
            var process = Runtime.getRuntime()
                    .exec(new String[]{"ffmpeg", "-ss", "00:00:00", "-i", input.toString(), "-y", "-vf", "scale=%s:-1".formatted(THUMBNAIL_SIZE), "-vframes", "1", "-f", "image2", output.toString()});
            if (process.waitFor() != 0) {
                return Optional.empty();
            }
            return Optional.of(Files.readAllBytes(output));
        } catch (Throwable throwable) {
            return Optional.empty();
        } finally {
            try {
                Files.delete(input);
                Files.delete(output);
            } catch (IOException ignored) {

            }
        }
    }

    private static Optional<byte[]> getPdfThumbnail(byte[] file) {
        try (var document = PDDocument.load(file); var outputStream = new ByteArrayOutputStream()) {
            var renderer = new PDFRenderer(document);
            var image = renderer.renderImage(0);
            var thumb = new BufferedImage(Specification.Whatsapp.THUMBNAIL_WIDTH, Specification.Whatsapp.THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
            var graphics2D = thumb.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(image, 0, 0, thumb.getWidth(), thumb.getHeight(), null);
            graphics2D.dispose();
            ImageIO.write(thumb, "jpg", outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private static Optional<byte[]> getPresentationThumbnail(byte[] file) {
        try (var ppt = new XMLSlideShow(new ByteArrayInputStream(file)); var outputStream = new ByteArrayOutputStream()) {
            if (ppt.getSlides().isEmpty()) {
                return Optional.empty();
            }
            var thumb = new BufferedImage(Specification.Whatsapp.THUMBNAIL_WIDTH, Specification.Whatsapp.THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
            var graphics2D = thumb.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ppt.getSlides().get(0).draw(graphics2D);
            ImageIO.write(thumb, "jpg", outputStream);
            return Optional.of(outputStream.toByteArray());
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    public static enum Format {
        UNKNOWN,
        PNG,
        JPG,
        VIDEO,
        PDF,
        PPTX;

        static Format ofDocument(String fileType) {
            return fileType == null ? UNKNOWN : switch (fileType.toLowerCase()) {
                case "pdf" -> PDF;
                case "pptx", "ppt" -> PPTX;
                default -> UNKNOWN;
            };
        }
    }

    private record FfprobeResult(List<MediaDimensions> streams) {

    }

    public static Optional<byte[]> getAudioWaveForm(byte[] audioData) {
        try {
            var rawData = toFloatArray(audioData);
            var samples = 64;
            var blockSize = rawData.length / samples;
            var filteredData = IntStream.range(0, samples)
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
            IntStream.range(0, normalizedData.size())
                    .forEach(i -> waveform[i] = normalizedData.get(i));
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
}
