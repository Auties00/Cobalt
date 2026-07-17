package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.ai.AiChatHistoryUploadRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that creates a chat-history backup for a WhatsApp Business AI agent.
 *
 * <p>The mutation takes one GraphQL variable, {@code input}, which the Meta graph endpoint forwards as the
 * {@code request} argument of {@code xfb_maiba_create_chat_history}. WhatsApp Web's
 * {@code WAWebBizAiChatHistoryCreateMutation.createChatHistoryBackup(input)} passes the upload request
 * through as the {@code input} variable; the upload request is an {@link AiChatHistoryUploadRequest}
 * grouping past conversations by the customer they were held with. The Meta graph endpoint returns the
 * create outcome under the linked {@code xfb_maiba_create_chat_history} field; the reply is consumed
 * through {@link BizAiChatHistoryCreateFacebookGraphQlResponse}.
 *
 * @see BizAiChatHistoryCreateFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiChatHistoryCreateMutation")
public final class BizAiChatHistoryCreateFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiChatHistoryCreateMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "27499829029619003";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiChatHistoryCreateMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiChatHistoryCreateMutation";

    /**
     * The {@code input} GraphQL variable carrying the chat-history upload request forwarded as the
     * field's {@code request} argument, or {@code null} to omit it.
     */
    private final AiChatHistoryUploadRequest input;

    /**
     * Constructs a create-chat-history mutation request.
     *
     * <p>The {@code input} is the chat-history upload request the Meta graph endpoint forwards as the
     * {@code request} argument. A {@code null} value omits the variable from the serialized object,
     * matching the dispatcher's empty-variables branch.
     *
     * @param input the chat-history upload request, or {@code null} to omit the variable
     */
    public BizAiChatHistoryCreateFacebookGraphQlRequest(AiChatHistoryUploadRequest input) {
        this.input = input;
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
     * @implNote This implementation emits {@code {"input": {"threads": [...]}}}, writing the variable
     * only when the upload request is non-null and emitting {@code "{}"} otherwise. The camelCase model
     * is mapped to its snake_case shape by
     * {@link BizAiInputJson#writeChatHistoryUploadRequest(JSONWriter, AiChatHistoryUploadRequest)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiChatHistoryCreateMutation", exports = "createChatHistoryBackup",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                BizAiInputJson.writeChatHistoryUploadRequest(writer, input);
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
