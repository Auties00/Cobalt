package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.media.*;
import it.auties.whatsapp.model.message.model.MediaMessageType;
import it.auties.whatsapp.util.Spec.Whatsapp;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
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

@UtilityClass
public class Medias {
    public static final int THUMBNAIL_WIDTH = 480;
    public static final int THUMBNAIL_HEIGHT = 339;
    private final int PROFILE_PIC_SIZE = 640;
    private final String DEFAULT_HOST = "https://mmg.whatsapp.net";
    private final int THUMBNAIL_SIZE = 32;
    private final int RANDOM_FILE_NAME_LENGTH = 8;
    private final Map<String, Path> CACHE = new ConcurrentHashMap<>();

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
            var upload = Json.readValue(response.body(), MediaUpload.class);
            return Optional.of(new MediaFile(fileSha256, fileEncSha256, keys.mediaKey(), file.length, upload.directPath(), upload.url()));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
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

    public OptionalInt getPagesCount(byte[] file, String fileType){
        try(var inputStream = new ByteArrayInputStream(file)) {
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

    public int getDuration(byte[] file) {
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
            var ffprobe = Json.readValue(result, FfprobeResult.class);
            if (ffprobe.streams() == null || ffprobe.streams().isEmpty()) {
                return MediaDimensions.DEFAULT;
            }
            return ffprobe.streams().get(0);
        } catch (IOException | InterruptedException throwable) {
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
        };
    }

    public Optional<byte[]> getThumbnail(byte[] file, String fileType){
        return switch (fileType) {
            case "pdf" -> getPdf(file);
            case "pptx", "ppt" -> getPresentation(file);
            default -> Optional.empty();
        };
    }

    private Optional<byte[]> getPresentation(byte[] file) {
        try (var ppt = new XMLSlideShow(new ByteArrayInputStream(file)); var outputStream = new ByteArrayOutputStream()) {
            if(ppt.getSlides().isEmpty()){
                return Optional.empty();
            }
            var thumb = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
            var graphics2D = thumb.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            ppt.getSlides().get(0).draw(graphics2D);
            ImageIO.write(thumb, "jpg", outputStream);
            return Optional.of(outputStream.toByteArray());
        }catch (Throwable throwable){
            return Optional.empty();
        }
    }

    private Optional<byte[]> getPdf(byte[] file) {
        try (var document = PDDocument.load(file); var outputStream = new ByteArrayOutputStream()) {
            var renderer = new PDFRenderer(document);
            var image = renderer.renderImage(0);
            var thumb = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
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
            try(var inputStream = new ByteArrayInputStream(file)) {
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

    public enum Format {
        PNG,
        JPG,
        VIDEO
    }

    private record FfprobeResult(List<MediaDimensions> streams) {

    }
}
