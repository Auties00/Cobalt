package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import com.github.auties00.cobalt.wire.linked.business.waa.ClientCapabilityMetadata;
import com.github.auties00.cobalt.wire.linked.business.waa.ModelRequestMetadata;
import java.util.List;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches the batched native machine-learning model manifest for a set
 * of requested models.
 *
 * <p>The operation takes two GraphQL variables: {@code model_request_metadatas}, a list of
 * {@link ModelRequestMetadata} naming the models and versions to resolve, and
 * {@code client_capability_metadata}, a {@link ClientCapabilityMetadata} describing the client's
 * decode capabilities. WhatsApp Web's
 * {@code WAWebNativeMLModelQuery.getNativeMLModel(modelRequestMetadatas, clientCapabilityMetadata)}
 * forwards both to the graph.whatsapp.com endpoint. The graph.whatsapp.com endpoint returns the model
 * manifest under {@code aim_model_batched_manifest}; the reply is consumed through
 * {@link NativeMlModelWhatsAppGraphQlResponse}.
 *
 * @implNote The only construction observed in the bundle ({@code WAWebBweMLModelManager}) builds each
 * list element as {@code {"name": <modelName>, "version": <version>}} and the capability object as
 * {@code {"bytecodeVersion": [...]}} with an always-empty array; this implementation mirrors those
 * shapes, keeping the {@code bytecodeVersion} key camelCase to match the wire.
 *
 * @see NativeMlModelWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebNativeMLModelQuery")
public final class NativeMlModelWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     *
     * @implNote This implementation ships the live {@code WAWebGraphQLPersistedQueries} numeric id
     * rather than the compiled {@code params.id} literal ({@code "32743078615336512"}); the
     * persisted-query map overrides that literal at dispatch time on WhatsApp Web.
     */
    @WhatsAppWebExport(moduleName = "WAWebNativeMLModelQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9175958945830972";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebNativeMLModelQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebNativeMLModelQuery";

    /**
     * The {@code model_request_metadatas} list naming the models to resolve, written only when
     * non-empty. Never {@code null} after construction.
     */
    private final List<ModelRequestMetadata> modelRequestMetadatas;

    /**
     * The {@code client_capability_metadata} object describing the client's decode capabilities, or
     * {@code null} to omit it.
     */
    private final ClientCapabilityMetadata clientCapabilityMetadata;

    /**
     * Constructs a native-ML-model query request carrying the model request metadatas and the client
     * capability metadata.
     *
     * <p>An empty {@code modelRequestMetadatas} list and a {@code null} {@code clientCapabilityMetadata}
     * each omit the corresponding variable from the serialized object.
     *
     * @param modelRequestMetadatas    the models to resolve, written only when non-empty
     * @param clientCapabilityMetadata the client decode capabilities, or {@code null} to omit the
     *                                 variable
     */
    public NativeMlModelWhatsAppGraphQlRequest(List<ModelRequestMetadata> modelRequestMetadatas, ClientCapabilityMetadata clientCapabilityMetadata) {
        this.modelRequestMetadatas = modelRequestMetadatas == null ? List.of() : List.copyOf(modelRequestMetadatas);
        this.clientCapabilityMetadata = clientCapabilityMetadata;
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
     * @implNote This implementation emits {@code {"model_request_metadatas": [{"name": ..., "version":
     * ...}, ...], "client_capability_metadata": {"bytecodeVersion": [...]}}}, writing
     * {@code model_request_metadatas} only when the list is non-empty and
     * {@code client_capability_metadata} only when it is non-null, and emitting {@code "{}"} when both
     * are absent.
     */
    @WhatsAppWebExport(moduleName = "WAWebNativeMLModelQuery", exports = "getNativeMLModel",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (!modelRequestMetadatas.isEmpty()) {
                writer.writeName("model_request_metadatas");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < modelRequestMetadatas.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    var metadata = modelRequestMetadatas.get(i);
                    writer.startObject();
                    metadata.name().ifPresent(value -> {
                        writer.writeName("name");
                        writer.writeColon();
                        writer.writeString(value);
                    });
                    metadata.version().ifPresent(value -> {
                        writer.writeName("version");
                        writer.writeColon();
                        writer.writeString(value);
                    });
                    writer.endObject();
                }
                writer.endArray();
            }

            if (clientCapabilityMetadata != null) {
                writer.writeName("client_capability_metadata");
                writer.writeColon();
                writer.startObject();
                writer.writeName("bytecodeVersion");
                writer.writeColon();
                writer.startArray();
                var versions = clientCapabilityMetadata.bytecodeVersion();
                for (var i = 0; i < versions.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeInt32(versions.get(i));
                }
                writer.endArray();
                writer.endObject();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.WWW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
