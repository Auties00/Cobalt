package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Parses the MEX response of the add-paid-partnership-label mutation built by
 * {@link NewsletterAddPaidPartnershipLabelMexRequest}.
 *
 * <p>Carries the message id echoed under {@code xwa2_newsletter_label_paid_partnership.id}. An empty
 * {@link #id()} is the failure signal: it means the relay refused the label, so callers replicate
 * the failure check by inspecting whether {@link #id()} is present.
 */
@WhatsAppWebModule(moduleName = "WAWebMexNewsletterAddPaidPartnershipLabelJob")
public final class NewsletterAddPaidPartnershipLabelMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the labelled message id echoed under
     * {@code xwa2_newsletter_label_paid_partnership.id}.
     */
    private final String id;

    /**
     * Constructs a response wrapping the echoed message id.
     *
     * <p>Invoked only by the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param id the message id echoed by the relay
     */
    private NewsletterAddPaidPartnershipLabelMexResponse(String id) {
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_label_paid_partnership} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<NewsletterAddPaidPartnershipLabelMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(NewsletterAddPaidPartnershipLabelMexResponse::of);
    }

    /**
     * Returns the labelled message id echoed by the relay.
     *
     * <p>Empty when the GraphQL envelope omits {@code id}, which is the signal that the relay
     * refused the label.
     *
     * @return the echoed message id, or empty when omitted
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
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
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_label_paid_partnership} root
     */
    private static Optional<NewsletterAddPaidPartnershipLabelMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_label_paid_partnership");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");

        return Optional.of(new NewsletterAddPaidPartnershipLabelMexResponse(id));
    }
}
