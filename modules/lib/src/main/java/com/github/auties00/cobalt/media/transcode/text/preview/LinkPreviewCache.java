package com.github.auties00.cobalt.media.transcode.text.preview;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessageBuilder;

import java.lang.System.Logger.Level;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches resolved link previews per session, keyed by URL, so repeated
 * sends of the same URL skip the rich fetch.
 *
 * <p>Two parallel stores are kept so that a preview resolved inside a
 * newsletter chat (where previewability of a URL is gated by server-side
 * rules) does not leak into ordinary 1:1 chats and vice versa; the store
 * is selected at call time by the {@code newsletterChat} flag. A URL
 * that resolved without producing a card is recorded under a negative
 * sentinel so subsequent lookups still short-circuit; callers detect the
 * sentinel through {@link #isNegative(ExtendedTextMessage)}.
 *
 * @implNote This implementation collapses both WA Web stores onto one
 * instance with two {@link ConcurrentMap} fields. The caches are
 * unbounded because session lifetimes are short and the JS counterpart
 * is also unbounded.
 */
@WhatsAppWebModule(moduleName = "WAWebLinkPreviewCache")
public final class LinkPreviewCache {
    /** The logger for {@link LinkPreviewCache}. */
    private static final System.Logger LOGGER = Log.get(LinkPreviewCache.class);

    /**
     * Holds the sentinel value stored when a URL was resolved but
     * produced no preview.
     *
     * @implNote This implementation uses a fresh empty
     * {@link ExtendedTextMessage} so identity comparison (via
     * {@link #isNegative(ExtendedTextMessage)}) distinguishes the
     * sentinel from any caller-supplied value.
     */
    private static final ExtendedTextMessage NEGATIVE = new ExtendedTextMessageBuilder().build();

    /**
     * Holds the cache for non-newsletter chats.
     *
     * <p>Selected when {@link #get(String, boolean)} or
     * {@link #put(String, boolean, ExtendedTextMessage)} is called with
     * {@code newsletterChat == false}.
     */
    private final ConcurrentMap<String, ExtendedTextMessage> regular;

    /**
     * Holds the cache for newsletter chats.
     *
     * <p>Selected when {@link #get(String, boolean)} or
     * {@link #put(String, boolean, ExtendedTextMessage)} is called with
     * {@code newsletterChat == true}.
     */
    private final ConcurrentMap<String, ExtendedTextMessage> newsletter;

    /**
     * Creates a fresh, empty cache pair.
     *
     * <p>One instance is created per
     * {@link com.github.auties00.cobalt.media.transcode.text.TextPipeline};
     * the cache is not shared across sessions.
     */
    public LinkPreviewCache() {
        this.regular = new ConcurrentHashMap<>();
        this.newsletter = new ConcurrentHashMap<>();
    }

    /**
     * Returns the cached preview for {@code url} when one is available.
     *
     * <p>Selects the newsletter or regular store from
     * {@code newsletterChat} and returns the stored value. The presence
     * of a value indicates the URL has been resolved at least once in
     * this session; a cached negative sentinel is returned unchanged so
     * the caller can detect "resolved but produced no preview" via
     * {@link #isNegative(ExtendedTextMessage)} and bypass attaching.
     * Returns {@link Optional#empty()} when the URL has not been
     * resolved yet.
     *
     * @param url            the URL whose preview is requested
     * @param newsletterChat whether the lookup is for a newsletter chat
     * @return the cached preview, or {@link Optional#empty()} when the
     *         URL has not been resolved yet
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewCache", exports = "getPreviewCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewCache", exports = "getNewsletterPreviewCache",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<ExtendedTextMessage> get(String url, boolean newsletterChat) {
        var pick = newsletterChat ? newsletter : regular;
        var cached = pick.get(url);
        if (cached == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview cache miss, newsletter={0}", newsletterChat);
            return Optional.empty();
        }
        if (cached == NEGATIVE) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview cache hit (negative), newsletter={0}", newsletterChat);
            return Optional.of(NEGATIVE);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview cache hit, newsletter={0}", newsletterChat);
        return Optional.of(cached);
    }

    /**
     * Returns whether {@code preview} is the negative sentinel.
     *
     * <p>The negative sentinel is the value stored by
     * {@link #put(String, boolean, ExtendedTextMessage)} for URLs that
     * resolved without producing a card. This distinguishes "resolved
     * but produced no preview" from "resolved successfully" on the value
     * returned by {@link #get(String, boolean)}.
     *
     * @param preview the preview returned from
     *                {@link #get(String, boolean)}
     * @return {@code true} when {@code preview} is the negative sentinel
     */
    public static boolean isNegative(ExtendedTextMessage preview) {
        return preview == NEGATIVE;
    }

    /**
     * Stores {@code preview} as the resolved value for {@code url}.
     *
     * <p>Selects the newsletter or regular store from
     * {@code newsletterChat} and records the value. A {@code null}
     * {@code preview} is stored as the negative sentinel so subsequent
     * lookups short-circuit instead of issuing the same network
     * round-trip again.
     *
     * @param url            the URL being cached
     * @param newsletterChat whether the resolution was for a newsletter
     *                       chat
     * @param preview        the preview to cache; {@code null} stores
     *                       the negative sentinel
     */
    public void put(String url, boolean newsletterChat, ExtendedTextMessage preview) {
        var pick = newsletterChat ? newsletter : regular;
        pick.put(url, preview != null ? preview : NEGATIVE);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview cached, newsletter={0}, negative={1}", newsletterChat, preview == null);
    }

    /**
     * Clears every cached entry across both stores.
     *
     * <p>Invoked when the user opts out of link previews or when the
     * owning session is recycled.
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewCache", exports = "clearPreviewCache",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewCache", exports = "clearNewsletterPreviewCache",
            adaptation = WhatsAppAdaptation.DIRECT)
    void clear() {
        regular.clear();
        newsletter.clear();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview cache cleared");
    }
}
