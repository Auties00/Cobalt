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
 * Builds the comet mutation that resumes a paused WhatsApp Business boosted component (an ad).
 *
 * <p>The single {@code boostID} GraphQL variable is the Facebook boost object identifier of the
 * boosted component to resume; the compiled document maps it onto the
 * {@code wa_resume_boosted_component} root mutation. The {@code boostID} is a numeric Facebook stanza id
 * rather than a WhatsApp address, so it is kept as a {@link String}. The mutation echoes the affected
 * component's id; the reply is consumed through {@link BizAdResumeFacebookGraphQlResponse}.
 *
 * @implNote This implementation models the {@code boostID} variable from the operation spec because
 * the {@code useWAWebBizAdResumeMutation} relay document and its calling hook are absent from the JS
 * bundle of snapshot {@code 1040120866}; only the persisted id, variable name, and response selection
 * tree are recoverable.
 *
 * @see BizAdResumeFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdResumeMutation")
public final class BizAdResumeFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdResumeMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "33395644700079788";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdResumeMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdResumeMutation";

    /**
     * The {@code boostID} GraphQL variable identifying the Facebook boosted component to resume, or
     * {@code null} to omit it.
     */
    private final String boostId;

    /**
     * Constructs a resume-boosted-component mutation request.
     *
     * <p>The {@code boostId} is the Facebook boost object identifier of the boosted component to
     * resume. A {@code null} value omits the variable from the serialized object.
     *
     * @param boostId the Facebook boost object identifier, or {@code null} to omit the variable
     */
    public BizAdResumeFacebookGraphQlRequest(String boostId) {
        this.boostId = boostId;
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
     * @implNote This implementation emits {@code {"boostID": <boostId>}}, writing the variable only
     * when its value is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (boostId != null) {
                writer.writeName("boostID");
                writer.writeColon();
                writer.writeString(boostId);
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
