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
 * Builds the Facebook GraphQL mutation that deletes an uploaded-file business-AI knowledge source.
 *
 * <p>The mutation takes a single {@code input} GraphQL variable. WhatsApp Web's
 * {@code WAWebBizAiKnowledgeSourceDeleteMutation.deleteFileSource(id)} builds the input as
 * {@code {input: {uploaded_file_data_source_id: id}}}, where the id is the opaque data-source
 * identifier the upload flow assigned to the file, not a WhatsApp address, so it is modelled as a
 * {@link String}. The Meta graph endpoint returns the deletion outcome under the linked field
 * {@code xfb_maiba_trigger_file_deletion}; the reply is consumed through
 * {@link BizAiKnowledgeSourceDeleteFacebookGraphQlResponse}.
 *
 * @see BizAiKnowledgeSourceDeleteFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutation")
public final class BizAiKnowledgeSourceDeleteFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26730550573239991";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizAiKnowledgeSourceDeleteMutation";

    /**
     * The {@code uploaded_file_data_source_id} field of the {@code input} object naming the
     * uploaded-file data source to delete, or {@code null} to omit it.
     */
    private final String uploadedFileDataSourceId;

    /**
     * Constructs a delete-file-source mutation request.
     *
     * <p>The {@code uploadedFileDataSourceId} populates the {@code input} object's
     * {@code uploaded_file_data_source_id} field; a {@code null} value omits the field from the
     * serialized object.
     *
     * @param uploadedFileDataSourceId the uploaded-file data-source id to delete, or {@code null} to
     *                                 omit the field
     */
    public BizAiKnowledgeSourceDeleteFacebookGraphQlRequest(String uploadedFileDataSourceId) {
        this.uploadedFileDataSourceId = uploadedFileDataSourceId;
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
     * @implNote This implementation emits
     * {@code {"input": {"uploaded_file_data_source_id": <id>}}}, writing the inner field only when it
     * is non-null and emitting {@code {"input": {}}} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizAiKnowledgeSourceDeleteMutation", exports = "deleteFileSource",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (uploadedFileDataSourceId != null) {
                writer.writeName("uploaded_file_data_source_id");
                writer.writeColon();
                writer.writeString(uploadedFileDataSourceId);
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
