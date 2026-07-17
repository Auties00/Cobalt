package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Parses the MEX response of the demote-newsletter-admin mutation built by
 * {@link DemoteNewsletterAdminMexRequest}.
 *
 * <p>Exposes the newsletter id echoed under {@code xwa2_newsletter_admin_demote}; consumers use it
 * to confirm the mutation applied to the expected channel before refreshing local membership state
 * to follower for the demoted user.
 */
@WhatsAppWebModule(moduleName = "WAWebMexDemoteNewsletterAdminJob")
public final class DemoteNewsletterAdminMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string echoed under the {@code xwa2_newsletter_admin_demote.id}
     * response field.
     */
    private final String id;

    /**
     * Constructs a response wrapping the echoed newsletter id.
     *
     * @param id the newsletter Jid echoed by the relay
     */
    private DemoteNewsletterAdminMexResponse(String id) {
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin_demote} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<DemoteNewsletterAdminMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(DemoteNewsletterAdminMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * @return the echoed newsletter id, or empty when the relay omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_admin_demote} root
     */
    private static Optional<DemoteNewsletterAdminMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin_demote");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");

        return Optional.of(new DemoteNewsletterAdminMexResponse(id));
    }
}
