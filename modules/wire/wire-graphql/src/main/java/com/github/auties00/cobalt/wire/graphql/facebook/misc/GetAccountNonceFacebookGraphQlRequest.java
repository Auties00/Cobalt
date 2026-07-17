package com.github.auties00.cobalt.wire.graphql.facebook.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that requests a single-use business account nonce.
 *
 * <p>The single {@code input} GraphQL variable is the {@code XFBWhatsAppBizAccountNonceInput} object.
 * WhatsApp Web's {@code WAWebGetAccountNonce} fills it with a nested {@code identifier} object whose
 * {@code scope} field names the nonce scope; the only value the bundle emits is the literal
 * {@code "REQUEST"}. The Meta graph endpoint returns the issued nonce and its request id under
 * {@code xfb_wa_biz_account_nonce}; the reply is consumed through
 * {@link GetAccountNonceFacebookGraphQlResponse}.
 *
 * @see GetAccountNonceFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebGetAccountNonceMutation")
public final class GetAccountNonceFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccountNonceMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25091178200467555";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccountNonceMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebGetAccountNonceMutation";

    /**
     * The {@code scope} field of the nested {@code identifier} object naming the nonce scope, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web emits the literal {@code "REQUEST"}; the value is kept as a {@link String}
     * because the full nonce-scope value set is not confirmable from the JS bundle.
     */
    private final String scope;

    /**
     * Constructs a get-account-nonce request carrying the nonce scope.
     *
     * <p>The value populates the nested {@code input.identifier} object; a {@code null} value is
     * omitted from the serialized object.
     *
     * @param scope the nonce scope, or {@code null} to omit the field
     */
    public GetAccountNonceFacebookGraphQlRequest(String scope) {
        this.scope = scope;
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
     * @implNote This implementation emits {@code {"input": {"identifier": {"scope": <scope>}}}},
     * writing the {@code scope} field only when its value is non-null and emitting an empty
     * {@code identifier} object when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebGetAccountNonce", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            writer.writeName("identifier");
            writer.writeColon();
            writer.startObject();
            if (scope != null) {
                writer.writeName("scope");
                writer.writeColon();
                writer.writeString(scope);
            }
            writer.endObject();
            writer.endObject();
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
