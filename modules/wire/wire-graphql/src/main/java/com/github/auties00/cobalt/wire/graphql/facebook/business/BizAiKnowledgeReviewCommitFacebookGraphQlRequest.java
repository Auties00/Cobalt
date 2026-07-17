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
 * Builds the Facebook GraphQL mutation that approves (commits) WhatsApp Business AI agent potential-knowledge
 * items the operator has reviewed.
 *
 * <p>The mutation takes one {@code ids} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiKnowledgeReviewCommitMutation.commitPendingData(ids)} forwards the operator's
 * selected knowledge-item ids straight to the Meta graph endpoint as {@code {ids: [...]}}; the ids are the
 * knowledge-item identifiers surfaced by
 * {@link com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiPendingKnowledge} and are plain string
 * ids rather than WhatsApp addresses. The Meta graph endpoint returns the commit outcome under
 * {@code xfb_maiba_approve_potential_knowledge}; the reply is consumed through
 * {@link BizAiKnowledgeReviewCommitFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeReviewCommitFacebookGraphQlResponse
 * @see com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiPendingKnowledge
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeReviewCommitMutation")
public final class BizAiKnowledgeReviewCommitFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewCommitMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24096216066682686";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewCommitMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeReviewCommitMutation";

    /**
     * The {@code ids} GraphQL variable listing the knowledge-item ids to approve, or {@code null} to
     * omit it.
     */
    private final List<String> ids;

    /**
     * Constructs a knowledge-review-commit mutation request carrying the ids to approve.
     *
     * <p>A {@code null} value omits the {@code ids} variable from the serialized object; an empty
     * list serializes as an empty array.
     *
     * @param ids the knowledge-item ids to approve, or {@code null} to omit the variable
     */
    public BizAiKnowledgeReviewCommitFacebookGraphQlRequest(List<String> ids) {
        this.ids = ids;
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
     * @implNote This implementation emits {@code {"ids": [...]}}, writing the {@code ids} array only
     * when the list is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewCommitMutation", exports = "commitPendingData",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (ids != null) {
                writer.writeName("ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < ids.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(ids.get(i));
                }
                writer.endArray();
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
