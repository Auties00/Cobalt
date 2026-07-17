package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.Optional;

/**
 * Parses the MEX response of the create-newsletter-admin-invite mutation built by
 * {@link CreateNewsletterAdminInviteMexRequest}.
 *
 * <p>Exposes the invite expiration timestamp and the newsletter id echoed under
 * {@code xwa2_newsletter_admin_invite_create}. The expiration timestamp is fed into the invite chat
 * message sent to the invitee.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateNewsletterAdminInviteJob")
public final class CreateNewsletterAdminInviteMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the unix-second timestamp at which the created invite expires.
     */
    private final Long inviteExpirationTime;

    /**
     * Holds the newsletter Jid string echoed under the
     * {@code xwa2_newsletter_admin_invite_create.id} response field.
     */
    private final String id;

    /**
     * Constructs a response wrapping the parsed scalar fields.
     *
     * @param inviteExpirationTime the unix-second invite expiration timestamp
     * @param id                   the newsletter Jid echoed by the relay
     */
    private CreateNewsletterAdminInviteMexResponse(Long inviteExpirationTime, String id) {
        this.inviteExpirationTime = inviteExpirationTime;
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin_invite_create} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<CreateNewsletterAdminInviteMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(CreateNewsletterAdminInviteMexResponse::of);
    }

    /**
     * Returns the invite expiration timestamp.
     *
     * <p>Carried by the relay as a unix-second integer and surfaced as an {@link Instant} for the
     * invite chat message.
     *
     * @return the parsed {@link Instant}, or empty when the relay omitted the field
     */
    public Optional<Instant> inviteExpirationTime() {
        return Optional.ofNullable(inviteExpirationTime).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * @return the echoed newsletter id, or empty when omitted
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
     *         {@code data.xwa2_newsletter_admin_invite_create} root
     */
    private static Optional<CreateNewsletterAdminInviteMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin_invite_create");
        if (root == null) {
            return Optional.empty();
        }

        var inviteExpirationTime = root.getLong("invite_expiration_time");
        var id = root.getString("id");

        return Optional.of(new CreateNewsletterAdminInviteMexResponse(inviteExpirationTime, id));
    }
}
