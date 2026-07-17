package com.github.auties00.cobalt.media.transcode.text;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.media.transcode.text.link.DeepLinkParser;
import com.github.auties00.cobalt.media.transcode.text.link.Idn;
import com.github.auties00.cobalt.media.transcode.text.link.Linkify;
import com.github.auties00.cobalt.media.transcode.text.preview.CatalogPreviewResolver;
import com.github.auties00.cobalt.media.transcode.text.preview.GroupInvitePreviewResolver;
import com.github.auties00.cobalt.media.transcode.text.preview.LinkPreviewCache;
import com.github.auties00.cobalt.media.transcode.text.preview.NewsletterPreviewResolver;
import com.github.auties00.cobalt.media.transcode.text.preview.PreviewThumbnailFetcher;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadata.PaymentLinkHeader.PaymentLinkHeaderType;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadataPaymentLinkHeaderBuilder;
import com.github.auties00.cobalt.wire.linked.message.payment.PaymentLinkMetadataPaymentLinkProviderBuilder;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessageBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.WebcLinkPreviewDisplayEvent;
import com.github.auties00.cobalt.wire.wam.event.WebcLinkPreviewDisplayEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.WebcDisplayStatusType;
import it.auties.linkpreview.LinkPreview;
import it.auties.linkpreview.LinkPreviewMedia;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Orchestrates the rich link-preview pipeline that decorates outgoing
 * {@link ExtendedTextMessage} bodies with title, description, inline JPEG
 * thumbnail, and (when applicable) payment-link metadata.
 *
 * <p>On every outgoing message the pipeline walks a fixed sequence. It first
 * consults the user's privacy gate ({@code store.settingsStore().disableLinkPreviews()}) and
 * scans the body for the first detected URL via
 * {@link Linkify#findLink(String, boolean)}. It drops the URL when
 * {@link #isSuspicious(Jid, Linkify.Match)} flags an IDN homograph attempt or
 * when {@link #isPreviewable(Jid, String)} closes the newsletter allow-list.
 * It then dispatches the four WhatsApp deep-link shapes recognised by
 * {@link DeepLinkParser} (group invite, catalog, product, payment link) to
 * their per-source resolvers. Failing a deep-link match, newsletter chats are
 * routed through the server-mediated MEX branch
 * ({@link NewsletterPreviewResolver}) while ordinary chats issue an HTTP fetch
 * via the embedded {@link LinkPreview} library and extract the og-tags.
 *
 * <p>Because Cobalt is the primary device, the WA Web branch that delegates
 * the fetch to the paired phone via the {@code GENERATE_LINK_PREVIEW}
 * peer-data-operation does not apply; the direct fetch path is the canonical
 * implementation. Instances are owned by the caller and re-used across sends
 * so the per-session preview cache and HTTP client amortise their setup cost.
 *
 * @implNote
 * This implementation applies the {@link #isSuspicious(Jid, Linkify.Match)}
 * gate up front rather than after the deep-link branches as the JS does,
 * because the recognised deep-link patterns are restricted to canonical
 * WhatsApp domains that always pass the gate. The peer-data-operation branch
 * to the companion in {@code WAWebLinkPreviewChatAction} is not implemented
 * because Cobalt is the primary, not the companion.
 */
@WhatsAppWebModule(moduleName = "WAWebLinkPreviewChatAction")
@WhatsAppWebModule(moduleName = "WASuspiciousLinks")
@WhatsAppWebModule(moduleName = "WAWebCheckIfDomainIsPreviewable")
@WhatsAppWebModule(moduleName = "WAWebLinkPreviewUtils")
@WhatsAppWebModule(moduleName = "WAWebGenMinimalLinkPreviewChatAction")
@WhatsAppWebModule(moduleName = "WAWebWebcLinkPreviewDisplayWamEvent")
public final class TextPipeline {
    /** The logger for {@link TextPipeline}. */
    private static final System.Logger LOGGER = Log.get(TextPipeline.class);

    /**
     * Holds the country-code sentinel used when no phone country code can be
     * extracted from a JID.
     *
     * <p>This value is applied to LID, group, and newsletter JIDs. It matches
     * the {@code "ZZ"} literal that {@code WASuspiciousLinks} returns when the
     * JID type is {@code lidUser}.
     */
    private static final String LID_COUNTRY_CODE_SENTINEL = "ZZ";

    /**
     * Holds the owning {@link LinkedWhatsAppClient} used for store access, media
     * uploads, and newsletter and catalog MEX round-trips.
     */
    private final LinkedWhatsAppClient client;

    /**
     * Holds the {@link ABPropsService} consulted to gate the rich fetch
     * ({@link ABProp#WEB_LINK_PREVIEW_SYNC_ENABLED}) and derive the
     * per-request timeout ({@link ABProp#LINK_PREVIEW_WAIT_TIME}).
     */
    private final ABPropsService abPropsService;

    /**
     * Holds the shared {@link MediaConnectionService} used to upload the HQ
     * thumbnail to WhatsApp's CDN.
     */
    private final MediaConnectionService mediaConnectionService;

    /**
     * Holds the {@link WamService} used to commit the
     * {@link WebcLinkPreviewDisplayEvent} telemetry that records the HQ-request,
     * HQ-response, non-HQ-fallback, and terminal display status of each rich
     * link-preview generation.
     */
    private final WamService wamService;

    /**
     * Holds the per-session {@link LinkPreviewCache} keyed by URL.
     */
    private final LinkPreviewCache cache;

    /**
     * Holds the {@link HttpClient} used to download the inline JPEG thumbnail.
     *
     * <p>The {@link LinkPreview} library supplies its own client for HTML
     * fetches; this one is reserved for the thumbnail GET so the connect
     * timeout and redirect policy can be tuned independently.
     */
    private final HttpClient httpClient;

    /**
     * Creates a fresh pipeline bound to {@code client}.
     *
     * <p>This constructor seeds the per-session cache and the thumbnail HTTP
     * client but performs no network I/O. It is invoked once per session,
     * typically from the message-sending service.
     *
     * @param client                 the owning client
     * @param abPropsService         the AB-props service used for feature
     *                               gating and timeout configuration
     * @param mediaConnectionService the CDN credentials service used to
     *                               upload the HQ thumbnail
     * @param wamService             the telemetry service used to commit the
     *                               link-preview display outcome
     * @throws NullPointerException if any argument is {@code null}
     */
    public TextPipeline(LinkedWhatsAppClient client, ABPropsService abPropsService,
                        MediaConnectionService mediaConnectionService, WamService wamService) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.mediaConnectionService = Objects.requireNonNull(mediaConnectionService, "mediaConnectionService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.cache = new LinkPreviewCache();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /**
     * Returns the per-request HTTP timeout derived from
     * {@link ABProp#LINK_PREVIEW_WAIT_TIME}.
     *
     * <p>WA Web uses the {@code link_preview_wait_time} AB-prop to bound the
     * wait on the primary device's peer-data-operation response. Cobalt is the
     * primary, so the same value bounds the direct HTTP fetch instead.
     *
     * @implNote
     * This implementation clamps the prop value to a minimum of one second so
     * a misconfigured AB-prop never produces a zero-length timeout that would
     * cause an immediate request abort.
     *
     * @return the per-request timeout
     */
    @WhatsAppWebExport(moduleName = "WAWebABProps", exports = "getABPropConfigValue",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private Duration linkPreviewTimeout() {
        var seconds = abPropsService.getInt(ABProp.LINK_PREVIEW_WAIT_TIME);
        return Duration.ofSeconds(Math.max(1, seconds));
    }

    /**
     * Decorates {@code message} with a rich link preview derived from the
     * first URL detected in its body, when one is present and all privacy and
     * abuse gates pass.
     *
     * <p>This method is called by the message-sending pipeline on every
     * outgoing {@link ExtendedTextMessage} immediately before encryption and
     * dispatch. A {@code null} message, a disabled-previews privacy setting,
     * an empty body, a body without any URL, a suspicious IDN host, or a
     * newsletter chat with the news-URL-preview gate closed all short-circuit
     * before any I/O. The resolved preview (or the negative sentinel) is
     * stored on the per-session cache so subsequent sends of the same URL
     * re-use the prior outcome.
     *
     * @param chatJid the target chat JID; controls the newsletter branch and
     *                the country-code derivation for the suspicious-link gate
     * @param message the outgoing message; mutated in place
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewChatAction", exports = "getLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public void run(Jid chatJid, ExtendedTextMessage message) {
        if (message == null) {
            return;
        }
        if (client.store().settingsStore().disableLinkPreviews()) {
            return;
        }
        var text = message.text().orElse(null);
        if (text == null || text.isEmpty()) {
            return;
        }
        var match = Linkify.findLink(text, false).orElse(null);
        if (match == null) {
            return;
        }
        if (isSuspicious(chatJid, match)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview dropped, suspicious idn host");
            return;
        }
        if (!isPreviewable(chatJid, match.domain())) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview dropped, domain not previewable");
            return;
        }
        var newsletterChat = chatJid != null && chatJid.hasNewsletterServer();
        var cached = cache.get(match.href(), newsletterChat).orElse(null);
        if (cached != null) {
            var negative = LinkPreviewCache.isNegative(cached);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "link preview cache hit, newsletter={0}, negative={1}",
                        newsletterChat, negative);
            }
            if (!negative) {
                copyPreviewFields(cached, message);
            }
            return;
        }
        var command = DeepLinkParser.parse(client, abPropsService, match.href());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "link preview deep link dispatch, type={0}", command.getClass().getSimpleName());
        }
        var attached = false;
        switch (command) {
            case DeepLinkParser.DeepLink.GroupInvite groupInvite ->
                    attached = GroupInvitePreviewResolver.resolve(client, groupInvite.code(),
                            httpClient, linkPreviewTimeout(), message);
            case DeepLinkParser.DeepLink.Catalog catalog ->
                    attached = CatalogPreviewResolver.resolve(client, catalog.catalogOwnerJid(), null,
                            httpClient, linkPreviewTimeout(), message);
            case DeepLinkParser.DeepLink.Product product ->
                    attached = CatalogPreviewResolver.resolve(client, product.businessOwnerJid(),
                            product.productId(), httpClient, linkPreviewTimeout(), message);
            case DeepLinkParser.DeepLink.PaymentLink payment -> {
                if (payment.shouldDetectInComposer()) {
                    message.setPreviewType(ExtendedTextMessage.PreviewType.PAYMENT_LINKS);
                    attachPaymentMetadata(message, payment.psp());
                }
            }
            case DeepLinkParser.DeepLink.NotApplicable _ -> {
            }
        }
        if (!attached) {
            if (newsletterChat) {
                attached = NewsletterPreviewResolver.resolve(client, match.href(), message);
            } else {
                attachRichPreview(match, message);
                attached = true;
            }
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "link preview resolved, newsletter={0}, attached={1}", newsletterChat, attached);
        }
        cacheCurrentMessage(match.href(), newsletterChat, attached, message);
    }

    /**
     * Returns whether the URL detected at {@code match} should be dropped
     * before any preview is generated.
     *
     * <p>This method resolves the recipient and sender country codes from the
     * JIDs and delegates to {@link Idn#isSuspicious(String, String, String, List)}.
     * Returning {@code true} causes the URL to be left unenhanced; the
     * recipient still sees the literal URL but no clickable preview card.
     *
     * @implNote
     * This implementation passes an empty recipient-language list to the
     * heuristic because WA Web's {@code WAWebLinkify.fillSuspiciousCharacters}
     * also passes an empty array; the recipient locale is not propagated
     * through the suspicious-character path in the JS pipeline.
     *
     * @param chatJid the recipient chat JID; phone users contribute their
     *                country code, while LID, group, and newsletter chats use
     *                {@link #LID_COUNTRY_CODE_SENTINEL}
     * @param match   the URL match
     * @return {@code true} when the host is flagged as suspicious
     */
    @WhatsAppWebExport(moduleName = "WASuspiciousLinks", exports = "findSuspiciousCharacters",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean isSuspicious(Jid chatJid, Linkify.Match match) {
        if (match == null || match.domain() == null) {
            return false;
        }
        var host = match.domain().toLowerCase();
        var recipientCC = countryCodeFor(chatJid);
        var selfCC = countryCodeFor(client.store().accountStore().jid().orElse(null));
        return Idn.isSuspicious(host, recipientCC, selfCC, List.of());
    }

    /**
     * Returns the phone country code associated with {@code jid}, or the LID
     * sentinel when no phone country code can be extracted.
     *
     * <p>The country code is fed to
     * {@link Idn#isSuspicious(String, String, String, List)} where it acts as
     * a strong allow-list signal for confusable characters tied to that
     * region's natural orthographies.
     *
     * @param jid the JID
     * @return the country-code prefix, or {@link #LID_COUNTRY_CODE_SENTINEL}
     *         for non-phone JIDs
     */
    private static String countryCodeFor(Jid jid) {
        if (jid == null || !jid.hasUserServer()) {
            return LID_COUNTRY_CODE_SENTINEL;
        }
        var user = jid.user();
        if (user == null || user.isEmpty()) {
            return LID_COUNTRY_CODE_SENTINEL;
        }
        return Idn.countryCodeOf(user);
    }

    /**
     * Returns whether {@code domain} is allowed to render a rich link preview
     * for {@code chatJid}.
     *
     * <p>For non-newsletter chats the gate is open by design. For newsletter
     * chats the gate is controlled by
     * {@link ABProp#CHANNELS_HIDE_NEWS_URL_PREVIEW} and the server-side
     * {@code WAWebNewsletterIsDomainPreviewableAction.isDomainPreviewable}
     * allow-list.
     *
     * @implNote
     * This implementation closes the gate on any transport-level error from
     * the server allow-list query so a hostile or flapping server cannot leak
     * URL metadata into newsletter chats.
     *
     * @param chatJid the target chat JID; used to detect newsletter chats
     * @param domain  the link's host portion
     * @return {@code true} when previews are allowed
     */
    @WhatsAppWebExport(moduleName = "WAWebCheckIfDomainIsPreviewable", exports = "checkIfDomainIsPreviewable",
            adaptation = WhatsAppAdaptation.DIRECT)
    private boolean isPreviewable(Jid chatJid, String domain) {
        if (chatJid == null || !chatJid.hasNewsletterServer()) {
            return true;
        }
        if (!abPropsService.getBool(ABProp.CHANNELS_HIDE_NEWS_URL_PREVIEW)) {
            return true;
        }
        try {
            return domain != null && client.isNewsletterDomainPreviewable(domain);
        } catch (RuntimeException failure) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "newsletter domain previewable check failed", failure);
            return false;
        }
    }

    /**
     * Issues the HTTP fetch via {@link LinkPreview} and attaches the resulting
     * og-tag metadata to {@code message}.
     *
     * <p>This method runs when no deep-link branch matched (or the matched
     * branch declined to produce a card) and the chat is not a newsletter. It
     * falls back to {@link #attachMinimal(Linkify.Match, ExtendedTextMessage)}
     * on any failure so the recipient always sees a clickable preview. Every
     * terminal path commits one {@link WebcLinkPreviewDisplayEvent} through
     * {@link #emitLinkPreviewDisplay} recording the HQ outcome.
     *
     * @implNote
     * This implementation skips the rich fetch and applies the minimal card
     * directly when {@link ABProp#WEB_LINK_PREVIEW_SYNC_ENABLED} is off,
     * mirroring the JS
     * {@code !web_link_preview_sync_enabled || !PrimaryFeatures.linkPreview}
     * branch. The HQ telemetry fields are derived from the direct-fetch state:
     * an HQ request is counted once {@link LinkPreview#createPreview(URI)} is
     * attempted, an HQ response once the thumbnail upload stamps a
     * {@link ExtendedTextMessage#thumbnailDirectPath()}, and a non-HQ fallback
     * whenever the minimal card or the inline-only thumbnail path is taken.
     *
     * @param match   the detected URL
     * @param message the outgoing message to enrich
     */
    private void attachRichPreview(Linkify.Match match, ExtendedTextMessage message) {
        if (!abPropsService.getBool(ABProp.WEB_LINK_PREVIEW_SYNC_ENABLED)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview rich fetch skipped, sync disabled");
            attachMinimal(match, message);
            emitLinkPreviewDisplay(WebcDisplayStatusType.SHOWED_PREVIEW_TO_USER, false, false, true);
            return;
        }
        URI uri;
        try {
            uri = URI.create(match.href());
        } catch (IllegalArgumentException malformed) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "link preview url malformed", malformed);
            attachMinimal(match, message);
            emitLinkPreviewDisplay(WebcDisplayStatusType.PREVIEW_MALFORMED, false, false, true);
            return;
        }
        var preview = LinkPreview.createPreview(uri).orElse(null);
        if (preview == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview not found via http fetch");
            attachMinimal(match, message);
            emitLinkPreviewDisplay(WebcDisplayStatusType.PREVIEW_NOT_FOUND, true, false, true);
            return;
        }
        var thumbnailBytes = downloadThumbnail(preview.images());
        message.setTitle(preview.title());
        message.setDescription(preview.siteDescription());
        message.setPreviewType(resolvePreviewType(preview.mediaType(), message.previewType().orElse(null)));
        message.setDoNotPlayInline(Boolean.TRUE);
        uploadThumbnail(message, thumbnailBytes);
        var respondedHq = message.thumbnailDirectPath().isPresent();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "link preview rich fetch complete, hqThumbnail={0}", respondedHq);
        emitLinkPreviewDisplay(WebcDisplayStatusType.SHOWED_PREVIEW_TO_USER, true, respondedHq, !respondedHq);
    }

    /**
     * Commits one {@link WebcLinkPreviewDisplayEvent} recording the outcome of a
     * rich link-preview generation.
     *
     * <p>This helper centralises the WAM emit so every terminal branch of
     * {@link #attachRichPreview(Linkify.Match, ExtendedTextMessage)} reports a
     * consistent tuple of HQ-request, HQ-response, non-HQ-fallback, and terminal
     * display status.
     *
     * @implNote
     * The WA Web counterpart fires this beacon on the companion when the paired
     * phone answers the {@code GENERATE_LINK_PREVIEW} peer-data-operation.
     * Cobalt is the primary, so the beacon reports the outcome of the direct
     * HTTP fetch instead; the three boolean fields are mutually consistent by
     * construction (an HQ response implies no non-HQ fallback).
     *
     * @param status              the terminal display status
     * @param didRequestHq        whether a high-quality preview fetch was
     *                            attempted
     * @param didRespondHqPreview whether the high-quality thumbnail was uploaded
     *                            to the CDN
     * @param didFallbackNonHq    whether the non-HQ (minimal or inline-only)
     *                            card was used
     */
    @WhatsAppWebExport(moduleName = "WAWebWebcLinkPreviewDisplayWamEvent", exports = "WebcLinkPreviewDisplay",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void emitLinkPreviewDisplay(WebcDisplayStatusType status, boolean didRequestHq,
                                        boolean didRespondHqPreview, boolean didFallbackNonHq) {
        wamService.commit(new WebcLinkPreviewDisplayEventBuilder()
                .webcDisplayStatus(status)
                .didRequestHq(didRequestHq)
                .didRespondHqPreview(didRespondHqPreview)
                .didFallbackNonHq(didFallbackNonHq)
                .build());
    }

    /**
     * Uploads the JPEG thumbnail as the HQ variant for {@code message} and
     * stamps the inline placeholder.
     *
     * <p>On a successful upload the CDN coordinates ({@code thumbnailDirectPath},
     * {@code thumbnailEncSha256}, {@code mediaKey}, {@code mediaKeyTimestamp})
     * and the {@code jpegThumbnail} placeholder land directly on
     * {@code message} so recipients can download the HQ image on demand while
     * the inline LQ JPEG renders immediately.
     *
     * @implNote
     * This implementation gracefully degrades on any failure (no media
     * connection, network error, interruption) by stamping the inline bytes
     * and the plaintext SHA-256 onto {@code message} so the receiver still
     * renders the LQ preview. The HQ upload goes directly through
     * {@link MediaConnectionService#upload(com.github.auties00.cobalt.wire.linked.media.MediaProvider, MediaPayload)}
     * because the JPEG payload already matches the {@code WAWebLinkPreviewUtils}
     * wire format; bypassing the transcoder avoids a redundant decode and
     * encode round-trip.
     *
     * @param message        the outgoing message to enrich
     * @param thumbnailBytes the JPEG bytes to embed and upload, or
     *                       {@code null} when no thumbnail was fetched
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewUtils", exports = "getThumbnailDetails",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private void uploadThumbnail(ExtendedTextMessage message, byte[] thumbnailBytes) {
        if (thumbnailBytes == null) {
            return;
        }
        message.setJpegThumbnail(thumbnailBytes);
        try {
            try (var payload = new MediaPayload.OfBytes(thumbnailBytes)) {
                if (mediaConnectionService.upload(message, payload)) {
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "link preview thumbnail uploaded, bytes={0}", thumbnailBytes.length);
                    }
                    return;
                }
            }
        } catch (InterruptedException interrupted) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "link preview thumbnail upload interrupted", interrupted);
            Thread.currentThread().interrupt();
        } catch (Throwable failure) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "link preview thumbnail upload failed, falling back to inline only", failure);
        }
        message.setThumbnailSha256(sha256(thumbnailBytes));
    }

    /**
     * Attaches the minimal
     * {@code {title=domain, description=url, doNotPlayInline=true}} fallback
     * card to {@code message}.
     *
     * <p>This method is invoked by
     * {@link #attachRichPreview(Linkify.Match, ExtendedTextMessage)} on every
     * failure path so the recipient always sees a clickable preview even when
     * the rich fetch could not produce a card. The preview type is left
     * unchanged when already set and otherwise defaults to
     * {@link ExtendedTextMessage.PreviewType#NONE}.
     *
     * @param match   the detected URL
     * @param message the outgoing message to enrich
     */
    @WhatsAppWebExport(moduleName = "WAWebGenMinimalLinkPreviewChatAction", exports = "genMinimalLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static void attachMinimal(Linkify.Match match, ExtendedTextMessage message) {
        message.setTitle(match.domain());
        message.setDescription(match.url());
        if (message.previewType().isEmpty()) {
            message.setPreviewType(ExtendedTextMessage.PreviewType.NONE);
        }
        message.setDoNotPlayInline(Boolean.TRUE);
    }

    /**
     * Materialises a {@code PaymentLinkMetadata} on {@code message} for the
     * matched payment-service-provider.
     *
     * <p>A {@code PaymentLinkProvider} carrying the PSP and a
     * {@code PaymentLinkHeader} of type
     * {@link PaymentLinkHeaderType#LINK_PREVIEW} are attached so the recipient
     * renders the payment card. A {@code null} PSP is a no-op.
     *
     * @implNote
     * The payment-link header carries no button text because button text in
     * WA Web is server-localised and only attached by the business composer,
     * never by URL detection.
     *
     * @param message the outgoing message to enrich
     * @param psp     the payment-service-provider label matched from
     *                {@link ABProp#SMB_PAYMENT_LINKS_URL_REGEX_LIST}
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewUtils", exports = "genLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static void attachPaymentMetadata(ExtendedTextMessage message, String psp) {
        if (psp == null) {
            return;
        }
        var provider = new PaymentLinkMetadataPaymentLinkProviderBuilder()
                .paramsJson(psp)
                .build();
        var header = new PaymentLinkMetadataPaymentLinkHeaderBuilder()
                .headerType(PaymentLinkHeaderType.LINK_PREVIEW)
                .build();
        var metadata = new PaymentLinkMetadataBuilder()
                .header(header)
                .provider(provider)
                .build();
        message.setPaymentLinkMetadata(metadata);
    }

    /**
     * Resolves the wire-format preview type from the page's og-type hint,
     * preserving any baseline the deep-link dispatch already selected.
     *
     * <p>This method maps {@code "video"} pages to
     * {@link ExtendedTextMessage.PreviewType#VIDEO} and otherwise preserves
     * the baseline ({@link ExtendedTextMessage.PreviewType#NONE} for plain
     * URLs, {@link ExtendedTextMessage.PreviewType#PAYMENT_LINKS} for
     * payment-link cards).
     *
     * @param mediaType the og-type or og-medium value
     * @param baseline  the baseline preview-type already set on the message by
     *                  an earlier branch
     * @return the resolved preview type
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkPreviewChatAction", exports = "getLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static ExtendedTextMessage.PreviewType resolvePreviewType(String mediaType,
                                                                      ExtendedTextMessage.PreviewType baseline) {
        if (baseline != null && baseline != ExtendedTextMessage.PreviewType.NONE) {
            return baseline;
        }
        if (mediaType != null && mediaType.toLowerCase().startsWith("video")) {
            return ExtendedTextMessage.PreviewType.VIDEO;
        }
        return ExtendedTextMessage.PreviewType.NONE;
    }

    /**
     * Downloads the first image referenced by the og-image set and returns the
     * raw bytes.
     *
     * <p>The download is routed through
     * {@link PreviewThumbnailFetcher#download(HttpClient, URI, Duration)} so
     * the payload is size-capped and resized to the inline thumbnail format
     * when {@code java.desktop} is on the runtime path.
     *
     * @param images the image set returned by the linkpreview library
     * @return the raw bytes, or {@code null} when the set is empty or the
     *         download failed
     */
    private byte[] downloadThumbnail(Set<LinkPreviewMedia> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        var firstImage = images.iterator().next();
        if (firstImage == null || firstImage.uri() == null) {
            return null;
        }
        return PreviewThumbnailFetcher.download(httpClient, firstImage.uri(), linkPreviewTimeout());
    }

    /**
     * Computes the SHA-256 digest of {@code bytes}.
     *
     * <p>The digest is used as the plaintext value stamped onto
     * {@link ExtendedTextMessage#thumbnailSha256()} when the HQ-upload path
     * fails and only the inline JPEG is available.
     *
     * @implNote
     * This implementation returns {@code null} when SHA-256 is unavailable in
     * the JCA provider chain, which is effectively impossible on any standard
     * JVM.
     *
     * @param bytes the bytes to hash
     * @return the SHA-256 digest, or {@code null} when SHA-256 is unavailable
     */
    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            return null;
        }
    }

    /**
     * Stores the resolved preview in the cache so subsequent sends of the same
     * URL short-circuit.
     *
     * <p>A failed resolution stores the negative sentinel; a successful one
     * stores a snapshot built from the just-decorated message so the cache
     * value carries only preview fields, not the message body.
     *
     * @implNote
     * This implementation skips caching for newsletter previews when no HQ
     * thumbnail was received; subsequent sends re-query the server because a
     * LQ-only result is considered transient, matching the JS guard
     * {@code i.thumbnailHQ != null && r.set(t.url, l)}.
     *
     * @param url            the URL key
     * @param newsletterChat whether the resolution happened in a newsletter
     *                       chat
     * @param attached       whether a preview was attached
     * @param message        the message that just had the preview attached
     */
    private void cacheCurrentMessage(String url, boolean newsletterChat, boolean attached, ExtendedTextMessage message) {
        if (!attached) {
            cache.put(url, newsletterChat, null);
            return;
        }
        if (newsletterChat && message.thumbnailDirectPath().isEmpty()) {
            return;
        }
        var snapshotBuilder = new ExtendedTextMessageBuilder()
                .title(message.title().orElse(null))
                .description(message.description().orElse(null))
                .previewType(message.previewType().orElse(null))
                .doNotPlayInline(message.doNotPlayInline())
                .jpegThumbnail(message.jpegThumbnail().orElse(null))
                .thumbnailDirectPath(message.thumbnailDirectPath().orElse(null))
                .thumbnailSha256(message.thumbnailSha256().orElse(null))
                .thumbnailEncSha256(message.thumbnailEncSha256().orElse(null))
                .mediaKey(message.mediaKey().orElse(null))
                .mediaKeyTimestamp(message.mediaKeyTimestamp().orElse(null))
                .paymentLinkMetadata(message.paymentLinkMetadata().orElse(null));
        message.thumbnailWidth().ifPresent(snapshotBuilder::thumbnailWidth);
        message.thumbnailHeight().ifPresent(snapshotBuilder::thumbnailHeight);
        cache.put(url, newsletterChat, snapshotBuilder.build());
    }

    /**
     * Copies the preview-bearing fields of a cached snapshot onto a fresh
     * outgoing message.
     *
     * <p>This method runs from
     * {@link #run(Jid, ExtendedTextMessage)} on a cache hit so two messages
     * sharing the same URL render the same preview without re-fetching; only
     * preview fields move across, not the body or mentions of the cached
     * snapshot.
     *
     * @param cached  the cached snapshot
     * @param message the outgoing message to enrich
     */
    private static void copyPreviewFields(ExtendedTextMessage cached, ExtendedTextMessage message) {
        cached.title().ifPresent(message::setTitle);
        cached.description().ifPresent(message::setDescription);
        cached.previewType().ifPresent(message::setPreviewType);
        message.setDoNotPlayInline(cached.doNotPlayInline());
        cached.jpegThumbnail().ifPresent(message::setJpegThumbnail);
        cached.thumbnailDirectPath().ifPresent(message::setThumbnailDirectPath);
        cached.thumbnailSha256().ifPresent(message::setThumbnailSha256);
        cached.thumbnailEncSha256().ifPresent(message::setThumbnailEncSha256);
        cached.mediaKey().ifPresent(message::setMediaKey);
        cached.mediaKeyTimestamp().ifPresent(message::setMediaKeyTimestamp);
        cached.thumbnailWidth().ifPresent(message::setThumbnailWidth);
        cached.thumbnailHeight().ifPresent(message::setThumbnailHeight);
        cached.paymentLinkMetadata().ifPresent(message::setPaymentLinkMetadata);
    }
}
