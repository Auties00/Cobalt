package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;

/**
 * Builds the Facebook GraphQL query that loads the WhatsApp Business AI agent's potential knowledge
 * pending review.
 *
 * <p>The operation takes one {@code timestamp} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiKnowledgeReviewQuery.getAllPendingKnowledge()} sets it to {@code Date.now()} and
 * passes it as a millisecond string; it gates which potential-knowledge items the Meta graph
 * endpoint returns. The Meta graph endpoint returns the pending items under
 * {@code xfb_maiba_load_potential_knowledge_for_review}; the reply is consumed through
 * {@link BizAiKnowledgeReviewFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeReviewFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeReviewQuery")
public final class BizAiKnowledgeReviewFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "24991295137127144";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeReviewQuery";

    /**
     * The {@code timestamp} GraphQL variable gating which pending-knowledge items are returned, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web sets this to the current instant; it is serialized as milliseconds-since-epoch.
     */
    private final Instant timestamp;

    /**
     * Constructs a knowledge-review request carrying the review cut-off timestamp.
     *
     * <p>A {@code null} value omits the {@code timestamp} variable from the serialized object.
     *
     * @param timestamp the review cut-off instant, or {@code null} to omit the variable
     */
    public BizAiKnowledgeReviewFacebookGraphQlRequest(Instant timestamp) {
        this.timestamp = timestamp;
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
     * @implNote This implementation emits {@code {"timestamp": "<millis>"}}, rendering
     * {@code timestamp} as its milliseconds-since-epoch string to match the source
     * {@code String(Date.now())} value, and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeReviewQuery", exports = "getAllPendingKnowledge",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (timestamp != null) {
                writer.writeName("timestamp");
                writer.writeColon();
                writer.writeString(String.valueOf(timestamp.toEpochMilli()));
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
