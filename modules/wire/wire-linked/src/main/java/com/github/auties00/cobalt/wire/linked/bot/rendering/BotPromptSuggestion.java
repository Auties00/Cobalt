package com.github.auties00.cobalt.wire.linked.bot.rendering;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A single prompt suggestion that can be displayed to the user alongside an AI
 * bot response to encourage follow-up interaction.
 *
 * <p>Each suggestion consists of a human-readable {@linkplain #prompt() display text}
 * and a server-assigned {@linkplain #promptId() identifier} used to track which
 * suggestion the user selected.
 *
 * @see BotPromptSuggestions
 * @see BotSuggestedPromptMetadata
 */
@ProtobufMessage(name = "BotPromptSuggestion")
public final class BotPromptSuggestion {
    /**
     * The display text of the prompt suggestion shown to the user, for example
     * {@code "Tell me more about this topic"}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String prompt;

    /**
     * The server-assigned unique identifier for this prompt suggestion, for
     * example {@code "prompt_abc123"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String promptId;


    /**
     * Constructs a new {@code BotPromptSuggestion} with the specified values.
     *
     * @param prompt   the suggestion display text, or {@code null}
     * @param promptId the suggestion identifier, or {@code null}
     */
    BotPromptSuggestion(String prompt, String promptId) {
        this.prompt = prompt;
        this.promptId = promptId;
    }

    /**
     * Returns the display text of the prompt suggestion.
     *
     * @return an {@code Optional} describing the prompt text, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> prompt() {
        return Optional.ofNullable(prompt);
    }

    /**
     * Returns the unique identifier for this prompt suggestion.
     *
     * @return an {@code Optional} describing the prompt identifier, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> promptId() {
        return Optional.ofNullable(promptId);
    }

    /**
     * Sets the display text of the prompt suggestion.
     *
     * @param prompt the new prompt text, or {@code null}
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Sets the unique identifier for this prompt suggestion.
     *
     * @param promptId the new prompt identifier, or {@code null}
     */
    public void setPromptId(String promptId) {
        this.promptId = promptId;
    }
}
