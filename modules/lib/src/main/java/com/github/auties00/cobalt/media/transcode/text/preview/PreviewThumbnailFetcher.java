package com.github.auties00.cobalt.media.transcode.text.preview;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

/**
 * Downloads and re-encodes inline thumbnails for every preview source
 * (group picture, business product image, page favicon).
 *
 * <p>Fetches an image URL, caps the response size to defend against
 * hostile servers, and re-encodes the bytes as a small JPEG so the
 * result fits the inline-thumbnail slot on {@link com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage}.
 *
 * @implNote This implementation resizes via {@link ImageIO} and
 * {@link BufferedImage} when the optional {@code java.desktop} module is
 * on the runtime path and falls back to the source bytes when it is not.
 * The Cobalt module is declared {@code requires static java.desktop} so
 * the build still succeeds on minimal runtimes.
 */
@WhatsAppWebModule(moduleName = "WAWebMediaDataUtils")
public final class PreviewThumbnailFetcher {
    /** The logger for {@link PreviewThumbnailFetcher}. */
    private static final System.Logger LOGGER = Log.get(PreviewThumbnailFetcher.class);

    /**
     * Holds the maximum response size, in bytes, accepted by
     * {@link #download(HttpClient, URI, Duration)}.
     *
     * <p>Larger responses are dropped because the inline JPEG slot is
     * meant for a small preview and an attacker-controlled server must
     * not be able to allocate hundreds of megabytes through the preview
     * pipeline.
     *
     * @implNote This implementation caps the payload at 5 MiB.
     */
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    /**
     * Holds the target side length, in pixels, of the resized JPEG
     * thumbnail.
     *
     * @implNote This implementation uses 100 pixels, matching WA Web's
     * preview sizing parameter ({@code {width: 100, height: 100, ...}}).
     */
    private static final int TARGET_SIZE = 100;

    /**
     * Holds the JPEG quality applied to the re-encoded thumbnail.
     *
     * @implNote This implementation uses 0.75, matching the
     * {@code imageFormatOptions: .75} parameter WA Web passes when
     * computing the preview thumbnail.
     */
    private static final float JPEG_QUALITY = 0.75f;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private PreviewThumbnailFetcher() {
        throw new AssertionError();
    }

    /**
     * Fetches {@code uri} via {@code httpClient} and returns the resized
     * JPEG bytes.
     *
     * <p>Issues a GET on {@code uri}, rejects non-2xx responses, empty
     * bodies, and bodies larger than {@link #MAX_BYTES}, then resizes
     * the body via {@link #tryResize(byte[])}. When resize is
     * unavailable the source bytes are returned unchanged. Returns
     * {@code null} on any failure (a {@code null} {@code httpClient} or
     * {@code uri}, a non-2xx response, an empty body, an oversized
     * payload, or a transport error) so the caller can fall back to a
     * minimal preview without inspecting the cause. When {@code timeout}
     * is {@code null} a 15-second timeout is applied.
     *
     * @param httpClient the HTTP client to issue the GET on
     * @param uri        the image URI
     * @param timeout    the per-request timeout; {@code null} applies a
     *                   15-second default
     * @return the resized JPEG bytes, the source bytes when resize was
     *         unavailable, or {@code null} on failure
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaDataUtils", exports = "getResizedThumbData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static byte[] download(HttpClient httpClient, URI uri, Duration timeout) {
        if (httpClient == null || uri == null) {
            return null;
        }
        try {
            var request = HttpRequest.newBuilder(uri)
                    .timeout(timeout != null ? timeout : Duration.ofSeconds(15))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "preview thumbnail download failed, status={0}", response.statusCode());
                return null;
            }
            var body = response.body();
            if (body == null || body.length == 0 || body.length > MAX_BYTES) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "preview thumbnail download rejected, bytes={0}",
                            body == null ? -1 : body.length);
                }
                return null;
            }
            var resized = tryResize(body);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "preview thumbnail downloaded, bytes={0}, resized={1}",
                        body.length, resized != null);
            }
            return resized != null ? resized : body;
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "preview thumbnail download failed", e);
            return null;
        }
    }

    /**
     * Decodes {@code source} as an image and re-encodes it as a JPEG
     * bounded by {@link #TARGET_SIZE} at {@link #JPEG_QUALITY}.
     *
     * <p>Aspect ratio is preserved by computing the scale factor from
     * the larger side so neither dimension exceeds {@link #TARGET_SIZE};
     * each output dimension is clamped to at least one pixel.
     *
     * @implNote This implementation returns {@code null} when
     * {@code java.desktop} is unavailable at runtime (the module is
     * declared {@code requires static java.desktop} so the JVM may run
     * without it), when the source bytes are not a recognised image
     * format, or when no JPEG writer is registered. The caller falls
     * back to the source bytes in that case.
     *
     * @param source the source image bytes
     * @return the resized JPEG bytes, or {@code null} when the resize
     *         failed
     */
    @WhatsAppWebExport(moduleName = "WAWebMediaDataUtils", exports = "getResizedThumbData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static byte[] tryResize(byte[] source) {
        try {
            var sourceImage = ImageIO.read(new ByteArrayInputStream(source));
            if (sourceImage == null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "preview thumbnail resize skipped, unrecognized image format");
                return null;
            }
            var width = sourceImage.getWidth();
            var height = sourceImage.getHeight();
            if (width <= 0 || height <= 0) {
                return null;
            }
            var scale = Math.min((double) TARGET_SIZE / width, (double) TARGET_SIZE / height);
            var targetWidth = Math.max(1, (int) Math.round(width * scale));
            var targetHeight = Math.max(1, (int) Math.round(height * scale));
            var resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = null;
            try {
                graphics = resized.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
            } finally {
                if (graphics != null) {
                    graphics.dispose();
                }
            }
            var output = new ByteArrayOutputStream();
            var writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "preview thumbnail resize failed, no jpeg writer available");
                return null;
            }
            var writer = writers.next();
            try (var stream = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(stream);
                var params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionType("JPEG");
                    params.setCompressionQuality(JPEG_QUALITY);
                }
                writer.write(null, new IIOImage(resized, null, null), params);
            } finally {
                writer.dispose();
            }
            return output.toByteArray();
        } catch (NoClassDefFoundError | RuntimeException | IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "preview thumbnail resize failed, falling back to source bytes", e);
            return null;
        }
    }
}
