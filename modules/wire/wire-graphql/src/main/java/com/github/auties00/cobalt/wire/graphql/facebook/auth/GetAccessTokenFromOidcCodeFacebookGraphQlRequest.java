package com.github.auties00.cobalt.wire.graphql.facebook.auth;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that exchanges an OpenID Connect authorization code for a Facebook
 * access token bound to the WhatsApp Business advertising flow.
 *
 * <p>The mutation takes two scalar GraphQL variables: the {@code code} OIDC authorization code and
 * the {@code state} anti-forgery nonce returned by the identity provider. WhatsApp Web drives it from
 * {@code WAWebGetAccessTokenFromOIDCCode} to mint a {@code WAWebCommonAdsTypes} ad-account token. The
 * Meta graph endpoint returns the minted token under {@code xfb_wa_biz_get_token_from_oidc_code}; the reply is
 * consumed through {@link GetAccessTokenFromOidcCodeFacebookGraphQlResponse}.
 *
 * @see GetAccessTokenFromOidcCodeFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGetAccessTokenFromOIDCCodeMutation")
public final class GetAccessTokenFromOidcCodeFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccessTokenFromOIDCCodeMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25278212845117908";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccessTokenFromOIDCCodeMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGetAccessTokenFromOIDCCodeMutation";

    /**
     * The {@code code} GraphQL variable carrying the OIDC authorization code, or {@code null} to omit
     * it.
     */
    private final String code;

    /**
     * The {@code state} GraphQL variable carrying the OIDC anti-forgery nonce, or {@code null} to omit
     * it.
     */
    private final String state;

    /**
     * Constructs a get-access-token-from-OIDC-code mutation request.
     *
     * <p>Each value populates its own GraphQL variable; a {@code null} value omits that variable from
     * the serialized object.
     *
     * @param code  the OIDC authorization code, or {@code null} to omit the variable
     * @param state the OIDC anti-forgery nonce, or {@code null} to omit the variable
     */
    public GetAccessTokenFromOidcCodeFacebookGraphQlRequest(String code, String state) {
        this.code = code;
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"code": <code>, "state": <state>}}, writing each
     * variable only when its value is non-null and emitting {@code "{}"} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccessTokenFromOIDCCode", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (code != null) {
                writer.writeName("code");
                writer.writeColon();
                writer.writeString(code);
            }

            if (state != null) {
                writer.writeName("state");
                writer.writeColon();
                writer.writeString(state);
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
