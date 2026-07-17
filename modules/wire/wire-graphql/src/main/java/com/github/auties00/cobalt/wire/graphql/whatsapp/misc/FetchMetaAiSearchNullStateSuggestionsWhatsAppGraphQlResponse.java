package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchSuggestion;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchSuggestionBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchSuggestions;
import com.github.auties00.cobalt.wire.linked.business.ai.MetaAiSearchSuggestionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the Meta AI search null-state suggestions query built by
 * {@link FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlRequest} into a {@link MetaAiSearchSuggestions}.
 *
 * <p>Reads the linked {@code xwa_genai_meta_ai_search_null_state} field and projects its suggestion
 * tiles onto the Cobalt domain model: each tile pairs the displayed suggestion copy with the query to
 * run when it is tapped and the originating session id.
 *
 * @see FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebFetchMetaAISearchNullStateSuggestionsQuery")
public final class FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed suggestions result.
     */
    private final MetaAiSearchSuggestions suggestions;

    /**
     * Constructs a response wrapping the parsed suggestions result.
     *
     * <p>Reserved for the static parser.
     *
     * @param suggestions the parsed suggestions result, or {@code null} when the graph.whatsapp.com endpoint omitted the
     *                    field
     */
    private FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse(MetaAiSearchSuggestions suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_genai_meta_ai_search_null_state} and projects its tiles onto
     * a {@link MetaAiSearchSuggestions}; the returned {@link Optional} is empty when {@code data} or the
     * null-state object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the null-state object is missing
     */
    public static Optional<FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var nullState = data.getJSONObject("xwa_genai_meta_ai_search_null_state");
        if (nullState == null) {
            return Optional.empty();
        }

        var suggestions = new MetaAiSearchSuggestionsBuilder()
                .suggestions(parseSuggestions(nullState.getJSONArray("suggestions")))
                .build();
        return Optional.of(new FetchMetaAiSearchNullStateSuggestionsWhatsAppGraphQlResponse(suggestions));
    }

    /**
     * Projects the {@code suggestions} array onto a list of {@link MetaAiSearchSuggestion}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<MetaAiSearchSuggestion> parseSuggestions(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<MetaAiSearchSuggestion>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new MetaAiSearchSuggestionBuilder()
                    .suggestion(obj.getString("suggestion"))
                    .query(obj.getString("query"))
                    .sessionId(obj.getString("session_id"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed suggestions result.
     *
     * @return the parsed {@link MetaAiSearchSuggestions}, never {@code null}
     */
    public MetaAiSearchSuggestions suggestions() {
        return suggestions;
    }
}
