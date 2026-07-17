package com.github.auties00.cobalt.wire.linked.bot.rendering;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Metadata for the prompt suggestions displayed alongside an AI bot message,
 * including tracking of which suggestion the user selected.
 *
 * <p>This type supports two mutually exclusive formats for prompt
 * suggestions, each with its own builder:
 * <ul>
 * <li>{@code BotSuggestedPromptMetadataLegacyBuilder} — a flat list of
 *     plain-text suggestion strings ({@linkplain #suggestedPrompts()}),
 *     with selection tracked by {@linkplain #selectedPromptIndex() index}.
 * <li>{@code BotSuggestedPromptMetadataStructuredBuilder} — a list of
 *     {@link BotPromptSuggestion} objects with unique identifiers
 *     ({@linkplain #promptSuggestions()}), with selection tracked by
 *     {@linkplain #selectedPromptId() ID}.
 * </ul>
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code suggestedPromptMetadata} field (protobuf index 4).
 *
 * @see BotPromptSuggestions
 * @see BotPromptSuggestion
 */
@ProtobufMessage(name = "BotSuggestedPromptMetadata", generateBuilder = false)
public final class BotSuggestedPromptMetadata {
    /**
     * The list of plain-text suggested prompts (legacy format), for example
     * {@code ["Tell me more", "Show me an example"]}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    List<String> suggestedPrompts;

    /**
     * The zero-based index of the prompt the user selected from
     * {@link #suggestedPrompts()}, used with the legacy plain-text format.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer selectedPromptIndex;

    /**
     * The structured prompt suggestions with unique identifiers.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BotPromptSuggestions promptSuggestions;

    /**
     * The identifier of the prompt the user selected from the structured
     * {@link #promptSuggestions()}, for example {@code "prompt_abc123"}.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String selectedPromptId;


    /**
     * Constructs a new {@code BotSuggestedPromptMetadata} with all fields specified directly.
     *
     * <p>Callers should prefer the named builder factories
     * {@link #ofLegacy(List, Integer)} and {@link #ofStructured(BotPromptSuggestions, String)}
     * to ensure only one format is populated at a time.
     *
     * @param suggestedPrompts    the plain-text suggested prompts, or {@code null}
     * @param selectedPromptIndex the zero-based index of the selected prompt, or {@code null}
     * @param promptSuggestions   the structured prompt suggestions, or {@code null}
     * @param selectedPromptId    the identifier of the selected prompt, or {@code null}
     */
    BotSuggestedPromptMetadata(List<String> suggestedPrompts, Integer selectedPromptIndex, BotPromptSuggestions promptSuggestions, String selectedPromptId) {
        this.suggestedPrompts = suggestedPrompts;
        this.selectedPromptIndex = selectedPromptIndex;
        this.promptSuggestions = promptSuggestions;
        this.selectedPromptId = selectedPromptId;
    }

    /**
     * Constructs a {@code BotSuggestedPromptMetadata} using the legacy
     * plain-text format.
     *
     * <p>The structured fields ({@code promptSuggestions} and
     * {@code selectedPromptId}) are left unset.
     *
     * @param suggestedPrompts    the plain-text suggested prompts, or
     *        {@code null}
     * @param selectedPromptIndex the zero-based index of the prompt the
     *        user selected, or {@code null} if no selection was made
     * @return a new {@code BotSuggestedPromptMetadata} in legacy format
     */
    @ProtobufBuilder(className = "BotSuggestedPromptMetadataLegacyBuilder")
    static BotSuggestedPromptMetadata ofLegacy(List<String> suggestedPrompts, Integer selectedPromptIndex) {
        return new BotSuggestedPromptMetadata(suggestedPrompts, selectedPromptIndex, null, null);
    }

    /**
     * Constructs a {@code BotSuggestedPromptMetadata} using the
     * structured format with unique prompt identifiers.
     *
     * <p>The legacy fields ({@code suggestedPrompts} and
     * {@code selectedPromptIndex}) are left unset.
     *
     * @param promptSuggestions the structured prompt suggestions, or
     *        {@code null}
     * @param selectedPromptId  the identifier of the prompt the user
     *        selected, or {@code null} if no selection was made
     * @return a new {@code BotSuggestedPromptMetadata} in structured
     *         format
     */
    @ProtobufBuilder(className = "BotSuggestedPromptMetadataStructuredBuilder")
    static BotSuggestedPromptMetadata ofStructured(BotPromptSuggestions promptSuggestions, String selectedPromptId) {
        return new BotSuggestedPromptMetadata(null, null, promptSuggestions, selectedPromptId);
    }

    /**
     * Returns an unmodifiable view of the plain-text suggested prompts.
     *
     * @return the list of suggested prompts, never {@code null}
     */
    public List<String> suggestedPrompts() {
        return suggestedPrompts == null ? List.of() : Collections.unmodifiableList(suggestedPrompts);
    }

    /**
     * Returns the zero-based index of the prompt the user selected.
     *
     * @return an {@code OptionalInt} describing the selected index, or an
     *         empty {@code OptionalInt} if no selection was made
     */
    public OptionalInt selectedPromptIndex() {
        return selectedPromptIndex == null ? OptionalInt.empty() : OptionalInt.of(selectedPromptIndex);
    }

    /**
     * Returns the structured prompt suggestions.
     *
     * @return an {@code Optional} describing the structured suggestions, or an
     *         empty {@code Optional} if not set
     */
    public Optional<BotPromptSuggestions> promptSuggestions() {
        return Optional.ofNullable(promptSuggestions);
    }

    /**
     * Returns the identifier of the prompt the user selected.
     *
     * @return an {@code Optional} describing the selected prompt identifier, or
     *         an empty {@code Optional} if no selection was made
     */
    public Optional<String> selectedPromptId() {
        return Optional.ofNullable(selectedPromptId);
    }
}
