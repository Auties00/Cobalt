package com.github.auties00.cobalt.wire.graphql.whatsapp.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import java.util.Optional;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the graph.whatsapp.com GraphQL query that fetches Meta AI search type-ahead suggestions for a partial query.
 *
 * <p>The operation takes a single {@code param} GraphQL variable of type
 * {@code XWAMetaAISearchTypeaheadInput}. WhatsApp Web's
 * {@code WAWebFetchMetaAISearchTypeAheadSuggestions} fills it from
 * {@code WAWebGetMetaAISearchSuggestionsAction}: a {@code query} prefix string, the current
 * {@code locale}, the optional {@code exp_config} experiment-bucket list pulled from the
 * {@code ai_experiment_graphql_config} AB property, and the {@code capabilities} list (for example
 * {@code "TEXT"}). The graph.whatsapp.com endpoint returns the suggestions under {@code xwa_genai_meta_ai_search_typeahead};
 * the reply is consumed through {@link FetchMetaAiSearchTypeAheadSuggestionsWhatsAppGraphQlResponse}.
 *
 * @see FetchMetaAiSearchTypeAheadSuggestionsWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchMetaAISearchTypeAheadSuggestionsQuery")
public final class FetchMetaAiSearchTypeAheadSuggestionsWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchTypeAheadSuggestionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "10099941310063078";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchTypeAheadSuggestionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchMetaAISearchTypeAheadSuggestionsQuery";

    /**
     * The {@code query} field of the {@code param} object carrying the partial search prefix, or
     * {@code null} to omit it.
     */
    private final String query;

    /**
     * The {@code locale} field of the {@code param} object carrying the requesting locale, or
     * {@code null} to omit it.
     */
    private final String locale;

    /**
     * The {@code exp_config} field of the {@code param} object carrying the experiment-bucket ids, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web derives these from the {@code ai_experiment_graphql_config} AB property as a
     * comma-separated list of integers.
     */
    private final List<Integer> expConfig;

    /**
     * The {@code capabilities} field of the {@code param} object enumerating the supported suggestion
     * modalities (for example {@code "TEXT"}), or {@code null} to omit it.
     */
    private final List<String> capabilities;

    /**
     * Constructs a Meta AI search type-ahead suggestions request.
     *
     * <p>The four values populate the corresponding fields of the {@code param} GraphQL object; each
     * value that is {@code null} omits its field from the serialized object.
     *
     * @param query        the partial search prefix, or {@code null} to omit the field
     * @param locale       the requesting locale, or {@code null} to omit the field
     * @param expConfig    the experiment-bucket ids, or {@code null} to omit the field
     * @param capabilities the supported suggestion modalities, or {@code null} to omit the field
     */
    public FetchMetaAiSearchTypeAheadSuggestionsWhatsAppGraphQlRequest(String query, String locale, List<Integer> expConfig, List<String> capabilities) {
        this.query = query;
        this.locale = locale;
        this.expConfig = expConfig;
        this.capabilities = capabilities;
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
     * {@code {"param": {"query": <query>, "locale": <locale>, "exp_config": [...], "capabilities": [...]}}},
     * writing each field only when its value is non-null and emitting {@code {"param": {}}} when all
     * four are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchTypeAheadSuggestions",
            exports = "fetchMetaAISearchTypeAheadSuggestions", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("param");
            writer.writeColon();
            writer.startObject();
            if (query != null) {
                writer.writeName("query");
                writer.writeColon();
                writer.writeString(query);
            }

            if (locale != null) {
                writer.writeName("locale");
                writer.writeColon();
                writer.writeString(locale);
            }

            if (expConfig != null) {
                writer.writeName("exp_config");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < expConfig.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeInt32(expConfig.get(i));
                }
                writer.endArray();
            }

            if (capabilities != null) {
                writer.writeName("capabilities");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < capabilities.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(capabilities.get(i));
                }
                writer.endArray();
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
