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
 * Builds the Facebook GraphQL mutation that deletes a pending business-AI knowledge-review item.
 *
 * <p>The mutation takes a single {@code id} GraphQL variable naming the potential-knowledge item to
 * delete. WhatsApp Web's {@code WAWebBizAiKnowledgeReviewDeleteMutation.deletePendingData(id)}
 * forwards the id straight to the Meta graph endpoint as the {@code id} variable. The id is the opaque
 * knowledge-item identifier the review surface assigns to a pending item, not a WhatsApp address, so
 * it is modelled as a {@link String}. The Meta graph endpoint returns the deletion outcome under the linked field
 * {@code xfb_maiba_delete_potential_knowledge_mutation}; the reply is consumed through
 * {@link BizAiKnowledgeReviewDeleteFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeReviewDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeReviewDeleteMutation")
public final class BizAiKnowledgeReviewDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24454985560765299";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeReviewDeleteMutation";

    /**
     * The {@code id} GraphQL variable naming the pending knowledge-review item to delete, or
     * {@code null} to omit it.
     */
    private final String id;

    /**
     * Constructs a delete-pending-knowledge-review mutation request.
     *
     * <p>The {@code id} names the pending knowledge-review item to delete; a {@code null} value omits
     * the variable from the serialized object.
     *
     * @param id the pending knowledge-review item id to delete, or {@code null} to omit the variable
     */
    public BizAiKnowledgeReviewDeleteFacebookGraphQlRequest(String id) {
        this.id = id;
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
     * @implNote This implementation emits {@code {"id": <id>}}, writing the variable only when it is
     * non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewDeleteMutation", exports = "deletePendingData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (id != null) {
                writer.writeName("id");
                writer.writeColon();
                writer.writeString(id);
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
