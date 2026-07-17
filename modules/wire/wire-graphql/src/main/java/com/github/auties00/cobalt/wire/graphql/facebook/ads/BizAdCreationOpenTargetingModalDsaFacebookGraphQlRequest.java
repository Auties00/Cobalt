package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL query that checks whether a targeting spec is subject to the EU Digital Services
 * Act (DSA) before opening the WhatsApp Business ad-creation targeting modal.
 *
 * <p>The query takes two GraphQL variables: {@code adAccountID}, the Facebook ad account the
 * targeting belongs to, and {@code targetSpecString}, the JSON-encoded targeting spec to evaluate.
 * The relay returns the DSA verdict under the linked {@code lwi} root's scalar
 * {@code target_spec_subject_to_dsa}; the reply is consumed through
 * {@link BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse}.
 *
 * @implNote This implementation derives its two variables from the operation spec because the
 * {@code useWAWebBizAdCreationOpenTargetingModalDSAQuery} hook module and its compiled
 * {@code .graphql} document are not present in the static bundle of snapshot {@code 1040120866}; it
 * is one of the Comet ad-creation documents loaded on demand. {@code adAccountID} is a Facebook
 * ad-account identifier (a numeric string), not a WhatsApp address, so it is modelled as a
 * {@code String} rather than a {@link com.github.auties00.cobalt.wire.core.jid.Jid};
 * {@code targetSpecString} is the already-serialized targeting spec and is likewise a {@code String}.
 *
 * @see BizAdCreationOpenTargetingModalDsaFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreationOpenTargetingModalDSAQuery")
public final class BizAdCreationOpenTargetingModalDsaFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationOpenTargetingModalDSAQuery.graphql",
            exports = "params.id", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25886732257633120";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreationOpenTargetingModalDSAQuery.graphql",
            exports = "params.name", adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreationOpenTargetingModalDSAQuery";

    /**
     * The {@code adAccountID} GraphQL variable naming the Facebook ad account the targeting belongs
     * to, or {@code null} to omit it.
     *
     * <p>A Facebook ad-account identifier (a numeric string), not a WhatsApp address.
     */
    private final String adAccountId;

    /**
     * The {@code targetSpecString} GraphQL variable carrying the JSON-encoded targeting spec to
     * evaluate, or {@code null} to omit it.
     */
    private final String targetSpecString;

    /**
     * Constructs an open-targeting-modal DSA query request.
     *
     * <p>The {@code adAccountId} populates the {@code adAccountID} GraphQL variable and the
     * {@code targetSpecString} populates the {@code targetSpecString} GraphQL variable; each value
     * that is {@code null} omits its variable from the serialized object.
     *
     * @param adAccountId      the Facebook ad-account identifier, or {@code null} to omit the variable
     * @param targetSpecString the JSON-encoded targeting spec, or {@code null} to omit the variable
     */
    public BizAdCreationOpenTargetingModalDsaFacebookGraphQlRequest(String adAccountId, String targetSpecString) {
        this.adAccountId = adAccountId;
        this.targetSpecString = targetSpecString;
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
     * @implNote This implementation emits {@code {"adAccountID": <adAccountId>, "targetSpecString":
     * <targetSpecString>}}, writing each variable only when its value is non-null and emitting
     * {@code "{}"} when both are {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (adAccountId != null) {
                writer.writeName("adAccountID");
                writer.writeColon();
                writer.writeString(adAccountId);
            }

            if (targetSpecString != null) {
                writer.writeName("targetSpecString");
                writer.writeColon();
                writer.writeString(targetSpecString);
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
