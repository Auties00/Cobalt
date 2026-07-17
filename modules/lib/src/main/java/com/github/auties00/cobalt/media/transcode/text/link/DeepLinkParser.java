package com.github.auties00.cobalt.media.transcode.text.link;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Recognises the deep-link shapes consumed by the link-preview pipeline.
 *
 * <p>Recognises the four link-preview-relevant deep-link families: group invites
 * ({@code chat.whatsapp.com/...}), business catalogs ({@code wa.me/c/...}), business
 * products ({@code wa.me/p/.../...}), and payment links ({@code wa.me/pay/...}). The
 * relevant identifiers are stripped out of the URL so the per-source resolvers can fetch
 * the right data. Any URL that none of the four families matches resolves to
 * {@link DeepLink.NotApplicable#INSTANCE}, leaving the regular fetch path to run.
 *
 * @implNote
 * This implementation is intentionally narrower than the WA Web parser, which also
 * recognises around twenty other deep-link shapes (open-chat, call-link, message-yourself,
 * registration campaigns, sticker pack, and so on); those shapes fall through the JS
 * pipeline as a message-send or new-chat command routed by a different module and never
 * produce a preview card, so collapsing everything outside the four supported shapes to
 * {@link DeepLink.NotApplicable#INSTANCE} matches the JS outcome on this code path.
 */
@WhatsAppWebModule(moduleName = "WAWebApiParse")
@WhatsAppWebModule(moduleName = "WAWebPaymentLinkUrlMetaData")
@WhatsAppWebModule(moduleName = "WAWebMobilePlatforms")
public final class DeepLinkParser {
    /** The logger for {@link DeepLinkParser}. */
    private static final System.Logger LOGGER = Log.get(DeepLinkParser.class);

    /**
     * Caches parsed payment-link regex maps keyed by the raw AB-prop value.
     *
     * <p>Backs {@link #paymentLink(LinkedWhatsAppClient, ABPropsService, String)} so a single
     * AB-prop refresh costs one parse, not one parse per outgoing message.
     *
     * @implNote
     * This implementation uses a {@link ConcurrentHashMap} because outgoing messages run
     * on virtual threads and several sends may race on the first parse after an AB-prop
     * refresh.
     */
    private static final Map<String, Map<Pattern, String>> PAYMENT_REGEX_CACHE = new ConcurrentHashMap<>();

    /**
     * Holds the web-origin prefix accepted by the catalog, product, and group patterns.
     *
     * <p>Covers the {@code whatsapp.com}, {@code web.whatsapp.com}, and
     * {@code chat.whatsapp.com} hosts under either scheme.
     */
    private static final String WEB_ORIGIN = "https?://(?:web\\.|chat\\.)?whatsapp\\.com";

    /**
     * Holds the optional locale-tag path segment that may follow the origin.
     *
     * <p>Matches {@code /xx} or {@code /xx-yy} where {@code xx} and {@code yy} are
     * two-letter language tags.
     */
    private static final String OPTIONAL_PATH_PART = "(?:/(?:[a-z]{2}|[a-z]{2}-[a-z]{2}))?";

    /**
     * Matches {@code chat.whatsapp.com/...?code=...} accept-style group invites.
     *
     * <p>The second capture group carries the invite code.
     */
    private static final Pattern ACCEPT_GROUP_INVITE = Pattern.compile(
            "^" + WEB_ORIGIN + OPTIONAL_PATH_PART + "/accept/?\\?code=(\\w+)(?:&.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code chat.whatsapp.com/invite/<code>} URLs.
     *
     * <p>Capture group one carries the invite code.
     */
    private static final Pattern INVITE_GROUP_INVITE = Pattern.compile(
            "^https?://chat\\.whatsapp\\.com/invite/(\\w+)(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code chat.whatsapp.com/<code>} short invite URLs.
     *
     * <p>Capture group one carries the invite code.
     */
    private static final Pattern SHORT_GROUP_INVITE = Pattern.compile(
            "^https?://chat\\.whatsapp\\.com/(\\w+)(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code whatsapp://chat?code=<code>} URLs.
     *
     * <p>Capture group one carries the invite code.
     */
    private static final Pattern SCHEME_GROUP_INVITE = Pattern.compile(
            "^whatsapp://chat/?\\?code=(\\w+)(?:&.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code wa.me/c/<jid>} catalog URLs.
     *
     * <p>Capture group one carries the catalog owner's phone number.
     */
    private static final Pattern WAME_CATALOG = Pattern.compile(
            "^https?://wa\\.me/c/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code whatsapp://catalog/<jid>} URLs.
     *
     * <p>Capture group one carries the catalog owner's phone number.
     */
    private static final Pattern SCHEME_CATALOG = Pattern.compile(
            "^whatsapp://catalog/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches catalog URLs hosted on the {@code whatsapp.com} origin
     * ({@code https://whatsapp.com/catalog/<jid>}).
     *
     * <p>Both the bare-host and locale-prefixed shapes collapse to this single pattern
     * because both resolve to the same {@link DeepLink.Catalog}.
     */
    private static final Pattern WEB_CATALOG = Pattern.compile(
            "^" + WEB_ORIGIN + OPTIONAL_PATH_PART + "/catalog/([0-9]{0,20})(/?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code wa.me/p/<productId>/<businessJid>} URLs with numeric product ids.
     *
     * <p>Capture group one carries the product id, group two the business owner's phone
     * number.
     */
    private static final Pattern WAME_PRODUCT = Pattern.compile(
            "^https?://wa\\.me/p/([0-9]{0,20})/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code whatsapp://product/<productId>/<businessJid>} URLs.
     *
     * <p>Capture group one carries the product id, group two the business owner's phone
     * number.
     */
    private static final Pattern SCHEME_PRODUCT = Pattern.compile(
            "^whatsapp://product/([0-9]{0,20})/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches product URLs hosted on the {@code whatsapp.com} origin
     * ({@code https://whatsapp.com/product/<productId>/<businessJid>}).
     *
     * <p>Capture group one carries the product id, group two the business owner's phone
     * number.
     */
    private static final Pattern WEB_PRODUCT = Pattern.compile(
            "^" + WEB_ORIGIN + OPTIONAL_PATH_PART + "/product/([0-9]{0,20})/([0-9]{0,20})(/?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code wa.me/p/<retailerId>/<businessJid>} URLs whose product identifier is
     * a free-form retailer id.
     *
     * <p>Permits any non-slash characters in capture group one so non-numeric retailer
     * SKUs match; group two carries the business owner's phone number.
     */
    private static final Pattern WAME_PRODUCT_RETAILER = Pattern.compile(
            "^https?://wa\\.me/p/([^/]{0,200})/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches {@code whatsapp://product/<retailerId>/<businessJid>} URLs whose product
     * identifier is a free-form retailer id.
     *
     * <p>Permits any non-slash characters in capture group one; group two carries the
     * business owner's phone number.
     */
    private static final Pattern SCHEME_PRODUCT_RETAILER = Pattern.compile(
            "^whatsapp://product/([^/]{0,200})/([0-9]{0,20})(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Matches retailer-product URLs hosted on the {@code whatsapp.com} origin.
     *
     * <p>Permits any non-slash characters in capture group one; group two carries the
     * business owner's phone number.
     */
    private static final Pattern WEB_PRODUCT_RETAILER = Pattern.compile(
            "^" + WEB_ORIGIN + OPTIONAL_PATH_PART + "/product/([^/]{0,200})/([0-9]{0,20})(/?.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private DeepLinkParser() {
        throw new AssertionError();
    }

    /**
     * Recognises the deep-link shape of {@code url}.
     *
     * <p>Tries the group-invite, catalog, and product shapes in order, then falls back to
     * {@link #paymentLink(LinkedWhatsAppClient, ABPropsService, String)}. Returns
     * {@link DeepLink.NotApplicable#INSTANCE} for a {@code null} {@code url} and for any
     * URL none of the four supported shapes matches, allowing the link-preview pipeline to
     * short-circuit the og-tag scrape on the recognised branches and run the regular fetch
     * otherwise.
     *
     * @implNote
     * This implementation falls back to {@link #paymentLink(LinkedWhatsAppClient, ABPropsService, String)}
     * last so the recognised payment-service-provider set tracks the
     * {@link ABProp#SMB_PAYMENT_LINKS_URL_REGEX_LIST} AB-prop rather than being hard-coded.
     *
     * @param client         the WhatsApp client used to derive SMB status for the
     *                       payment-link branch
     * @param abPropsService the AB-props service consulted by the payment-link branch
     * @param url            the URL to inspect
     * @return the parsed deep-link, or {@link DeepLink.NotApplicable#INSTANCE}
     */
    @WhatsAppWebExport(moduleName = "WAWebApiParse", exports = "parseAPICmd",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static DeepLink parse(LinkedWhatsAppClient client, ABPropsService abPropsService, String url) {
        if (url == null) {
            return DeepLink.NotApplicable.INSTANCE;
        }
        var inviteAccept = ACCEPT_GROUP_INVITE.matcher(url);
        if (inviteAccept.matches()) {
            return new DeepLink.GroupInvite(inviteAccept.group(2));
        }
        var inviteFull = INVITE_GROUP_INVITE.matcher(url);
        if (inviteFull.matches()) {
            return new DeepLink.GroupInvite(inviteFull.group(1));
        }
        var inviteShort = SHORT_GROUP_INVITE.matcher(url);
        if (inviteShort.matches()) {
            return new DeepLink.GroupInvite(inviteShort.group(1));
        }
        var inviteScheme = SCHEME_GROUP_INVITE.matcher(url);
        if (inviteScheme.matches()) {
            return new DeepLink.GroupInvite(inviteScheme.group(1));
        }
        for (var catalogPattern : new Pattern[]{WAME_CATALOG, SCHEME_CATALOG, WEB_CATALOG}) {
            var matcher = catalogPattern.matcher(url);
            if (matcher.matches()) {
                return new DeepLink.Catalog(matcher.group(1) + "@s.whatsapp.net");
            }
        }
        for (var productPattern : new Pattern[]{WAME_PRODUCT, SCHEME_PRODUCT, WEB_PRODUCT,
                WAME_PRODUCT_RETAILER, SCHEME_PRODUCT_RETAILER, WEB_PRODUCT_RETAILER}) {
            var matcher = productPattern.matcher(url);
            if (matcher.matches()) {
                return new DeepLink.Product(matcher.group(1), matcher.group(2) + "@s.whatsapp.net");
            }
        }
        var payment = paymentLink(client, abPropsService, url);
        if (payment != null) {
            return payment;
        }
        return DeepLink.NotApplicable.INSTANCE;
    }

    /**
     * Returns the {@link DeepLink.PaymentLink} matching {@code url} against the
     * AB-prop-driven payment-service-provider regex map.
     *
     * <p>Reads {@link ABProp#SMB_PAYMENT_LINKS_URL_REGEX_LIST}, iterates its entries in
     * JSON-declaration order so the first matching entry wins, and returns the matched
     * payment-link. Returns {@code null} when any argument is {@code null}, when the
     * AB-prop is unset or empty, or when no configured regex matches. The
     * {@link DeepLink.PaymentLink#shouldDetectInComposer()} flag on the returned variant is
     * {@code true} only on SMB clients (Android-Business or iOS-Business), as decided by
     * {@link #isSmb(LinkedWhatsAppClient)}.
     *
     * @param client         the WhatsApp client used to determine SMB status from the
     *                       local device platform
     * @param abPropsService the AB-props service consulted for
     *                       {@link ABProp#SMB_PAYMENT_LINKS_URL_REGEX_LIST}
     * @param url            the URL to inspect
     * @return the matched payment-link deep-link, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebPaymentLinkUrlMetaData", exports = "getPaymentLinkUrlMetaData",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static DeepLink.PaymentLink paymentLink(LinkedWhatsAppClient client, ABPropsService abPropsService, String url) {
        if (client == null || abPropsService == null || url == null) {
            return null;
        }
        var raw = abPropsService.getString(ABProp.SMB_PAYMENT_LINKS_URL_REGEX_LIST);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        var regexMap = PAYMENT_REGEX_CACHE.computeIfAbsent(raw, DeepLinkParser::parsePaymentRegex);
        if (regexMap.isEmpty()) {
            return null;
        }
        var smb = isSmb(client);
        for (var entry : regexMap.entrySet()) {
            if (entry.getKey().matcher(url).find()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment link matched, smb={0}", smb);
                return new DeepLink.PaymentLink(entry.getValue(), smb);
            }
        }
        return null;
    }

    /**
     * Parses the AB-prop JSON value into a regex-to-payment-service-provider-label map.
     *
     * <p>Invoked from {@link #paymentLink(LinkedWhatsAppClient, ABPropsService, String)} via
     * {@link Map#computeIfAbsent(Object, java.util.function.Function)} so the parse happens
     * at most once per distinct AB-prop value. Returns an empty map for a {@code null},
     * empty, or malformed top-level JSON object.
     *
     * @implNote
     * This implementation silently skips individual malformed entries (broken regexes,
     * non-string values) to mirror the JS, which iterates the parsed object with a
     * {@code for ... in} loop that ignores any entry whose value is not a string and only
     * raises a bad regex at match time, not at parse time.
     *
     * @param raw the AB-prop JSON value
     * @return the parsed map, possibly empty
     */
    private static Map<Pattern, String> parsePaymentRegex(String raw) {
        try {
            var json = JSON.parseObject(raw);
            if (json == null || json.isEmpty()) {
                return Map.of();
            }
            var out = new LinkedHashMap<Pattern, String>();
            for (var entry : json.entrySet()) {
                if (!(entry.getValue() instanceof String psp)) {
                    continue;
                }
                try {
                    out.put(Pattern.compile(entry.getKey()), psp);
                } catch (PatternSyntaxException invalid) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "payment link regex entry invalid, skipped", invalid);
                }
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "payment link regex list parsed, entries={0}", out.size());
            return Map.copyOf(out);
        } catch (RuntimeException malformed) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "payment link regex list malformed", malformed);
            return Map.of();
        }
    }

    /**
     * Returns whether the local client runs the SMB (WhatsApp Business) variant.
     *
     * <p>Returns {@code true} when the local device platform is one of the
     * {@code _BUSINESS} variants ({@link ClientPlatformType#ANDROID_BUSINESS} or
     * {@link ClientPlatformType#IOS_BUSINESS}), and {@code false} when the device is unset
     * or runs a consumer platform. The flag decides whether the payment-link card
     * materialises in the composer; non-SMB clients still see the URL but no preview card.
     *
     * @param client the WhatsApp client
     * @return {@code true} when the device platform is one of the {@code _BUSINESS}
     *         variants
     */
    @WhatsAppWebExport(moduleName = "WAWebMobilePlatforms", exports = "isSMB",
            adaptation = WhatsAppAdaptation.ADAPTED)
    private static boolean isSmb(LinkedWhatsAppClient client) {
        var device = client.store().accountStore().device();
        if (device == null) {
            return false;
        }
        var platform = device.platform();
        return platform == ClientPlatformType.ANDROID_BUSINESS
                || platform == ClientPlatformType.IOS_BUSINESS;
    }

    /**
     * Represents the tagged union of deep-link shapes the link-preview pipeline dispatches
     * on.
     *
     * <p>The four concrete variants map to the four link-preview-relevant deep-link result
     * types; {@link NotApplicable} covers every other URL. The pipeline switches on the
     * variant to pick the resolver, comparing {@link NotApplicable#INSTANCE} by identity.
     */
    public sealed interface DeepLink {
        /**
         * Represents a {@code chat.whatsapp.com} group invite link.
         *
         * <p>Dispatched to the group-invite preview resolver, which queries the group
         * metadata and downloads the group's profile picture as the inline thumbnail.
         *
         * @param code the group invite code
         */
        record GroupInvite(String code) implements DeepLink {
        }

        /**
         * Represents a business catalog link without a specific product.
         *
         * <p>Dispatched to the catalog preview resolver with a {@code null} product id,
         * which renders the catalog-of-owner card.
         *
         * @param catalogOwnerJid the JID of the catalog owner, in
         *                        {@code <number>@s.whatsapp.net} form
         */
        record Catalog(String catalogOwnerJid) implements DeepLink {
        }

        /**
         * Represents a business product link pointing at a specific product.
         *
         * <p>Dispatched to the catalog preview resolver, which renders the product name,
         * description, and price as the card.
         *
         * @param productId        the product identifier (a numeric id or a free-form
         *                         retailer SKU)
         * @param businessOwnerJid the JID of the business owner, in
         *                         {@code <number>@s.whatsapp.net} form
         */
        record Product(String productId, String businessOwnerJid) implements DeepLink {
        }

        /**
         * Represents a payment-link deep link.
         *
         * <p>Materialised onto the wire as payment-link metadata carrying the
         * payment-service-provider label; the preview card is rendered only when
         * {@link #shouldDetectInComposer()} is {@code true}, which holds for SMB clients
         * only.
         *
         * @param psp                    the payment service provider matched from
         *                               {@link ABProp#SMB_PAYMENT_LINKS_URL_REGEX_LIST}
         * @param shouldDetectInComposer whether the composer should materialise a
         *                               payment-link card
         */
        record PaymentLink(String psp, boolean shouldDetectInComposer) implements DeepLink {
        }

        /**
         * Represents a regular web link rather than a recognised WhatsApp deep-link.
         *
         * <p>Returned for every URL outside the four preview-pipeline-relevant deep-link
         * shapes, signalling that the rich-fetch branch of the text pipeline runs.
         */
        final class NotApplicable implements DeepLink {
            /**
             * Holds the shared singleton instance.
             *
             * <p>Compared against by identity in the link-preview pipeline switch; a second
             * {@code NotApplicable} is never instantiated.
             */
            public static final NotApplicable INSTANCE = new NotApplicable();

            /**
             * Prevents instantiation and enforces the singleton.
             */
            private NotApplicable() {
            }
        }
    }
}
