package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelIdentity;
import com.github.auties00.cobalt.wire.linked.business.aichannel.AiChannelIdentityBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the GenAI agent-channel identity query built by
 * {@link CanonicalHatchIdentityGetWhatsAppWebGraphQlRequest} into an {@link AiChannelIdentity}.
 *
 * <p>Reads the linked root {@code wa_genai_hatch_identity_get} and flattens its nested
 * {@code avatar.image_url} chain onto the Cobalt domain model: the agent display name and the
 * resolved avatar image URL.
 *
 * @see CanonicalHatchIdentityGetWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalHatchIdentityGetQuery")
public final class CanonicalHatchIdentityGetWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed agent-channel identity.
     */
    private final AiChannelIdentity identity;

    /**
     * Constructs a response wrapping the parsed identity.
     *
     * <p>Reserved for the static parser.
     *
     * @param identity the parsed identity, or {@code null} when the relay omitted the field
     */
    private CanonicalHatchIdentityGetWhatsAppWebGraphQlResponse(AiChannelIdentity identity) {
        this.identity = identity;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code wa_genai_hatch_identity_get} and projects it onto an
     * {@link AiChannelIdentity}; the returned {@link Optional} is empty when {@code data} or the
     * identity object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the identity object is missing
     */
    public static Optional<CanonicalHatchIdentityGetWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_genai_hatch_identity_get");
        if (node == null) {
            return Optional.empty();
        }

        var avatar = node.getJSONObject("avatar");
        var identity = new AiChannelIdentityBuilder()
                .displayName(node.getString("name"))
                .avatarImageUrl(avatar == null ? null : avatar.getString("image_url"))
                .build();
        return Optional.of(new CanonicalHatchIdentityGetWhatsAppWebGraphQlResponse(identity));
    }

    /**
     * Returns the parsed agent-channel identity.
     *
     * @return the parsed {@link AiChannelIdentity}, never {@code null}
     */
    public AiChannelIdentity identity() {
        return identity;
    }
}
