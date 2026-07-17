package com.github.auties00.cobalt.wire.graphql.whatsapp.auth;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessSignupMetadata;
import com.github.auties00.cobalt.wire.linked.business.auth.BusinessSignupMetadataBuilder;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the WhatsApp Business signup-metadata query built by
 * {@link SignupMetadataWhatsAppGraphQlRequest} into a {@link BusinessSignupMetadata}.
 *
 * <p>Reads the linked root {@code wa_signup_metadata} and projects its scalars (server-issued signup
 * identifier, consent message, and privacy-policy URL) onto the Cobalt domain model. WhatsApp Web
 * treats the reply as invalid when either the id or the message is missing.
 *
 * @see SignupMetadataWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebSignupMetadataQuery")
public final class SignupMetadataWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed signup metadata.
     */
    private final BusinessSignupMetadata metadata;

    /**
     * Constructs a response wrapping the parsed signup metadata.
     *
     * <p>Reserved for the static parser.
     *
     * @param metadata the parsed signup metadata, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private SignupMetadataWhatsAppGraphQlResponse(BusinessSignupMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code wa_signup_metadata} and projects it onto a
     * {@link BusinessSignupMetadata}; the returned {@link Optional} is empty when {@code data} or
     * the metadata object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the metadata object is missing
     */
    public static Optional<SignupMetadataWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("wa_signup_metadata");
        if (node == null) {
            return Optional.empty();
        }

        var metadata = new BusinessSignupMetadataBuilder()
                .id(node.getString("id"))
                .signupMessage(node.getString("signup_message"))
                .privacyPolicyUrl(node.getString("privacy_policy_url"))
                .build();
        return Optional.of(new SignupMetadataWhatsAppGraphQlResponse(metadata));
    }

    /**
     * Returns the parsed signup metadata.
     *
     * @return the parsed {@link BusinessSignupMetadata}, never {@code null}
     */
    public BusinessSignupMetadata metadata() {
        return metadata;
    }
}
