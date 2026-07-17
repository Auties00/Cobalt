package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Builds the Facebook GraphQL mutation that deletes the business-AI chat-history knowledge source.
 *
 * <p>The operation takes no variables; WhatsApp Web's
 * {@code WAWebBizAiKnowledgeSourceDeleteMutation.deleteChatHistorySource()} commits the mutation with
 * an empty variable object, asking the Meta graph endpoint to delete the chat-history data source
 * tied to the authenticated business account. The Meta graph endpoint returns the deletion outcome
 * under the linked field {@code xfb_maiba_delete_chat_history}; the reply is consumed through
 * {@link BizAiKnowledgeSourceDeleteMutationChatHistoryFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeSourceDeleteMutationChatHistoryFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutationChatHistoryMutation")
public final class BizAiKnowledgeSourceDeleteMutationChatHistoryFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutationChatHistoryMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26634033512955259";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutationChatHistoryMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeSourceDeleteMutationChatHistoryMutation";

    /**
     * Constructs a delete-chat-history-source mutation request.
     *
     * <p>The operation carries no variables, so the request holds no state.
     */
    public BizAiKnowledgeSourceDeleteMutationChatHistoryFacebookGraphQlRequest() {
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
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutation", exports = "deleteChatHistorySource",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        return "{}";
    }
}
