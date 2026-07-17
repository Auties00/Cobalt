package com.github.auties00.cobalt.wire.linked.bot.rendering;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata controlling how the AI bot response is rendered in the chat UI,
 * providing interactive keywords that link to follow-up prompts.
 *
 * <p>Each {@link Keyword} in this metadata represents a tappable element
 * within the rendered bot response. When the user taps a keyword, one of its
 * {@linkplain Keyword#associatedPrompts() associated prompts} can be sent as
 * a follow-up query to the bot.
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code renderingMetadata} field (protobuf index 16).
 */
@ProtobufMessage(name = "BotRenderingMetadata")
public final class BotRenderingMetadata {
    /**
     * The list of interactive keywords embedded in the bot response.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<Keyword> keywords;


    /**
     * Constructs a new {@code BotRenderingMetadata} with the specified keywords.
     *
     * @param keywords the list of interactive keywords, or {@code null}
     */
    BotRenderingMetadata(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    /**
     * Returns an unmodifiable view of the interactive keywords.
     *
     * @return the list of keywords, never {@code null}
     */
    public List<Keyword> keywords() {
        return keywords == null ? List.of() : Collections.unmodifiableList(keywords);
    }

    /**
     * Sets the list of interactive keywords.
     *
     * @param keywords the new list of keywords, or {@code null}
     */
    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    /**
     * An interactive keyword embedded in a bot response that maps to one or
     * more follow-up prompts the user can send by tapping the keyword.
     */
    @ProtobufMessage(name = "BotRenderingMetadata.Keyword")
    public static final class Keyword {
        /**
         * The display text of the keyword, for example {@code "weather"} or
         * {@code "recipe"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String value;

        /**
         * The follow-up prompt texts associated with this keyword, for example
         * {@code "What is the weather forecast for tomorrow?"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        List<String> associatedPrompts;


        /**
         * Constructs a new {@code Keyword} with the specified values.
         *
         * @param value             the keyword display text, or {@code null}
         * @param associatedPrompts the associated follow-up prompts, or {@code null}
         */
        Keyword(String value, List<String> associatedPrompts) {
            this.value = value;
            this.associatedPrompts = associatedPrompts;
        }

        /**
         * Returns the display text of the keyword.
         *
         * @return an {@code Optional} describing the keyword value, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Returns an unmodifiable view of the follow-up prompts associated
         * with this keyword.
         *
         * @return the list of associated prompts, never {@code null}
         */
        public List<String> associatedPrompts() {
            return associatedPrompts == null ? List.of() : Collections.unmodifiableList(associatedPrompts);
        }

        /**
         * Sets the display text of the keyword.
         *
         * @param value the new keyword value, or {@code null}
         */
        public void setValue(String value) {
            this.value = value;
    }

        /**
         * Sets the follow-up prompts associated with this keyword.
         *
         * @param associatedPrompts the new list of prompts, or {@code null}
         */
        public void setAssociatedPrompts(List<String> associatedPrompts) {
            this.associatedPrompts = associatedPrompts;
    }
    }
}
