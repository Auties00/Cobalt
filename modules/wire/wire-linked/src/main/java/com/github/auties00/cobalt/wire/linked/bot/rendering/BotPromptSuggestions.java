package com.github.auties00.cobalt.wire.linked.bot.rendering;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * A container for a list of {@link BotPromptSuggestion} instances displayed
 * alongside an AI bot response.
 *
 * <p>This type wraps the repeated prompt suggestions that appear as
 * interactive chips below a bot message, allowing the user to quickly send a
 * follow-up query by tapping one of the suggestions.
 *
 * @see BotPromptSuggestion
 * @see BotSuggestedPromptMetadata
 */
@ProtobufMessage(name = "BotPromptSuggestions")
public final class BotPromptSuggestions {
    /**
     * The list of prompt suggestions to display to the user.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotPromptSuggestion> suggestions;


    /**
     * Constructs a new {@code BotPromptSuggestions} with the specified list.
     *
     * @param suggestions the list of prompt suggestions, or {@code null}
     */
    BotPromptSuggestions(List<BotPromptSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * Returns an unmodifiable view of the prompt suggestions.
     *
     * @return the list of prompt suggestions, never {@code null}
     */
    public List<BotPromptSuggestion> suggestions() {
        return suggestions == null ? List.of() : Collections.unmodifiableList(suggestions);
    }

    /**
     * Sets the list of prompt suggestions.
     *
     * @param suggestions the new list of suggestions, or {@code null}
     */
    public void setSuggestions(List<BotPromptSuggestion> suggestions) {
        this.suggestions = suggestions;
    }
}
