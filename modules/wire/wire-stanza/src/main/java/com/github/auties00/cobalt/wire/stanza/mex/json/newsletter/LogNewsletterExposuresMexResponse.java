package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Parses the MEX response of the log-newsletter-exposures mutation built by
 * {@link LogNewsletterExposuresMexRequest}.
 *
 * <p>Acts as a presence marker: the relay's reply carries no field of interest, so this type
 * exposes no getters and a marker instance signals only that the
 * {@code data.xwa2_newsletter_log_exposures} root was present in the reply.
 *
 * @deprecated Intentionally not wired: exposure logging is genuinely fire-and-forget. WhatsApp Web
 * awaits the mutation internally but discards its result and swallows any error, so
 * {@code LiveLinkedWhatsAppClient.logNewsletterExposures} dispatches the request with
 * {@code sendNodeWithNoResponse} and never parses this response. It is retained only to document the
 * reply shape.
 */
@Deprecated
@WhatsAppWebModule(moduleName = "WAWebMexLogNewsletterExposuresJob")
public final class LogNewsletterExposuresMexResponse implements MexStanza.Response.Json {

    /**
     * Constructs the marker response.
     *
     * <p>Invoked only by the static parser; external callers obtain instances via {@link #of(Stanza)}.
     * The marker carries no state because the relay's reply has no fields of interest.
     */
    private LogNewsletterExposuresMexResponse() {
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_log_exposures} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the marker response, or empty when the stanza does not carry a well-formed result payload
     */
    public static Optional<LogNewsletterExposuresMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(LogNewsletterExposuresMexResponse::of);
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Invoked only by the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the marker response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_log_exposures} root
     */
    private static Optional<LogNewsletterExposuresMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.get("xwa2_newsletter_log_exposures");
        if (root == null) {
            return Optional.empty();
        }

        return Optional.of(new LogNewsletterExposuresMexResponse());
    }
}
