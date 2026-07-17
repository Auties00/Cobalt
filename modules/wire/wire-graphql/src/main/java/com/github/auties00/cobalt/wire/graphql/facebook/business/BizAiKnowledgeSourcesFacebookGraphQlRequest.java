package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL query that lists the knowledge sources configured for a WhatsApp
 * Business AI agent.
 *
 * <p>The operation takes no variables; it asks the Meta graph endpoint for the agent's AI-home view, returned under
 * {@code xfb_meta_ai_biz_agent_wa_ai_home}, which carries the chat-history export status and the list
 * of configured knowledge sources (uploaded files, chat history, and websites). The reply is consumed
 * through {@link BizAiKnowledgeSourcesFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeSourcesFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourcesQuery")
public final class BizAiKnowledgeSourcesFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourcesQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26739372069027009";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourcesQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeSourcesQuery";

    /**
     * Constructs a list-knowledge-sources request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAiKnowledgeSourcesFacebookGraphQlRequest() {
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
     * @implNote This implementation returns the empty object {@code "{}"}: the operation declares no
     * GraphQL variables, so there is nothing to serialize.
     */
    @Override
    public String variables() {
        return "{}";
    }
}
