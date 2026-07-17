package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.AiLeadGenFlowInput;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that updates a lead-generation flow of a WhatsApp Business AI agent.
 *
 * <p>The single {@code request} GraphQL variable is the lead-gen-flow update input carried as an
 * {@link AiLeadGenFlowInput}: the flow id being updated, its moment configuration, and its list of
 * capture fields. WhatsApp Web's {@code WAWebBizAiLeadGenUpdateMutation.updateLeadGenFlow(request)}
 * forwards it to the Meta graph endpoint, which returns the update outcome under
 * {@code xfb_meta_ai_biz_agent_wa_update_lead_gen_flow}; the reply is consumed through
 * {@link BizAiLeadGenUpdateFacebookGraphQlResponse}. An update carries the input's flow id.
 *
 * @see BizAiLeadGenUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenUpdateMutation")
public final class BizAiLeadGenUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26449028471447289";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiLeadGenUpdateMutation";

    /**
     * The {@code request} GraphQL variable describing the lead-generation flow changes, or
     * {@code null} to omit it.
     */
    private final AiLeadGenFlowInput request;

    /**
     * Constructs an update-lead-gen-flow mutation request.
     *
     * <p>The {@code request} describes the flow changes and carries the flow id being updated. A
     * {@code null} value omits the variable.
     *
     * @param request the lead-generation flow changes, or {@code null} to omit the variable
     */
    public BizAiLeadGenUpdateFacebookGraphQlRequest(AiLeadGenFlowInput request) {
        this.request = request;
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
     * @implNote This implementation emits {@code {"request": {...}}} with the flow fields under the
     * snake_case keys {@code custom_moment}, {@code moment_type}, {@code fields} (each
     * {@code {label, is_enabled}}), and {@code id}, writing the variable only when the request is
     * non-null and emitting {@code "{}"} when it is {@code null}. The camelCase-to-snake_case mapping is
     * performed by {@link BizAiInputJson#writeLeadGenFlowInput(JSONWriter, AiLeadGenFlowInput)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenUpdateMutation", exports = "updateLeadGenFlow",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (request != null) {
                writer.writeName("request");
                writer.writeColon();
                BizAiInputJson.writeLeadGenFlowInput(writer, request);
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
