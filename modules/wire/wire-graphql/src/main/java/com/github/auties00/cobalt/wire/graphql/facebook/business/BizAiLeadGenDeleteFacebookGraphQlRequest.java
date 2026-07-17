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
 * Builds the Facebook GraphQL mutation that deletes a lead-generation flow of a WhatsApp Business AI agent.
 *
 * <p>The single {@code request} GraphQL variable is the lead-gen-flow deletion input. WhatsApp Web's
 * {@code WAWebBizAiLeadGenDeleteMutation.deleteLeadGenFlow(flowId)} wraps its argument as
 * {@code {flow_id: flowId}}, the server-assigned identifier of the flow to delete (the {@code id} of
 * the {@code XFBMetaAIBusinessAgentWhatsAppLeadGenFlow} stanza, not a WhatsApp address). The Meta graph endpoint
 * returns the deletion outcome under {@code xfb_meta_ai_biz_agent_wa_delete_lead_gen_flow}; the reply
 * is consumed through {@link BizAiLeadGenDeleteFacebookGraphQlResponse}.
 *
 * @see BizAiLeadGenDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenDeleteMutation")
public final class BizAiLeadGenDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26426132383680154";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiLeadGenDeleteMutation";

    /**
     * The {@code flow_id} field of the {@code request} object identifying the lead-gen flow to delete,
     * or {@code null} to omit it.
     */
    private final String flowId;

    /**
     * Constructs a delete-lead-gen-flow mutation request.
     *
     * <p>The {@code flowId} populates the {@code request} object's {@code flow_id} field; a
     * {@code null} value is omitted from the serialized object.
     *
     * @param flowId the identifier of the lead-gen flow to delete, or {@code null} to omit the field
     */
    public BizAiLeadGenDeleteFacebookGraphQlRequest(String flowId) {
        this.flowId = flowId;
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
     * @implNote This implementation emits {@code {"request": {"flow_id": <flowId>}}}, writing the
     * {@code flow_id} field only when its value is non-null and emitting {@code {"request": {}}} when
     * it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiLeadGenDeleteMutation", exports = "deleteLeadGenFlow",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("request");
            writer.writeColon();
            writer.startObject();
            if (flowId != null) {
                writer.writeName("flow_id");
                writer.writeColon();
                writer.writeString(flowId);
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
