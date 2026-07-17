package com.github.auties00.cobalt.wire.linked.bot.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Represents a suggested prompt from a WhatsApp AI bot's profile, displayed
 * to help users start a conversation with the bot.
 *
 * <p>Each prompt consists of an optional emoji icon and a text suggestion.
 * In the WhatsApp UI, these prompts appear as tappable buttons or chips
 * that the user can select to send the suggested text to the bot without
 * having to type it manually.
 *
 * @see BotProfile#prompts()
 */
@ProtobufMessage
public final class BotProfilePrompt {
    /**
     * The emoji icon displayed alongside the prompt text to provide a visual
     * cue about the prompt's topic, such as a sparkle or paint palette emoji.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String emoji;

    /**
     * The suggested prompt text that the user can tap to send to the bot,
     * such as {@code "Tell me a joke"} or {@code "Write a poem about nature"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String text;

    /**
     * Constructs a new {@code BotProfilePrompt}. Prefer using the generated
     * {@code BotProfilePromptBuilder} instead of calling this constructor
     * directly.
     *
     * @param emoji the emoji icon for this prompt, or {@code null} if none
     * @param text  the suggested prompt text, or {@code null} if none
     */
    BotProfilePrompt(String emoji, String text) {
        this.emoji = emoji;
        this.text = text;
    }

    /**
     * Returns the emoji icon associated with this prompt.
     *
     * @return an {@code Optional} containing the emoji string if present
     *         and non-empty, or an empty {@code Optional}
     */
    public Optional<String> emoji() {
        return Optional.ofNullable(emoji).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the suggested prompt text that a user can send to the bot.
     *
     * @return an {@code Optional} containing the text if present and
     *         non-empty, or an empty {@code Optional}
     */
    public Optional<String> text() {
        return Optional.ofNullable(text).filter(s -> !s.isEmpty());
    }

    /**
     * Sets the emoji icon for this prompt.
     *
     * @param emoji the emoji string, or {@code null} to clear
     */
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    /**
     * Sets the suggested prompt text.
     *
     * @param text the prompt text, or {@code null} to clear
     */
    public void setText(String text) {
        this.text = text;
    }
}
