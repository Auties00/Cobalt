package com.github.auties00.cobalt.media.transcode.text.preview;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterLinkPreview;

import java.lang.System.Logger.Level;
import java.util.Base64;

/**
 * Builds preview cards for URLs sent inside a newsletter chat.
 *
 * <p>This resolver is called from
 * {@link com.github.auties00.cobalt.media.transcode.text.TextPipeline#run}
 * for newsletter recipients because the previewability of a URL inside a
 * channel is gated by server-side rules that cannot be evaluated
 * client-side, so the rich og-tag fetch must happen on the server. The
 * server response is then stamped onto the outgoing message.
 */
@WhatsAppWebModule(moduleName = "WAWebNewsletterFetchLinkPreviewAction")
public final class NewsletterPreviewResolver {
    /** The logger for {@link NewsletterPreviewResolver}. */
    private static final System.Logger LOGGER = Log.get(NewsletterPreviewResolver.class);

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private NewsletterPreviewResolver() {
        throw new AssertionError();
    }

    /**
     * Resolves the preview for a newsletter URL and stamps the result
     * onto {@code message}.
     *
     * <p>Issues the server link-preview request via
     * {@link LinkedWhatsAppClient#queryNewsletterLinkPreview(String)} and
     * writes every populated field of the response onto {@code message}
     * in place: {@code title}, {@code description}, {@code previewType},
     * {@code doNotPlayInline}, {@code jpegThumbnail} (the low-quality
     * placeholder), {@code thumbnailDirectPath} (the high-quality
     * download path), the matching SHA-256, and the
     * {@code thumbnailWidth} and {@code thumbnailHeight} advisory
     * dimensions. Returns {@code false} without mutating {@code message}
     * when {@code client}, {@code url}, or {@code message} is
     * {@code null}, or when the server round-trip fails or yields no
     * response.
     *
     * @implNote This implementation returns {@code false} on any
     * {@link RuntimeException} from the server round-trip so the
     * link-preview pipeline falls back to the minimal preview card,
     * leaving the recipient a clickable URL.
     *
     * @param client  the WhatsApp client used to query the server
     * @param url     the URL whose preview is requested
     * @param message the outgoing message to enrich; mutated in place
     * @return {@code true} when a preview was applied, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebNewsletterFetchLinkPreviewAction", exports = "fetchPlaintextLinkPreviewAction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean resolve(LinkedWhatsAppClient client, String url, ExtendedTextMessage message) {
        if (client == null || url == null || message == null) {
            return false;
        }
        var response = querySafely(client, url);
        if (response == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "newsletter link preview unavailable");
            return false;
        }
        response.title().ifPresent(message::setTitle);
        response.description().ifPresent(message::setDescription);
        message.setPreviewType(ExtendedTextMessage.PreviewType.NONE);
        message.setDoNotPlayInline(Boolean.TRUE);
        response.thumbnailData()
                .map(NewsletterPreviewResolver::decodeBase64)
                .ifPresent(message::setJpegThumbnail);
        response.thumbnailDirectPath().ifPresent(message::setThumbnailDirectPath);
        response.thumbnailHash()
                .map(NewsletterPreviewResolver::decodeBase64)
                .ifPresent(message::setThumbnailSha256);
        if (response.thumbnailWidth().isPresent()) {
            message.setThumbnailWidth(response.thumbnailWidth().getAsInt());
        }
        if (response.thumbnailHeight().isPresent()) {
            message.setThumbnailHeight(response.thumbnailHeight().getAsInt());
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "newsletter link preview resolved");
        return true;
    }

    /**
     * Issues the newsletter link-preview query and swallows any
     * transport-level error.
     *
     * <p>Returns {@code null} when the query fails so that
     * {@link #resolve(LinkedWhatsAppClient, String, ExtendedTextMessage)}
     * short-circuits and the link-preview pipeline falls back to the
     * minimal preview card.
     *
     * @param client the WhatsApp client used to query the server
     * @param url    the URL to resolve
     * @return the server response, or {@code null} when the query failed
     */
    private static NewsletterLinkPreview querySafely(
            LinkedWhatsAppClient client, String url) {
        try {
            return client.queryNewsletterLinkPreview(url).orElse(null);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "newsletter link preview query failed", e);
            return null;
        }
    }

    /**
     * Decodes a base64-encoded thumbnail string.
     *
     * <p>Returns {@code null} on malformed input so the link-preview
     * pipeline can fall back to a minimal preview instead of propagating
     * the {@link IllegalArgumentException}.
     *
     * @param base64 the base64 string
     * @return the decoded bytes, or {@code null} when the input is
     *         malformed
     */
    private static byte[] decodeBase64(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException malformed) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "newsletter preview thumbnail base64 decode failed");
            return null;
        }
    }
}
