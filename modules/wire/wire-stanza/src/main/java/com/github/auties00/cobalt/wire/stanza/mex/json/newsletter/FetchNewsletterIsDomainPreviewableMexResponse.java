package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-is-domain-previewable query built by
 * {@link FetchNewsletterIsDomainPreviewableMexRequest}.
 *
 * <p>Exposes the per-domain allowlist verdict echoed under
 * {@code xwa2_newsletter_message_integrity}. The {@link UrlPreviews} entries pair the queried
 * domain with a boolean indicating whether the relay permits a link preview for that domain inside
 * newsletter messages.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterIsDomainPreviewableJob")
public final class FetchNewsletterIsDomainPreviewableMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the per-domain previewability verdicts.
     */
    private final List<UrlPreviews> urlPreviews;

    /**
     * Constructs a response wrapping the parsed verdicts.
     *
     * @param urlPreviews the per-domain previewability verdicts
     */
    private FetchNewsletterIsDomainPreviewableMexResponse(List<UrlPreviews> urlPreviews) {
        this.urlPreviews = urlPreviews;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_message_integrity} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterIsDomainPreviewableMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterIsDomainPreviewableMexResponse::of);
    }

    /**
     * Returns the per-domain previewability verdicts.
     *
     * @return the parsed verdicts, empty when the relay returned none
     */
    public List<UrlPreviews> urlPreviews() {
        return urlPreviews;
    }

    /**
     * Wraps one entry of the {@code url_previews} array.
     *
     * <p>Each entry carries the queried {@code url_domain} string and the boolean verdict; WhatsApp
     * Web collapses the list into a domain-keyed boolean map before consumption.
     */
    public static final class UrlPreviews {
        /**
         * Holds the queried domain.
         */
        private final String urlDomain;

        /**
         * Holds whether the relay permits a preview for the domain.
         */
        private final Boolean isPreviewable;

        /**
         * Constructs a verdict wrapper from the parsed sub-fields.
         *
         * @param urlDomain     the queried domain
         * @param isPreviewable the relay verdict, may be {@code null}
         */
        private UrlPreviews(String urlDomain, Boolean isPreviewable) {
            this.urlDomain = urlDomain;
            this.isPreviewable = isPreviewable;
        }

        /**
         * Returns the queried domain.
         *
         * @return the domain, or empty when the relay omitted the field
         */
        public Optional<String> urlDomain() {
            return Optional.ofNullable(urlDomain);
        }

        /**
         * Returns whether the relay permits a preview for the domain.
         *
         * @return {@code true} when the relay reported the domain as previewable, {@code false}
         *         otherwise or when it omitted the field
         */
        public boolean isPreviewable() {
            return isPreviewable != null && isPreviewable;
        }

        /**
         * Parses an {@link UrlPreviews} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<UrlPreviews> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var urlDomain = obj.getString("url_domain");
            var isPreviewable = obj.getBoolean("is_previewable");
            return Optional.of(new UrlPreviews(urlDomain, isPreviewable));
        }

        /**
         * Parses a list of {@link UrlPreviews} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<UrlPreviews> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<UrlPreviews>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_message_integrity} root
     */
    private static Optional<FetchNewsletterIsDomainPreviewableMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_message_integrity");
        if (root == null) {
            return Optional.empty();
        }

        var urlPreviews = UrlPreviews.ofArray(root.getJSONArray("url_previews"));

        return Optional.of(new FetchNewsletterIsDomainPreviewableMexResponse(urlPreviews));
    }
}
