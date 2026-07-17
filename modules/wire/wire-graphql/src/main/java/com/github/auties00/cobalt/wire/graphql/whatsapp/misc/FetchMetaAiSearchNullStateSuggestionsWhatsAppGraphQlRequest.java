package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

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
 * Builds the graph.whatsapp.com GraphQL query that fetches Meta AI search null-state suggestions, the suggested queries
 * shown before the user has typed anything into the AI search box.
 *
 * <p>The operation takes three top-level GraphQL variables, mapped directly without an enclosing
 * input object. WhatsApp Web's {@code WAWebFetchMetaAISearchNullStateSuggestions} fills them from
 * {@code WAWebGetMetaAISearchSuggestionsAction}: the current {@code locale}, a
 * {@code null_state_source} naming the surface that requested the suggestions (observed value
 * {@code "SEARCH"}), and the optional {@code exp_config} experiment-bucket list derived from the
 * {@code ai_experiment_graphql_config} AB property. The graph.whatsapp.com endpoint returns the suggestions under
 * {@code xwa_genai_meta_ai_search_null_state}; the reply is consumed through
 * {@link FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse}.
 *
 * @see FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchMetaAISearchNullStateSuggestionsQuery")
public final class FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchNullStateSuggestionsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9962874563796224";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchNullStateSuggestionsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchMetaAISearchNullStateSuggestionsQuery";

    /**
     * The {@code locale} variable carrying the requesting locale, or {@code null} to omit it.
     */
    private final String locale;

    /**
     * The {@code null_state_source} variable naming the surface that requested the suggestions, or
     * {@code null} to omit it.
     *
     * <p>WhatsApp Web sets this to {@code "SEARCH"} from the search action; the value set is not
     * declared as a closed enum in the bundle, so it is carried as a free-form {@link String}.
     */
    private final String nullStateSource;

    /**
     * The {@code exp_config} variable carrying the experiment-bucket ids, or {@code null} to omit it.
     *
     * <p>WhatsApp Web derives these from the {@code ai_experiment_graphql_config} AB property by
     * splitting its comma-separated value into integers.
     */
    private final List<Integer> expConfig;

    /**
     * Constructs a Meta AI search null-state suggestions request.
     *
     * <p>The three values populate the corresponding top-level GraphQL variables; each value that is
     * {@code null} omits its variable from the serialized object.
     *
     * @param locale          the requesting locale, or {@code null} to omit the variable
     * @param nullStateSource the surface that requested the suggestions, or {@code null} to omit the
     *                        variable
     * @param expConfig       the experiment-bucket ids, or {@code null} to omit the variable
     */
    public FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlRequest(String locale, String nullStateSource, List<Integer> expConfig) {
        this.locale = locale;
        this.nullStateSource = nullStateSource;
        this.expConfig = expConfig;
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
     * {@code {"locale": <locale>, "null_state_source": <nullStateSource>, "exp_config": [...]}},
     * writing each variable only when its value is non-null and emitting the empty object {@code "{}"}
     * when all three are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchMetaAISearchNullStateSuggestions",
            exports = "fetchMetaAISearchNullStateSuggestions", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (locale != null) {
                writer.writeName("locale");
                writer.writeColon();
                writer.writeString(locale);
            }

            if (nullStateSource != null) {
                writer.writeName("null_state_source");
                writer.writeColon();
                writer.writeString(nullStateSource);
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
