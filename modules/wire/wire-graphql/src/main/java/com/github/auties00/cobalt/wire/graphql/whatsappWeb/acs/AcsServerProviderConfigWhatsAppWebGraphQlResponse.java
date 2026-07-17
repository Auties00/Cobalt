package com.github.auties00.cobalt.wire.graphql.whatsappWeb.acs;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialServiceConfig;
import com.github.auties00.cobalt.wire.linked.business.acs.AnonymousCredentialServiceConfigBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the anonymous-credential server-provider configuration query built
 * by {@link AcsServerProviderConfigWhatsAppWebGraphQlRequest} into an
 * {@link AnonymousCredentialServiceConfig}.
 *
 * <p>Reads the linked {@code xwa_wa_acs_config} root and projects its identifier, cipher suite,
 * public key, evaluation and redemption limits, configuration expiry, and token lifetime onto an
 * {@link AnonymousCredentialServiceConfig}.
 *
 * @see AcsServerProviderConfigWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebACSServerProviderConfigQuery")
public final class AcsServerProviderConfigWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed service configuration.
     */
    private final AnonymousCredentialServiceConfig config;

    /**
     * Constructs a response wrapping the parsed service configuration.
     *
     * <p>Reserved for the static parser.
     *
     * @param config the parsed service configuration
     */
    private AcsServerProviderConfigWhatsAppWebGraphQlResponse(AnonymousCredentialServiceConfig config) {
        this.config = config;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code xwa_wa_acs_config} root and projects it onto an
     * {@link AnonymousCredentialServiceConfig}; the returned {@link Optional} is empty when
     * {@code data} or the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root is missing
     */
    public static Optional<AcsServerProviderConfigWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa_wa_acs_config");
        if (root == null) {
            return Optional.empty();
        }

        var config = new AnonymousCredentialServiceConfigBuilder()
                .id(root.getString("id"))
                .cipherSuite(root.getString("cipher_suite"))
                .publicKey(root.getString("public_key"))
                .evaluationLimit(root.getLong("max_evals"))
                .redemptionLimit(root.getLong("redemption_limit"))
                .expiresAtEpochMilli(root.getLong("expire_time"))
                .tokenLifetimeMillis(root.getLong("token_ttl"))
                .build();
        return Optional.of(new AcsServerProviderConfigWhatsAppWebGraphQlResponse(config));
    }

    /**
     * Returns the parsed service configuration.
     *
     * @return the parsed {@link AnonymousCredentialServiceConfig}, never {@code null}
     */
    public AnonymousCredentialServiceConfig config() {
        return config;
    }
}
