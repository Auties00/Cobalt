package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.Optional;

/**
 * Parses the MEX response of the revoke-newsletter-admin-invite mutation built by
 * {@link RevokeNewsletterAdminInviteMexRequest}.
 *
 * <p>Carries the newsletter id echoed under {@code xwa2_newsletter_admin_invite_revoke.id} after the
 * invite has been removed server-side; consumers use it to confirm which newsletter just had its
 * pending admin invite cancelled.
 */
@WhatsAppWebModule(moduleName = "WAWebMexRevokeNewsletterAdminInviteJob")
public final class RevokeNewsletterAdminInviteMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string echoed under
     * {@code xwa2_newsletter_admin_invite_revoke.id}.
     */
    private final String id;

    /**
     * Constructs a response wrapping the echoed newsletter id.
     *
     * <p>Invoked only by the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param id the newsletter Jid string echoed by the relay
     */
    private RevokeNewsletterAdminInviteMexResponse(String id) {
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin_invite_revoke} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<RevokeNewsletterAdminInviteMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(RevokeNewsletterAdminInviteMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * <p>Empty when the GraphQL envelope omits {@code id}; otherwise carries the same Jid string
     * sent in {@link RevokeNewsletterAdminInviteMexRequest}.
     *
     * @return the echoed newsletter id, or empty when omitted
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
     *         {@code data.xwa2_newsletter_admin_invite_revoke} root
     */
    private static Optional<RevokeNewsletterAdminInviteMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin_invite_revoke");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");

        return Optional.of(new RevokeNewsletterAdminInviteMexResponse(id));
    }
}
