package com.github.auties00.cobalt.wire.graphql.whatsappWeb.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the canonical-user-valid query built by
 * {@link CanonicalUserValidWhatsAppWebGraphQlRequest} into a single boolean validity verdict.
 *
 * <p>Reads the linked root {@code xwa_canonical_user_valid} and projects its single scalar
 * {@code success} onto a boolean: whether the authenticated session resolves to a valid canonical
 * WhatsApp user.
 *
 * @see CanonicalUserValidWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalUserValidQuery")
public final class CanonicalUserValidWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed validity verdict.
     */
    private final boolean valid;

    /**
     * Constructs a response wrapping the parsed validity verdict.
     *
     * <p>Reserved for the static parser.
     *
     * @param valid the parsed validity verdict
     */
    private CanonicalUserValidWhatsAppWebGraphQlResponse(boolean valid) {
        this.valid = valid;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_canonical_user_valid} and projects its {@code success}
     * scalar onto a boolean; the returned {@link Optional} is empty when {@code data} or the
     * canonical-user object is missing. An absent {@code success} field is treated as {@code false}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the canonical-user object is missing
     */
    public static Optional<CanonicalUserValidWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_canonical_user_valid");
        if (node == null) {
            return Optional.empty();
        }

        var success = node.getBoolean("success");
        return Optional.of(new CanonicalUserValidWhatsAppWebGraphQlResponse(success != null && success));
    }

    /**
     * Returns whether the authenticated session resolves to a valid canonical WhatsApp user.
     *
     * @return {@code true} when the relay reported the verdict set, {@code false} when it did not or
     *         omitted the field
     */
    public boolean valid() {
        return valid;
    }
}
