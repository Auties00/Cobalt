package com.github.auties00.cobalt.media.transcode.text.preview;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.media.transcode.text.link.DeepLinkParser;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.BusinessVerifiedName;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessCatalogEntry;
import com.github.auties00.cobalt.wire.linked.business.catalog.BusinessReviewStatus;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;

import java.lang.System.Logger.Level;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Builds preview cards for {@code wa.me/c/...} catalog and
 * {@code wa.me/p/.../...} product deep links.
 *
 * <p>This resolver is invoked from
 * {@link com.github.auties00.cobalt.media.transcode.text.TextPipeline#run}
 * on the {@link DeepLinkParser.DeepLink.Catalog} and
 * {@link DeepLinkParser.DeepLink.Product} branches. It queries the
 * server-side business catalog, selects the entry to render, and stamps
 * the resulting card onto the outgoing message.
 */
@WhatsAppWebModule(moduleName = "WAWebBizLinkPreviewCatalogUtils")
public final class CatalogPreviewResolver {
    /** The logger for {@link CatalogPreviewResolver}. */
    private static final System.Logger LOGGER = Log.get(CatalogPreviewResolver.class);

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private CatalogPreviewResolver() {
        throw new AssertionError();
    }

    /**
     * Resolves the preview for a catalog or product URL and stamps the
     * result onto {@code message}.
     *
     * <p>When {@code productId} is non-{@code null} the preview renders
     * the single matching product (name, description, and price); when
     * {@code productId} is {@code null} the preview renders the
     * "View {owner}'s catalog" card backed by the first
     * approved-and-visible product. On a successful resolve the
     * {@code title}, {@code description}, {@code previewType},
     * {@code doNotPlayInline}, and {@code jpegThumbnail} fields are
     * written onto {@code message} in place. Returns {@code false}
     * without mutating {@code message} when {@code client},
     * {@code ownerJid}, or {@code message} is {@code null}, when
     * {@code ownerJid} is malformed, when the catalog query fails or is
     * empty, or when no eligible product is found.
     *
     * @implNote This implementation only considers entries whose
     * {@link BusinessReviewStatus} is
     * {@link BusinessReviewStatus#APPROVED} and which are not hidden,
     * mirroring the {@code reviewStatus === "APPROVED" && !isHidden} JS
     * filter on the product collection.
     *
     * @param client     the WhatsApp client used to query the
     *                   server-side catalog
     * @param ownerJid   the catalog owner JID, in
     *                   {@code <number>@s.whatsapp.net} form
     * @param productId  the optional product identifier; when
     *                   {@code null} the first approved-and-visible
     *                   product is selected
     * @param httpClient the HTTP client used to download the product
     *                   image
     * @param timeout    the per-download timeout
     * @param message    the outgoing message to enrich; mutated in place
     * @return {@code true} when a preview was applied, {@code false}
     *         otherwise
     */
    @WhatsAppWebExport(moduleName = "WAWebBizLinkPreviewCatalogUtils", exports = "getProductOrCatalogLinkPreview",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean resolve(LinkedWhatsAppClient client, String ownerJid, String productId,
                                  HttpClient httpClient, Duration timeout, ExtendedTextMessage message) {
        if (client == null || ownerJid == null || message == null) {
            return false;
        }
        Jid wid;
        try {
            wid = Jid.of(ownerJid);
        } catch (RuntimeException malformed) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "catalog preview owner jid malformed: {0}", new LogRedactable.User(ownerJid));
            return false;
        }
        var catalog = safeQueryCatalog(client, wid);
        if (catalog.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "catalog preview skipped, empty catalog for {0}", wid);
            return false;
        }
        var product = pickProduct(catalog, productId);
        if (product == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "catalog preview skipped, no eligible product for {0}", wid);
            return false;
        }
        var ownerName = ownerDisplayName(client, wid);
        var hasProductFocus = productId != null;
        var title = hasProductFocus
                ? "%s from %s on WhatsApp.".formatted(product.name().orElse(""), ownerName)
                : "View %s's catalog on WhatsApp".formatted(ownerName);
        var description = hasProductFocus
                ? buildProductDescription(product)
                : "Learn more about their products & services";
        message.setTitle(title);
        message.setDescription(description);
        message.setPreviewType(ExtendedTextMessage.PreviewType.NONE);
        message.setDoNotPlayInline(Boolean.TRUE);
        var imageUri = product.encryptedImage().orElse(null);
        if (imageUri != null) {
            var thumbnailBytes = PreviewThumbnailFetcher.download(httpClient, imageUri, timeout);
            if (thumbnailBytes != null) {
                message.setJpegThumbnail(thumbnailBytes);
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "catalog preview resolved for {0}, productFocus={1}", wid, hasProductFocus);
        return true;
    }

    /**
     * Issues the catalog query and swallows any transport-level error.
     *
     * <p>Returns the empty list when the query fails so that
     * {@link #resolve} short-circuits and the link-preview pipeline
     * falls back to the minimal preview card, leaving the recipient a
     * clickable URL.
     *
     * @param client the WhatsApp client used to query the server
     * @param wid    the catalog owner JID
     * @return the catalog entries, or the empty list when the query
     *         failed
     */
    private static List<BusinessCatalogEntry> safeQueryCatalog(LinkedWhatsAppClient client, Jid wid) {
        try {
            return client.queryBusinessCatalog(wid);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "catalog query failed for " + new LogRedactable.User(String.valueOf(wid)), e);
            return List.of();
        }
    }

    /**
     * Picks the product entry to render on the preview card.
     *
     * <p>When {@code productId} is supplied the matching entry is
     * picked; otherwise the first approved-and-visible entry in the
     * catalog is picked. Entries whose {@link BusinessReviewStatus} is
     * not {@link BusinessReviewStatus#APPROVED}, and entries that are
     * hidden, are skipped in both modes.
     *
     * @param catalog   the catalog entries
     * @param productId the product identifier, or {@code null}
     * @return the picked entry, or {@code null} when none is eligible
     */
    private static BusinessCatalogEntry pickProduct(List<BusinessCatalogEntry> catalog, String productId) {
        for (var entry : catalog) {
            if (productId != null && !productId.equals(entry.id())) {
                continue;
            }
            if (entry.reviewStatus().orElse(null) != BusinessReviewStatus.APPROVED) {
                continue;
            }
            if (entry.hidden()) {
                continue;
            }
            return entry;
        }
        return null;
    }

    /**
     * Resolves the display name of the catalog owner.
     *
     * <p>The cached verified-business name has priority, followed by the
     * cached contact's chosen name or short name, and finally a fresh
     * usync round-trip via
     * {@code LinkedWhatsAppClient.queryBusinessProfile(JidProvider)} which
     * surfaces the server-supplied {@link BusinessVerifiedName}. When
     * nothing resolves the JID's user portion is returned.
     *
     * @implNote This implementation swallows the {@link RuntimeException}
     * from the usync round-trip so the fallback to the JID user portion
     * still runs when the server is unreachable.
     *
     * @param client the WhatsApp client whose stores and usync are
     *               consulted
     * @param wid    the catalog owner JID
     * @return the display name, falling back to the JID's user portion
     *         when nothing else resolves
     */
    @WhatsAppWebExport(moduleName = "WAWebGetOrQueryUsyncInfoContactAction", exports = "getOrQueryUsyncInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String ownerDisplayName(LinkedWhatsAppClient client, Jid wid) {
        var contactName = client.store().contactStore().findContactByJid(wid)
                .flatMap(contact -> contact.chosenName().or(contact::shortName))
                .orElse(null);
        if (contactName != null && !contactName.isEmpty()) {
            return contactName;
        }
        var verified = client.store().contactStore().findVerifiedBusinessName(wid)
                .flatMap(BusinessVerifiedName::name)
                .orElse(null);
        if (verified != null && !verified.isEmpty()) {
            return verified;
        }
        try {
            client.queryBusinessProfile(wid);
            var refreshed = client.store().contactStore().findVerifiedBusinessName(wid)
                    .flatMap(BusinessVerifiedName::name)
                    .orElse(null);
            if (refreshed != null && !refreshed.isEmpty()) {
                return refreshed;
            }
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "business profile query failed for " + new LogRedactable.User(String.valueOf(wid)), e);
        }
        return wid.user();
    }

    /**
     * Builds the description rendered for a single-product preview.
     *
     * <p>When both description and price are available, both are
     * surfaced separated by a middle dot; otherwise the available half
     * is used; an empty string is returned when neither is present. A
     * price is considered present only when both a currency code and a
     * strictly positive amount are set.
     *
     * @param product the catalog entry
     * @return the description string, or the empty string when neither a
     *         description nor a price is available
     */
    private static String buildProductDescription(BusinessCatalogEntry product) {
        var description = product.description().orElse(null);
        var currency = product.currency().orElse(null);
        var price = product.price();
        if (description != null && currency != null && price > 0) {
            return description + " Â· " + formatAmount(currency, price);
        }
        if (description != null) {
            return description;
        }
        if (currency != null && price > 0) {
            return formatAmount(currency, price);
        }
        return "";
    }

    /**
     * Formats a price expressed in thousandths of the major currency
     * unit.
     *
     * <p>Divides {@code amount} by 1000 to recover the major-unit value
     * and renders it with two fractional digits followed by the currency
     * code.
     *
     * @implNote This implementation uses {@link String#format} with
     * {@link Locale#US} as the fallback formatter because no
     * locale-aware currency formatter (the JS counterpart delegates to
     * {@code Intl.NumberFormat}) is wired into the lib module.
     *
     * @param currency the ISO 4217 currency code
     * @param amount   the amount in thousandths of the major unit
     * @return the formatted amount
     */
    @WhatsAppWebExport(moduleName = "WAWebCurrencyUtils", exports = "formatAmount1000",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static String formatAmount(String currency, long amount) {
        var major = amount / 1000d;
        return String.format(Locale.US, "%.2f %s", major, currency);
    }
}
