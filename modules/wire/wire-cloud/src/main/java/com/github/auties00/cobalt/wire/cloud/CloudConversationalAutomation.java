package com.github.auties00.cobalt.wire.cloud;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API conversational automation configuration.
 *
 * <p>Conversational components let a business pre-configure how a chat opens and what shortcuts the
 * consumer sees: an optional welcome message shown when a consumer first messages the business, a set
 * of ice-breaker prompts offered as tappable suggestions, and a set of slash {@link Command commands}
 * the consumer can type. This model carries those three settings as written to and read from the
 * phone number's {@code conversational_automation} edge.
 */
public final class CloudConversationalAutomation {
    /**
     * Whether the welcome message is shown when a consumer first messages the business, or
     * {@code null} when the setting is unspecified.
     */
    private final Boolean enableWelcomeMessage;

    /**
     * The ice-breaker prompts offered as tappable suggestions.
     */
    private final List<String> prompts;

    /**
     * The slash commands the consumer can type.
     */
    private final List<Command> commands;

    /**
     * Constructs a new conversational automation configuration.
     *
     * @param enableWelcomeMessage whether the welcome message is enabled, or {@code null} to leave the
     *                             setting unspecified
     * @param prompts              the ice-breaker prompts, or {@code null} for none
     * @param commands             the slash commands, or {@code null} for none
     */
    public CloudConversationalAutomation(Boolean enableWelcomeMessage, List<String> prompts, List<Command> commands) {
        this.enableWelcomeMessage = enableWelcomeMessage;
        this.prompts = prompts == null ? List.of() : List.copyOf(prompts);
        this.commands = commands == null ? List.of() : List.copyOf(commands);
    }

    /**
     * Returns whether the welcome message is shown when a consumer first messages the business.
     *
     * @return an {@link Optional} carrying the welcome-message flag, or empty when unspecified
     */
    public Optional<Boolean> enableWelcomeMessage() {
        return Optional.ofNullable(enableWelcomeMessage);
    }

    /**
     * Returns the ice-breaker prompts offered as tappable suggestions.
     *
     * @return an unmodifiable list of prompts, empty when none were declared
     */
    public List<String> prompts() {
        return prompts;
    }

    /**
     * Returns the slash commands the consumer can type.
     *
     * @return an unmodifiable list of commands, empty when none were declared
     */
    public List<Command> commands() {
        return commands;
    }

    /**
     * A slash command offered to the consumer within a conversational automation configuration.
     *
     * <p>A command pairs a short name the consumer types after a leading slash with a human-readable
     * description shown alongside it. Command names are unique per phone number.
     */
    public static final class Command {
        /**
         * The command name typed after a leading slash.
         */
        private final String name;

        /**
         * The human-readable command description.
         */
        private final String description;

        /**
         * Constructs a new command.
         *
         * @param name        the command name typed after a leading slash
         * @param description the human-readable command description
         * @throws NullPointerException if {@code name} or {@code description} is {@code null}
         */
        public Command(String name, String description) {
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.description = Objects.requireNonNull(description, "description must not be null");
        }

        /**
         * Returns the command name typed after a leading slash.
         *
         * @return the command name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the human-readable command description.
         *
         * @return the command description
         */
        public String description() {
            return description;
        }
    }
}
