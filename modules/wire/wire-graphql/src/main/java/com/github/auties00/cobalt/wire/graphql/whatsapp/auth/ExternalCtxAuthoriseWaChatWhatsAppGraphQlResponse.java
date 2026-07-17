package com.github.auties00.cobalt.wire.graphql.whatsapp.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.auth.ExternalChatDeepLinkAuthorization;
import com.github.auties00.cobalt.wire.linked.business.auth.ExternalChatDeepLinkAuthorizationBuilder;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the external deep-link chat-authorisation mutation built by
 * {@link ExternalCtxAuthoriseWaChatWhatsAppGraphQlRequest} into an
 * {@link ExternalChatDeepLinkAuthorization}.
 *
 * <p>Reads the linked root {@code xwa_external_ctx_authorise_wa_chat} and projects its scalars
 * (authorisation verdict and authorising partner display name) onto the Cobalt domain model.
 *
 * @see ExternalCtxAuthoriseWaChatWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebExternalCtxAuthoriseWAChatMutation")
public final class ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed deep-link chat-authorisation verdict.
     */
    private final ExternalChatDeepLinkAuthorization authorization;

    /**
     * Constructs a response wrapping the parsed authorisation.
     *
     * <p>Reserved for the static parser.
     *
     * @param authorization the parsed authorisation, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse(ExternalChatDeepLinkAuthorization authorization) {
        this.authorization = authorization;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_external_ctx_authorise_wa_chat} and projects it onto an
     * {@link ExternalChatDeepLinkAuthorization}; the returned {@link Optional} is empty when
     * {@code data} or the authorisation object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the authorisation object is missing
     */
    public static Optional<ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_external_ctx_authorise_wa_chat");
        if (node == null) {
            return Optional.empty();
        }

        var success = node.getBoolean("success");
        var authorization = new ExternalChatDeepLinkAuthorizationBuilder()
                .authorized(success != null && success)
                .partnerName(node.getString("partner_name"))
                .build();
        return Optional.of(new ExternalCtxAuthoriseWaChatWhatsAppGraphQlResponse(authorization));
    }

    /**
     * Returns the parsed deep-link chat-authorisation verdict.
     *
     * @return the parsed {@link ExternalChatDeepLinkAuthorization}, never {@code null}
     */
    public ExternalChatDeepLinkAuthorization authorization() {
        return authorization;
    }
}
