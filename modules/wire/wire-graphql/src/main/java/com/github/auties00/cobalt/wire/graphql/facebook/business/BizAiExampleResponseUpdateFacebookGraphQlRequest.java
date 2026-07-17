package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.AiFaqEntry;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that updates a WhatsApp Business AI agent's example responses (the FAQ
 * knowledge entries the agent answers from).
 *
 * <p>The mutation takes one {@code input} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiExampleResponseUpdateMutation.updateExampleResponses(faq)} wraps the FAQ payload as
 * {@code {input: {faq: <faq>}}} and forwards it to the Meta graph endpoint; the {@code faq} value is the
 * whole FAQ set, a list of {@link AiFaqEntry} question-answer entries. The Meta graph endpoint returns
 * the update outcome under {@code xfb_meta_ai_biz_agent_wa_update_knowledge}; the reply is consumed
 * through {@link BizAiExampleResponseUpdateFacebookGraphQlResponse}.
 *
 * @see BizAiExampleResponseUpdateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiExampleResponseUpdateMutation")
public final class BizAiExampleResponseUpdateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseUpdateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "36542743545312870";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseUpdateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiExampleResponseUpdateMutation";

    /**
     * The FAQ set placed under the {@code input.faq} GraphQL field, or {@code null} to omit it. Never
     * {@code null} after construction; an empty list omits the field.
     */
    private final List<AiFaqEntry> faq;

    /**
     * Constructs an example-response-update mutation request.
     *
     * <p>The {@code faq} is the whole FAQ set placed under {@code input.faq}. A {@code null} or empty
     * list omits the {@code faq} field from the serialized {@code input} object.
     *
     * @param faq the FAQ entries, or {@code null} to omit the field
     */
    public BizAiExampleResponseUpdateFacebookGraphQlRequest(List<AiFaqEntry> faq) {
        this.faq = faq == null ? List.of() : List.copyOf(faq);
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
     * @implNote This implementation emits {@code {"input": {"faq": [{"question": ..., "answer": ...,
     * "id": ...}, ...]}}}, writing the {@code faq} array only when the FAQ set is non-empty and emitting
     * {@code {"input": {}}} otherwise. Each entry is rendered by
     * {@link BizAiInputJson#writeFaqEntry(JSONWriter, AiFaqEntry)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseUpdateMutation", exports = "updateExampleResponses",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (!faq.isEmpty()) {
                writer.writeName("faq");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < faq.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    BizAiInputJson.writeFaqEntry(writer, faq.get(i));
                }
                writer.endArray();
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
