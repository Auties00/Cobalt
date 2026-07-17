package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ads.LwiBoostedComponentInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the comet mutation that creates a click-to-WhatsApp (CTWA) ad draft.
 *
 * <p>The mutation takes a single {@code input} GraphQL object carrying the draft payload as a
 * {@link LwiBoostedComponentInput}: the ad-group creatives, budget, schedule, audience, placement, and
 * welcome experience of the boost. The relay returns the created draft under
 * {@code create_ads_ctwa_draft}, whose {@code id} scalar is the new draft identifier; the reply is
 * consumed through {@link BizAdCreateDraftFacebookGraphQlResponse}.
 *
 * @see BizAdCreateDraftFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "useWAWebBizAdCreateDraftMutation")
public final class BizAdCreateDraftFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the Facebook GraphQL request body.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreateDraftMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "34750511344595780";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "useWAWebBizAdCreateDraftMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "useWAWebBizAdCreateDraftMutation";

    /**
     * The {@code input} GraphQL variable carrying the draft payload, or {@code null} to omit it.
     */
    private final LwiBoostedComponentInput input;

    /**
     * Constructs a create-CTWA-ad-draft mutation request.
     *
     * <p>The {@code input} holds the draft payload. A {@code null} value omits the variable from the
     * serialized object.
     *
     * @param input the boosted-component draft payload, or {@code null} to omit the variable
     */
    public BizAdCreateDraftFacebookGraphQlRequest(LwiBoostedComponentInput input) {
        this.input = input;
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
     * @implNote This implementation emits {@code {"input": {...}}} with the boost fields under their
     * snake_case keys, writing the variable only when the input is non-null and emitting {@code "{}"}
     * otherwise. The camelCase-to-snake_case mapping is performed by
     * {@link BizAdInputJson#writeLwiBoostedComponentInput(JSONWriter, LwiBoostedComponentInput)}.
     */
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                BizAdInputJson.writeLwiBoostedComponentInput(writer, input);
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
