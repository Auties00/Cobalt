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
 * Builds the Facebook GraphQL mutation that triggers file-knowledge extraction for a WhatsApp
 * Business AI agent.
 *
 * <p>The single {@code input} GraphQL variable is the file-extraction trigger object. WhatsApp Web's
 * {@code WAWebBizAiKnowledgeSourceUploadMutation.triggerFileExtraction(manifoldFilePath,
 * userProvidedFileName)} fills it with the {@code manifold_file_path} naming the previously uploaded
 * source file on Meta's Manifold blob store and the {@code user_provided_file_name} the agent owner
 * typed when uploading. The Meta graph endpoint returns the extraction outcome under
 * {@code xfb_maiba_trigger_file_knowledge_extraction}; the reply is consumed through
 * {@link BizAiKnowledgeSourceUploadFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeSourceUploadFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourceUploadMutation")
public final class BizAiKnowledgeSourceUploadFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceUploadMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26994916406807814";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceUploadMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeSourceUploadMutation";

    /**
     * The {@code manifold_file_path} field of the {@code input} object naming the uploaded source file
     * on Meta's Manifold blob store, or {@code null} to omit it.
     */
    private final String manifoldFilePath;

    /**
     * The {@code user_provided_file_name} field of the {@code input} object carrying the file name the
     * agent owner typed when uploading, or {@code null} to omit it.
     */
    private final String userProvidedFileName;

    /**
     * Constructs a file-knowledge-extraction trigger request.
     *
     * <p>Both values populate the {@code input} GraphQL object; each value that is {@code null} is
     * omitted from the serialized object.
     *
     * @param manifoldFilePath     the Manifold path of the uploaded source file, or {@code null} to
     *                             omit the field
     * @param userProvidedFileName the user-supplied file name, or {@code null} to omit the field
     */
    public BizAiKnowledgeSourceUploadFacebookGraphQlRequest(String manifoldFilePath, String userProvidedFileName) {
        this.manifoldFilePath = manifoldFilePath;
        this.userProvidedFileName = userProvidedFileName;
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
     * @implNote This implementation emits {@code {"input": {"manifold_file_path": <manifoldFilePath>,
     * "user_provided_file_name": <userProvidedFileName>}}}, writing each field only when its value is
     * non-null and emitting {@code {"input": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceUploadMutation", exports = "triggerFileExtraction",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (manifoldFilePath != null) {
                writer.writeName("manifold_file_path");
                writer.writeColon();
                writer.writeString(manifoldFilePath);
            }

            if (userProvidedFileName != null) {
                writer.writeName("user_provided_file_name");
                writer.writeColon();
                writer.writeString(userProvidedFileName);
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
