package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * The empty-state suggestions shown in Meta AI search before the user types.
 *
 * <p>When the user opens AI search but has not yet entered a query, WhatsApp
 * fills the box with a list of suggested-query tiles to help the user get
 * started. This model is that list of {@linkplain MetaAiSearchSuggestion
 * suggestion tiles} as the server returns it for the requesting surface and
 * locale.
 */
@ProtobufMessage(name = "MetaAiSearchSuggestions")
public final class MetaAiSearchSuggestions {
    /**
     * Suggested-query tiles to show in the empty state, in the order the server
     * returned them. Never {@code null}, possibly empty when the server
     * returned none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<MetaAiSearchSuggestion> suggestions;

    /**
     * Constructs a new {@code MetaAiSearchSuggestions}. A {@code null}
     * {@code suggestions} is coerced to an empty list.
     *
     * @param suggestions the suggestion tiles; {@code null} treated as empty
     */
    MetaAiSearchSuggestions(List<MetaAiSearchSuggestion> suggestions) {
        this.suggestions = suggestions == null ? List.of() : suggestions;
    }

    /**
     * Returns the suggested-query tiles to show in the empty state.
     *
     * @return an unmodifiable view of the suggestion tiles; never {@code null},
     *         possibly empty
     */
    public List<MetaAiSearchSuggestion> suggestions() {
        return Collections.unmodifiableList(suggestions);
    }
}
