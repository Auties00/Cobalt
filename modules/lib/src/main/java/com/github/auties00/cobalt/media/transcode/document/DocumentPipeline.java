package com.github.auties00.cobalt.media.transcode.document;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.media.transcode.MediaTranscoderService;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Drives the document branch of the upload transcoder.
 *
 * <p>For PDF input the pipeline renders page 0 via Apache PDFBox, downscales the rendering to
 * a {@value #DOC_THUMB_MAX_EDGE}-pixel maximum edge, JPEG-encodes the result, and reports the
 * document page count. For every other document type the source bytes pass through verbatim; no
 * thumbnail is generated and no page count is recorded. The original document bytes are always
 * the transcoded payload that ships to the WhatsApp CDN, returned as a {@link MediaPayload}.
 *
 * @implNote
 * This implementation stays inside the JDK and PDFBox; it does not invoke FFmpeg. The thumbnail
 * is size-tuned via {@link ImageWriteParam#setCompressionQuality(float)} so the encoded JPEG fits
 * inside the {@value #MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES}-byte micro-thumbnail budget WhatsApp
 * pins on the wire; quality is stepped from {@link #THUMBNAIL_QUALITY_START} downwards by
 * {@link #THUMBNAIL_QUALITY_STEP} until the encode fits or {@link #THUMBNAIL_QUALITY_FLOOR} is
 * reached.
 */
public final class DocumentPipeline {
    /** The logger for {@link DocumentPipeline}. */
    private static final System.Logger LOGGER = Log.get(DocumentPipeline.class);

    /**
     * Holds the MIME type reported when the source is identifiably a PDF document.
     */
    private static final String PDF_MIMETYPE = "application/pdf";

    /**
     * Holds the MIME type used as the pass-through default when no other inference applies.
     */
    private static final String DEFAULT_MIMETYPE = "application/octet-stream";

    /**
     * Holds the magic-byte prefix {@code "%PDF-"} that identifies a PDF document.
     */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    /**
     * Bounds the longer edge in pixels of the rendered page used as the document thumbnail.
     */
    private static final int DOC_THUMB_MAX_EDGE = 480;

    /**
     * Sets the render DPI for the first PDF page before downscaling to {@link #DOC_THUMB_MAX_EDGE}.
     *
     * @implNote
     * This implementation uses {@code 96}, matching the screen-resolution default Apache PDFBox
     * samples use, which keeps the off-heap buffer cost predictable for large pages.
     */
    private static final int PDF_RENDER_DPI = 96;

    /**
     * Caps the encoded thumbnail size in bytes.
     *
     * @implNote
     * This implementation pins {@code 1300} bytes, the micro-thumbnail budget WhatsApp enforces
     * on the wire for the in-band document preview.
     */
    private static final int MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES = 1300;

    /**
     * Holds the initial JPEG quality, in the JDK's {@code 0.0..1.0} range, used when encoding the
     * document thumbnail.
     */
    private static final float THUMBNAIL_QUALITY_START = 0.6f;

    /**
     * Holds the lower bound on JPEG quality below which the size-tuning loop stops trying to shrink
     * the encoded thumbnail.
     */
    private static final float THUMBNAIL_QUALITY_FLOOR = 0.10f;

    /**
     * Holds the multiplier applied to the JPEG quality on each retry pass.
     */
    private static final float THUMBNAIL_QUALITY_STEP = 0.66f;

    /**
     * Constructs the pipeline.
     *
     * <p>The parent {@link MediaTranscoderService} owns the single instance.
     */
    public DocumentPipeline() {
    }

    /**
     * Pass-through transcodes the source document, applies metadata to {@code provider}, and
     * returns the resulting payload.
     *
     * <p>Mutates the provider in place. Every {@link MediaProvider} receives the source byte
     * length via {@link MediaProvider#setMediaSize(long)}. When {@code provider} is a
     * {@link DocumentMessage} the MIME type is set to either {@value #PDF_MIMETYPE} or
     * {@value #DEFAULT_MIMETYPE}; for PDF sources the page count and a page-zero
     * {@code jpegThumbnail} are populated as well. Documents other than PDF pass through
     * verbatim with no thumbnail; PDFs trigger a page-0 render for the in-band thumbnail. The
     * returned {@link MediaPayload} always wraps the original {@code source} bytes unchanged.
     *
     * @param provider the upload target whose metadata fields are applied to this instance
     * @param source   the raw document file
     * @return the document payload wrapping the original source bytes
     * @throws WhatsAppMediaException.Processing if the source cannot be read or, for a PDF source,
     *         if rendering or thumbnail encoding fails
     */
    public MediaPayload run(MediaProvider provider, Path source)
            throws WhatsAppMediaException.Processing {
        long length;
        try {
            length = Files.size(source);
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to size document source", e);
            throw new WhatsAppMediaException.Processing("failed to size document source", e);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "document transcode start: bytes={0}", length);
        provider.setMediaSize(length);
        if (isPdf(source)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "document is pdf, rendering page 0 thumbnail");
            applyPdfMetadata(provider, source);
        } else if (provider instanceof DocumentMessage document) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "document pass-through, no thumbnail");
            document.setMimetype(DEFAULT_MIMETYPE);
        }
        return new MediaPayload.OfPath(source, length, false);
    }

    /**
     * Parses the PDF file for its page count and a page-zero thumbnail and applies them to
     * {@code provider} when it is a {@link DocumentMessage}.
     *
     * <p>The thumbnail is rendered only when the document has at least one page; an empty
     * document yields a {@code null} thumbnail that is not applied. Providers that are not a
     * {@link DocumentMessage} receive no update from this method.
     *
     * @param provider the upload target receiving the metadata updates
     * @param source   the PDF file
     * @throws WhatsAppMediaException.Processing if PDF parsing or rendering fails
     */
    private static void applyPdfMetadata(MediaProvider provider, Path source)
            throws WhatsAppMediaException.Processing {
        byte[] thumbnail;
        int pageCount;
        try (var document = Loader.loadPDF(source.toFile())) {
            pageCount = document.getNumberOfPages();
            thumbnail = pageCount > 0 ? renderThumbnail(document) : null;
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to render pdf page 0 thumbnail", e);
            throw new WhatsAppMediaException.Processing("failed to render PDF page 0 thumbnail", e);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "pdf metadata: pages={0} thumbnailBytes={1}",
                    pageCount, thumbnail == null ? 0 : thumbnail.length);
        }
        if (provider instanceof DocumentMessage document) {
            document.setMimetype(PDF_MIMETYPE);
            document.setPageCount(pageCount);
            if (thumbnail != null) {
                document.setJpegThumbnail(thumbnail);
            }
        }
    }

    /**
     * Renders page 0 of the document, downscales it to a {@link #DOC_THUMB_MAX_EDGE} maximum
     * edge, and JPEG-encodes the result within the {@link #MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES}
     * budget.
     *
     * @param document the open PDF document
     * @return the encoded JPEG thumbnail bytes
     * @throws IOException if rendering or encoding fails
     */
    private static byte[] renderThumbnail(PDDocument document) throws IOException {
        var renderer = new PDFRenderer(document);
        var rendered = renderer.renderImageWithDPI(0, PDF_RENDER_DPI, ImageType.RGB);
        var resized = downscale(rendered, DOC_THUMB_MAX_EDGE);
        return encodeJpegUnderBudget(resized);
    }

    /**
     * Returns a copy of {@code source} scaled with bilinear interpolation so that the longer edge
     * is at most {@code maxEdge}.
     *
     * <p>The source aspect ratio is preserved, and each scaled edge is clamped to a minimum of one
     * pixel. A source image whose longer edge is already at or below {@code maxEdge} is returned
     * unchanged.
     *
     * @param source  the rendered page bitmap
     * @param maxEdge the upper bound on the longer edge of the result
     * @return the resized image
     */
    private static BufferedImage downscale(BufferedImage source, int maxEdge) {
        var srcW = source.getWidth();
        var srcH = source.getHeight();
        var longestEdge = Math.max(srcW, srcH);
        if (longestEdge <= maxEdge) {
            return source;
        }
        var scale = (double) maxEdge / longestEdge;
        var dstW = Math.max(1, (int) Math.round(srcW * scale));
        var dstH = Math.max(1, (int) Math.round(srcH * scale));
        var dst = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB);
        var g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, dstW, dstH, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /**
     * JPEG-encodes the given image, retrying with progressively lower quality until the output
     * fits inside {@link #MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES} or {@link #THUMBNAIL_QUALITY_FLOOR}
     * is reached.
     *
     * <p>Quality starts at {@link #THUMBNAIL_QUALITY_START} and is multiplied by
     * {@link #THUMBNAIL_QUALITY_STEP} on each pass. The first encode that fits the byte budget is
     * returned immediately; if no pass fits, the last encode produced at or above the quality
     * floor is returned regardless of its size.
     *
     * @param image the image to encode
     * @return the encoded JPEG bytes
     * @throws IOException if no JPEG writer is available or every encode attempt fails
     */
    private static byte[] encodeJpegUnderBudget(BufferedImage image) throws IOException {
        byte[] last = null;
        for (var quality = THUMBNAIL_QUALITY_START;
             quality >= THUMBNAIL_QUALITY_FLOOR;
             quality *= THUMBNAIL_QUALITY_STEP) {
            last = encodeJpeg(image, quality);
            if (last.length <= MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES) {
                return last;
            }
        }
        if (last == null) {
            throw new IOException("no JPEG writer available");
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "document thumbnail exceeds size budget: bytes={0} budgetBytes={1}",
                    last.length, MICRO_THUMBNAIL_MAX_FILE_SIZE_BYTES);
        }
        return last;
    }

    /**
     * Encodes the given image as a JPEG at the requested quality.
     *
     * @implNote
     * This implementation subclasses {@link ByteArrayOutputStream} to override
     * {@link ByteArrayOutputStream#toByteArray()} so it returns the backing array directly rather
     * than a defensive copy; the caller is the only reader and the stream is discarded immediately
     * after, so the copy is wasted work for a hot, per-page allocation.
     *
     * @param image   the image to encode
     * @param quality the JPEG quality in the JDK's {@code 0.0..1.0} range
     * @return the encoded JPEG bytes
     * @throws IOException if no JPEG writer is available or encoding fails
     */
    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("no JPEG writer available");
        }
        var writer = writers.next();
        try {
            var params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            var baos = new ByteArrayOutputStream() {
                @Override
                public byte[] toByteArray() {
                    return buf;
                }
            };
            try (var out = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, null), params);
            }
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    /**
     * Reports whether the given file begins with the PDF magic {@code "%PDF-"}.
     *
     * <p>Reads only the leading {@link #PDF_MAGIC} bytes from the channel; the document is never
     * loaded into memory. A file shorter than the magic prefix, or one whose leading bytes do not
     * match, is reported as not a PDF.
     *
     * @param source the document file
     * @return {@code true} when the source is identifiable as a PDF
     * @throws WhatsAppMediaException.Processing if the file cannot be probed
     */
    private static boolean isPdf(Path source) throws WhatsAppMediaException.Processing {
        try (var channel = FileChannel.open(source, StandardOpenOption.READ)) {
            if (channel.size() < PDF_MAGIC.length) {
                return false;
            }
            var probe = ByteBuffer.allocate(PDF_MAGIC.length);
            var total = 0;
            while (probe.hasRemaining()) {
                var n = channel.read(probe);
                if (n <= 0) {
                    return false;
                }
                total += n;
            }
            if (total < PDF_MAGIC.length) {
                return false;
            }
            var bytes = probe.array();
            for (var i = 0; i < PDF_MAGIC.length; i++) {
                if (bytes[i] != PDF_MAGIC[i]) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to probe document source", e);
            throw new WhatsAppMediaException.Processing("failed to probe document source", e);
        }
    }
}
