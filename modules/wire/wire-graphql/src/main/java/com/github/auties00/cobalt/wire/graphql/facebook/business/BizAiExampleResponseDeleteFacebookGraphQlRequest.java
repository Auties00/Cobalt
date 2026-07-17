package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that deletes business-AI knowledge entries for a WhatsApp Business AI
 * agent.
 *
 * <p>The mutation takes one GraphQL variable, {@code input}, an object carrying the knowledge to
 * delete. WhatsApp Web's {@code WAWebBizAiExampleResponseDeleteMutation} builds it two ways: the
 * {@code deleteExampleResponse(type)} entry point sends {@code {knowledge_types_to_delete: [type]}},
 * and the {@code deleteKnowledgeWithIds(type, faqIds)} entry point additionally sends
 * {@code {faq_ids: faqIds}}. Both forward straight to the Meta graph endpoint, which returns the delete outcome
 * under the linked {@code xfb_meta_ai_biz_agent_wa_delete_knowledge} field; the reply is consumed
 * through {@link BizAiExampleResponseDeleteFacebookGraphQlResponse}.
 *
 * <p>The {@code knowledge_types_to_delete} values are kept as {@link String}: the JS bundle surfaces
 * knowledge-type labels such as {@code "FAQ"}, {@code "DESCRIPTION"} and {@code "ADDRESS"} only
 * through a curated UI suggestion config, never as an authoritative complete value set, so no Java
 * enum is introduced for them.
 *
 * @see BizAiExampleResponseDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiExampleResponseDeleteMutation")
public final class BizAiExampleResponseDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25766734919668692";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiExampleResponseDeleteMutation";

    /**
     * The {@code knowledge_types_to_delete} field of the {@code input} object naming the knowledge
     * types to delete, or {@code null} to omit it.
     */
    private final List<String> knowledgeTypesToDelete;

    /**
     * The {@code faq_ids} field of the {@code input} object naming the FAQ ids to delete, or
     * {@code null} to omit it.
     */
    private final List<String> faqIds;

    /**
     * Constructs a delete-knowledge mutation request carrying the knowledge types and FAQ ids.
     *
     * <p>Both values populate the {@code input} object; each value that is {@code null} is omitted
     * from the serialized object. Passing {@code null} for {@code faqIds} mirrors the
     * {@code deleteExampleResponse} entry point, which sends only the knowledge types; passing a
     * non-null list mirrors {@code deleteKnowledgeWithIds}.
     *
     * @param knowledgeTypesToDelete the knowledge types to delete, or {@code null} to omit the field
     * @param faqIds                 the FAQ ids to delete, or {@code null} to omit the field
     */
    public BizAiExampleResponseDeleteFacebookGraphQlRequest(List<String> knowledgeTypesToDelete, List<String> faqIds) {
        this.knowledgeTypesToDelete = knowledgeTypesToDelete;
        this.faqIds = faqIds;
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
     * @implNote This implementation emits {@code {"input": {"knowledge_types_to_delete": [...],
     * "faq_ids": [...]}}}, writing each {@code input} field only when its value is non-null and
     * emitting {@code {"input": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseDeleteMutation", exports = "deleteExampleResponse",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebBizAiExampleResponseDeleteMutation", exports = "deleteKnowledgeWithIds",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (knowledgeTypesToDelete != null) {
                writer.writeName("knowledge_types_to_delete");
                writer.writeColon();
                writeStringArray(writer, knowledgeTypesToDelete);
            }

            if (faqIds != null) {
                writer.writeName("faq_ids");
                writer.writeColon();
                writeStringArray(writer, faqIds);
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

    /**
     * Writes the given list of strings as a JSON array onto the supplied writer.
     *
     * @param writer the writer to emit the array onto
     * @param values the strings to emit, never {@code null}
     */
    private static void writeStringArray(JSONWriter writer, List<String> values) {
        writer.startArray();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            writer.writeString(values.get(i));
        }
        writer.endArray();
    }
}
