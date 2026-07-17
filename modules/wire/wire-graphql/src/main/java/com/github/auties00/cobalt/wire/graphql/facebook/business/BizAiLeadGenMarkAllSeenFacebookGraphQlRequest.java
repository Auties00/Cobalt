package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that marks all captured lead data of a lead-generation flow as seen.
 *
 * <p>The single {@code request} GraphQL variable carries the {@code lead_gen_flow_id} of the flow
 * whose leads are being marked. WhatsApp Web's
 * {@code WAWebBizAiLeadGenMarkAllSeenMutation.markAllLeadGenDataAsSeenForFlow(flowId)} wraps its
 * argument as {@code {lead_gen_flow_id: flowId}}, the server-assigned identifier of the lead-gen flow
 * stanza (not a WhatsApp address). The Meta graph endpoint returns the outcome under
 * {@code meta_ai_biz_agent_wa_mark_all_lead_gen_data_as_seen_for_flow}; the reply is consumed through
 * {@link BizAiLeadGenMarkAllSeenFacebookGraphQlResponse}.
 *
 * @see BizAiLeadGenMarkAllSeenFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenMarkAllSeenMutation")
public final class BizAiLeadGenMarkAllSeenFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenMarkAllSeenMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27447561748164034";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenMarkAllSeenMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiLeadGenMarkAllSeenMutation";

    /**
     * The {@code lead_gen_flow_id} field of the {@code request} object identifying the flow whose
     * leads are being marked as seen, or {@code null} to omit it.
     */
    private final String leadGenFlowId;

    /**
     * Constructs a mark-all-lead-data-as-seen mutation request.
     *
     * <p>The {@code leadGenFlowId} populates the {@code request} object's {@code lead_gen_flow_id}
     * field; a {@code null} value is omitted from the serialized object.
     *
     * @param leadGenFlowId the identifier of the lead-gen flow whose leads are being marked as seen,
     *                      or {@code null} to omit the field
     */
    public BizAiLeadGenMarkAllSeenFacebookGraphQlRequest(String leadGenFlowId) {
        this.leadGenFlowId = leadGenFlowId;
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
     * @implNote This implementation emits {@code {"request": {"lead_gen_flow_id": <leadGenFlowId>}}},
     * writing the {@code lead_gen_flow_id} field only when its value is non-null and emitting
     * {@code {"request": {}}} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenMarkAllSeenMutation",
            exports = "markAllLeadGenDataAsSeenForFlow", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (leadGenFlowId != null) {
                writer.writeName("lead_gen_flow_id");
                writer.writeColon();
                writer.writeString(leadGenFlowId);
            }
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
