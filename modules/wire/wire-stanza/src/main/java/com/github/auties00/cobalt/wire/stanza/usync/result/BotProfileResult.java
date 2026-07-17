package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the success result of the bot-profile USync parser.
 *
 * Surfaced by USync queries that request the bot-profile protocol; it carries
 * the Meta AI or third-party bot metadata used by the bot picker, the inline
 * command picker, and the "posing as professional" badge. Several wire fields
 * are nullable ({@link #isMetaCreated()}, {@link #creatorName()},
 * {@link #creatorProfileUrl()}, {@link #posingAsProfessional()}); the
 * remainder default to the empty string or the empty list when the relay omits
 * the child element.
 *
 * @implNote
 * This implementation copies the {@link Prompt} and {@link Command} lists at
 * construction time and exposes them as unmodifiable {@link List} snapshots.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncBotProfile")
public final class BotProfileResult implements UsyncProtocolResponse {
    /**
     * Holds the bot's display name.
     *
     * Defaults to the empty string when the {@code <name>} child is missing or
     * empty.
     */
    private final String name;

    /**
     * Holds the opaque {@code <attributes>} content carried through verbatim
     * for the rendering tier.
     *
     * Defaults to the empty string when the child is missing.
     */
    private final String attributes;

    /**
     * Holds the bot's description.
     *
     * Defaults to the empty string when the {@code <description>} child is
     * missing.
     */
    private final String description;

    /**
     * Holds the bot's category label.
     *
     * Defaults to the empty string when the {@code <category>} child is
     * missing.
     */
    private final String category;

    /**
     * Indicates whether the bot is marked as the default suggestion via the
     * {@code <default>true</default>} child.
     */
    private final boolean isDefault;

    /**
     * Holds the starter prompts the bot suggests for new conversations.
     *
     * Defaults to the empty list when the {@code <prompts>} child is missing.
     */
    private final List<Prompt> prompts;

    /**
     * Holds the bot's persona identifier from the {@code persona_id} attribute
     * on {@code <profile>}.
     *
     * Defaults to the empty string when the attribute is absent.
     */
    private final String personaId;

    /**
     * Holds the slash-commands published by the bot.
     *
     * Defaults to the empty list when the {@code <commands>} child is missing.
     */
    private final List<Command> commands;

    /**
     * Holds the free-form blurb shown above the command list.
     *
     * Defaults to the empty string when the {@code <description>} child of
     * {@code <commands>} is missing.
     */
    private final String commandsDescription;

    /**
     * Holds whether Meta authored the bot, decoded from
     * {@code <is_meta_created>true</is_meta_created>}.
     *
     * Is {@code null} when the child element is absent.
     */
    private final Boolean isMetaCreated;

    /**
     * Holds the human creator's display name from the {@code <name>} child
     * inside {@code <creator>}.
     *
     * Is {@code null} when the {@code <creator>} block is absent.
     */
    private final String creatorName;

    /**
     * Holds the human creator's profile URL from the {@code <profile_url>}
     * child inside {@code <creator>}.
     *
     * Is {@code null} when absent.
     */
    private final String creatorProfileUrl;

    /**
     * Holds the classification decoded from the {@code type} attribute on
     * {@code <posing_as_professional>}.
     *
     * Is {@code null} when the child is absent.
     */
    private final PosingAsProfessional posingAsProfessional;

    /**
     * Creates a new bot-profile result.
     *
     * @param name                 the {@code <name>} content; must not be
     *                             {@code null}
     * @param attributes           the {@code <attributes>} content; must not
     *                             be {@code null}
     * @param description          the {@code <description>} content; must not
     *                             be {@code null}
     * @param category             the {@code <category>} content; must not be
     *                             {@code null}
     * @param isDefault            whether the bot is the default suggestion
     * @param prompts              the {@link Prompt} list, or {@code null} for
     *                             an empty list
     * @param personaId            the {@code persona_id} attribute; must not
     *                             be {@code null}
     * @param commands             the {@link Command} list, or {@code null}
     *                             for an empty list
     * @param commandsDescription  the {@code <commands>}/{@code <description>}
     *                             content; must not be {@code null}
     * @param isMetaCreated        whether Meta authored the bot, or
     *                             {@code null} when the wire element is absent
     * @param creatorName          the human creator name, or {@code null}
     * @param creatorProfileUrl    the human creator profile URL, or
     *                             {@code null}
     * @param posingAsProfessional the {@link PosingAsProfessional}
     *                             classification, or {@code null}
     */
    public BotProfileResult(
            String name,
            String attributes,
            String description,
            String category,
            boolean isDefault,
            List<Prompt> prompts,
            String personaId,
            List<Command> commands,
            String commandsDescription,
            Boolean isMetaCreated,
            String creatorName,
            String creatorProfileUrl,
            PosingAsProfessional posingAsProfessional) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.isDefault = isDefault;
        this.prompts = prompts == null ? List.of() : List.copyOf(prompts);
        this.personaId = Objects.requireNonNull(personaId, "personaId cannot be null");
        this.commands = commands == null ? List.of() : List.copyOf(commands);
        this.commandsDescription = Objects.requireNonNull(commandsDescription, "commandsDescription cannot be null");
        this.isMetaCreated = isMetaCreated;
        this.creatorName = creatorName;
        this.creatorProfileUrl = creatorProfileUrl;
        this.posingAsProfessional = posingAsProfessional;
    }

    /**
     * Returns the bot's display name.
     *
     * @return the display name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the opaque attributes payload.
     *
     * The format is bot-defined and is forwarded verbatim to the rendering
     * tier rather than parsed.
     *
     * @return the attributes string, never {@code null}
     */
    public String attributes() {
        return attributes;
    }

    /**
     * Returns the bot's free-form description.
     *
     * @return the description, never {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Returns the bot's category label.
     *
     * Values are bot-defined strings supplied by the relay.
     *
     * @return the category, never {@code null}
     */
    public String category() {
        return category;
    }

    /**
     * Returns whether the bot is the default suggestion.
     *
     * @return {@code true} when {@code <default>true</default>} is present
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the starter prompts.
     *
     * @return the prompts, never {@code null}
     */
    public List<Prompt> prompts() {
        return prompts;
    }

    /**
     * Returns the persona identifier.
     *
     * Passes back into a subsequent query when refreshing the profile for a
     * specific persona.
     *
     * @return the persona id, never {@code null}
     */
    public String personaId() {
        return personaId;
    }

    /**
     * Returns the published slash-commands.
     *
     * @return the commands, never {@code null}
     */
    public List<Command> commands() {
        return commands;
    }

    /**
     * Returns the blurb shown above the slash-command list.
     *
     * Defaults to empty when the bot has no commands or does not provide a
     * header.
     *
     * @return the description, never {@code null}
     */
    public String commandsDescription() {
        return commandsDescription;
    }

    /**
     * Returns whether Meta authored the bot, when present.
     *
     * Empty when the relay did not include the wire element, which is treated
     * as "unknown" rather than "false".
     *
     * @return the meta-created flag, or empty when absent
     */
    public Optional<Boolean> isMetaCreated() {
        return Optional.ofNullable(isMetaCreated);
    }

    /**
     * Returns the human creator's display name, when present.
     *
     * Present only for third-party bots that declare a creator block.
     *
     * @return the creator name, or empty when absent
     */
    public Optional<String> creatorName() {
        return Optional.ofNullable(creatorName);
    }

    /**
     * Returns the human creator's profile URL, when present.
     *
     * Present only for third-party bots that declare a creator block.
     *
     * @return the creator profile URL, or empty when absent
     */
    public Optional<String> creatorProfileUrl() {
        return Optional.ofNullable(creatorProfileUrl);
    }

    /**
     * Returns the {@link PosingAsProfessional} classification, when present.
     *
     * Drives the "posing as professional" warning badge shown on bots that
     * impersonate a real-world professional. Absent when the relay did not
     * include the {@code <posing_as_professional>} child.
     *
     * @return the classification, or empty when absent
     */
    public Optional<PosingAsProfessional> posingAsProfessional() {
        return Optional.ofNullable(posingAsProfessional);
    }

    /**
     * Holds one starter prompt the UI shows for the user to send to the bot.
     *
     * Each prompt is a small {@code (emoji, text)} pair; both halves may be the
     * empty string when the relay omitted the corresponding child.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncBotProfile")
    public static final class Prompt {
        /**
         * Holds the leading emoji glyph from the {@code <emoji>} child.
         *
         * Defaults to the empty string when absent.
         */
        private final String emoji;

        /**
         * Holds the prompt text from the {@code <text>} child.
         *
         * Defaults to the empty string when absent.
         */
        private final String text;

        /**
         * Creates a new prompt.
         *
         * @param emoji the emoji glyph; must not be {@code null}
         * @param text  the prompt text; must not be {@code null}
         */
        public Prompt(String emoji, String text) {
            this.emoji = Objects.requireNonNull(emoji, "emoji cannot be null");
            this.text = Objects.requireNonNull(text, "text cannot be null");
        }

        /**
         * Returns the leading emoji glyph.
         *
         * @return the emoji, never {@code null}
         */
        public String emoji() {
            return emoji;
        }

        /**
         * Returns the prompt text.
         *
         * @return the text, never {@code null}
         */
        public String text() {
            return text;
        }
    }

    /**
     * Holds one slash-command published by the bot.
     *
     * Drives one row of the inline command picker that opens when the user
     * types {@code /} in a chat with the bot.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncBotProfile")
    public static final class Command {
        /**
         * Holds the slash-command identifier from the {@code <name>} child.
         */
        private final String name;

        /**
         * Holds the picker description from the {@code <description>} child.
         */
        private final String description;

        /**
         * Creates a new command descriptor.
         *
         * @param name        the slash-command identifier; must not be
         *                    {@code null}
         * @param description the picker description; must not be {@code null}
         */
        public Command(String name, String description) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.description = Objects.requireNonNull(description, "description cannot be null");
        }

        /**
         * Returns the slash-command identifier.
         *
         * The token the user types after {@code /} in the composer.
         *
         * @return the identifier, never {@code null}
         */
        public String name() {
            return name;
        }

        /**
         * Returns the picker description.
         *
         * @return the description, never {@code null}
         */
        public String description() {
            return description;
        }
    }

    /**
     * Classifies the {@code type} attribute of the
     * {@code <posing_as_professional>} child into a tristate.
     *
     * Drives the "may impersonate a professional" UI badge.
     */
    @WhatsAppWebModule(moduleName = "WAWebBotTypes")
    public enum PosingAsProfessional {
        /**
         * Represents wire value {@code type="unknown"}: the relay did not
         * classify the bot.
         */
        UNKNOWN,
        /**
         * Represents wire value {@code type="yes"}: the bot is flagged as
         * posing as a professional.
         */
        YES,
        /**
         * Represents wire value {@code type="no"}: the bot is explicitly not
         * posing as a professional.
         */
        NO
    }
}
