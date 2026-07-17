package com.github.auties00.cobalt.wire.linked.bot.profile;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.*;

/**
 * Represents the profile of a WhatsApp AI bot, containing the bot's
 * identity, display metadata, registered commands, suggested prompts,
 * and classification flags.
 *
 * <p>WhatsApp supports AI-powered bots (such as Meta AI) that users can
 * interact with in dedicated chat threads. Each bot has a profile that
 * describes its persona, capabilities, and creator. This class models all
 * of that information and is typically fetched from the server during
 * contact synchronization.
 *
 * <p>A bot profile includes:
 * <ul>
 * <li>Basic identity: {@link #jid()}, {@link #name()}, {@link #personaId()}
 * <li>Display information: {@link #description()}, {@link #category()},
 *     {@link #attributes()}
 * <li>Interaction hints: {@link #prompts()} for suggested conversation
 *     starters and {@link #commands()} for registered slash-commands
 * <li>Creator details: {@link #creatorName()},
 *     {@link #creatorProfileUrl()}
 * <li>Classification flags: {@link #isDefault()}, {@link #isMetaCreated()},
 *     {@link #professionalStatus()}
 * </ul>
 *
 * <p>Use {@link #isCommand(String)} to check whether a message text
 * starts with one of this bot's registered slash-commands.
 *
 * @see BotProfileCommand
 * @see BotProfilePrompt
 * @see BotProfileCategory
 * @see BotProfessionalStatus
 */
@ProtobufMessage
public final class BotProfile {
    /**
     * The bot's unique JID (Jabber Identifier), used to address this bot
     * in conversations and contact lookups.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The bot's display name as shown to users (e.g. {@code "Meta AI"}).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * An opaque, server-supplied attributes string associated with this bot
     * profile. The exact format and meaning of this string is determined by
     * the server and may vary between bot types. May be {@code null} if no
     * attributes were provided.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String attributes;

    /**
     * A human-readable description of what the bot does
     * (e.g. {@code "Ask me anything"}).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String description;

    /**
     * The character category of this bot's persona, indicating whether the
     * bot is a purely synthetic AI, a persona based on a living person, a
     * fictional character, or a historical figure.
     *
     * @see BotProfileCategory
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    BotProfileCategory category;

    /**
     * Whether this is the default Meta AI bot. When {@code true}, this bot
     * is the primary AI assistant and is typically surfaced prominently in
     * the WhatsApp UI, such as at the top of the chat list or in search
     * results.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    boolean isDefault;

    /**
     * Suggested prompts displayed to users to help them start a conversation
     * with this bot. Each prompt consists of an emoji and a text suggestion
     * that the user can tap to send. Defaults to an empty list if no prompts
     * are available.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    SequencedCollection<BotProfilePrompt> prompts;

    /**
     * The persona identifier for this bot profile. A single bot JID may
     * have multiple persona variants, such as the default persona, a
     * first-party character persona, or a user-generated persona. This
     * identifier distinguishes between them.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String personaId;

    /**
     * The bot's registered slash-commands, such as {@code /imagine} or
     * {@code /translate}. When a user sends a message starting with one
     * of these command names prefixed by a slash, the message's body type
     * should be set to {@code COMMAND} instead of {@code PROMPT}. Defaults
     * to an empty list if no commands are registered.
     *
     * @see #isCommand(String)
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    SequencedCollection<BotProfileCommand> commands;

    /**
     * A human-readable description of the commands section as a whole,
     * such as {@code "Available commands"}. This text can be displayed
     * as a heading or tooltip above the list of slash-commands.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String commandsDescription;

    /**
     * Whether this bot was created by Meta as a first-party AI. First-party
     * bots (such as Meta AI) are distinguished from third-party or
     * user-generated bots in the WhatsApp client.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    boolean isMetaCreated;

    /**
     * The display name of the entity or organization that created this bot,
     * such as {@code "Meta"}.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String creatorName;

    /**
     * The profile URL of the bot's creator, such as
     * {@code "https://www.meta.com"}. Can be displayed as a link to
     * provide more information about the bot's creator.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    URI creatorProfileUrl;

    /**
     * Whether this bot claims to act as a professional, such as a doctor,
     * lawyer, or financial advisor. May be {@code null} if the server did
     * not provide this classification.
     *
     * @see BotProfessionalStatus
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    BotProfessionalStatus professionalStatus;

    /**
     * Constructs a new {@code BotProfile} with all fields. Prefer using
     * the generated {@code BotProfileBuilder} instead of calling this
     * constructor directly.
     *
     * @param jid                  the bot's JID, must not be {@code null}
     * @param name                 the bot's display name, or {@code null} if unknown
     * @param attributes           the server-supplied attributes string, or {@code null}
     * @param description          a description of what the bot does, or {@code null}
     * @param category             the persona category, or {@code null} if not classified
     * @param isDefault            {@code true} if this is the default Meta AI bot
     * @param prompts              the suggested conversation starters, or {@code null}
     *                             for an empty list
     * @param personaId            the persona identifier, or {@code null}
     * @param commands             the registered slash-commands, or {@code null} for
     *                             an empty list
     * @param commandsDescription  a heading for the commands section, or {@code null}
     * @param isMetaCreated        {@code true} if Meta created this bot
     * @param creatorName          the creator's display name, or {@code null}
     * @param creatorProfileUrl    the creator's profile URL, or {@code null}
     * @param professionalStatus   the professional-status classification, or {@code null}
     */
    BotProfile(
            Jid jid,
            String name,
            String attributes,
            String description,
            BotProfileCategory category,
            boolean isDefault,
            SequencedCollection<BotProfilePrompt> prompts,
            String personaId,
            SequencedCollection<BotProfileCommand> commands,
            String commandsDescription,
            boolean isMetaCreated,
            String creatorName,
            URI creatorProfileUrl,
            BotProfessionalStatus professionalStatus
    ) {
        this.jid = Objects.requireNonNull(jid, "jid");
        this.name = name;
        this.attributes = attributes;
        this.description = description;
        this.category = category;
        this.isDefault = isDefault;
        this.prompts = prompts != null ? Collections.unmodifiableSequencedCollection(prompts) : List.of();
        this.personaId = personaId;
        this.commands = commands != null ? Collections.unmodifiableSequencedCollection(commands) : List.of();
        this.commandsDescription = commandsDescription;
        this.isMetaCreated = isMetaCreated;
        this.creatorName = creatorName;
        this.creatorProfileUrl = creatorProfileUrl;
        this.professionalStatus = professionalStatus;
    }

    /**
     * Returns the bot's unique JID, used to address this bot in conversations
     * and contact lookups.
     *
     * @return a non-{@code null} {@link Jid}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the bot's display name.
     *
     * @return an {@code Optional} containing the display name if present and
     *         non-empty, otherwise an empty {@code Optional}
     */
    public Optional<String> name() {
        return Optional.ofNullable(name).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the server-supplied attributes string associated with this bot
     * profile.
     *
     * @return an {@code Optional} containing the attributes string if present
     *         and non-empty, or an empty {@code Optional}
     */
    public Optional<String> attributes() {
        return Optional.ofNullable(attributes).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the bot's description.
     *
     * @return an {@code Optional} containing the description if present and
     *         non-empty, otherwise an empty {@code Optional}
     */
    public Optional<String> description() {
        return Optional.ofNullable(description).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the character category of this bot's persona, indicating
     * whether it is synthetic, based on a living person, fictional, or
     * historical.
     *
     * @return an {@code Optional} containing the {@link BotProfileCategory}
     *         if present, or an empty {@code Optional}
     */
    public Optional<BotProfileCategory> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns whether this is the default Meta AI bot, which is the primary
     * AI assistant prominently surfaced in the WhatsApp UI.
     *
     * @return {@code true} if this is the default bot, {@code false} otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the suggested prompts for this bot, which are conversation
     * starters displayed to help users begin interacting with the bot.
     *
     * @return an unmodifiable collection of {@link BotProfilePrompt}
     *         instances, never {@code null} (empty if no prompts are defined)
     */
    public SequencedCollection<BotProfilePrompt> prompts() {
        return prompts;
    }

    /**
     * Returns the persona identifier for this bot profile. A single bot JID
     * may have multiple persona variants; this ID distinguishes between them.
     *
     * @return an {@code Optional} containing the persona ID if present and
     *         non-empty, or an empty {@code Optional}
     */
    public Optional<String> personaId() {
        return Optional.ofNullable(personaId).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the bot's registered slash-commands. When a user sends a
     * message starting with {@code /commandName} and the command name
     * matches one returned here, the message body type should be set to
     * {@code COMMAND} instead of {@code PROMPT}.
     *
     * @return an unmodifiable collection of {@link BotProfileCommand}
     *         instances, never {@code null} (empty if no commands are
     *         registered)
     * @see #isCommand(String)
     */
    public SequencedCollection<BotProfileCommand> commands() {
        return commands;
    }

    /**
     * Returns the heading or description for the commands section, such as
     * {@code "Available commands"}.
     *
     * @return an {@code Optional} containing the description if present and
     *         non-empty, or an empty {@code Optional}
     */
    public Optional<String> commandsDescription() {
        return Optional.ofNullable(commandsDescription).filter(s -> !s.isEmpty());
    }

    /**
     * Returns whether this bot was created by Meta as a first-party AI.
     *
     * @return {@code true} if Meta created this bot, {@code false} otherwise
     */
    public boolean isMetaCreated() {
        return isMetaCreated;
    }

    /**
     * Returns the display name of the entity or organization that created
     * this bot, such as {@code "Meta"}.
     *
     * @return an {@code Optional} containing the creator name if present and
     *         non-empty, or an empty {@code Optional}
     */
    public Optional<String> creatorName() {
        return Optional.ofNullable(creatorName).filter(s -> !s.isEmpty());
    }

    /**
     * Returns the profile URL of the bot's creator, which can be displayed
     * as a link to provide more information about who built the bot.
     *
     * @return an {@code Optional} containing the {@link URI} if present,
     *         or an empty {@code Optional}
     */
    public Optional<URI> creatorProfileUrl() {
        return Optional.ofNullable(creatorProfileUrl);
    }

    /**
     * Returns the professional-status classification for this bot, indicating
     * whether it claims to act as a professional such as a doctor, lawyer,
     * or financial advisor.
     *
     * @return an {@code Optional} containing the {@link BotProfessionalStatus}
     *         if present, or an empty {@code Optional}
     */
    public Optional<BotProfessionalStatus> professionalStatus() {
        return Optional.ofNullable(professionalStatus);
    }

    /**
     * Checks whether the given message text starts with one of this bot's
     * registered slash-commands.
     *
     * <p>The text must begin with {@code /commandName} and either end there
     * or be followed by whitespace. For example, given a registered command
     * named {@code "imagine"}, the texts {@code "/imagine"} and
     * {@code "/imagine a sunset"} both match, but {@code "/imaginemore"}
     * does not.
     *
     * @param text the message text to test, may be {@code null}
     * @return {@code true} if the text starts with a registered command,
     *         {@code false} otherwise
     */
    public boolean isCommand(String text) {
        if (text == null || !text.startsWith("/") || commands.isEmpty()) {
            return false;
        }

        for (var command : commands) {
            var slash = "/" + command.name();
            if (text.startsWith(slash)
                    && (text.length() == slash.length() || Character.isWhitespace(text.charAt(slash.length())))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the bot's unique JID.
     *
     * @param jid the JID to set, must not be {@code null}
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * Sets the bot's display name.
     *
     * @param name the display name, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the server-supplied attributes string.
     *
     * @param attributes the attributes string, or {@code null} to clear
     */
    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    /**
     * Sets the bot's human-readable description.
     *
     * @param description the description, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the character category of this bot's persona.
     *
     * @param category the category, or {@code null} to clear
     */
    public void setCategory(BotProfileCategory category) {
        this.category = category;
    }

    /**
     * Sets whether this is the default Meta AI bot.
     *
     * @param isDefault {@code true} if this is the default bot
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Sets the suggested conversation starters for this bot.
     *
     * @param prompts the prompts collection, or {@code null} to clear
     */
    public void setPrompts(SequencedCollection<BotProfilePrompt> prompts) {
        this.prompts = prompts;
    }

    /**
     * Sets the persona identifier for this bot profile.
     *
     * @param personaId the persona ID, or {@code null} to clear
     */
    public void setPersonaId(String personaId) {
        this.personaId = personaId;
    }

    /**
     * Sets the registered slash-commands for this bot.
     *
     * @param commands the commands collection, or {@code null} to clear
     */
    public void setCommands(SequencedCollection<BotProfileCommand> commands) {
        this.commands = commands;
    }

    /**
     * Sets the heading or description for the commands section.
     *
     * @param commandsDescription the description, or {@code null} to clear
     */
    public void setCommandsDescription(String commandsDescription) {
        this.commandsDescription = commandsDescription;
    }

    /**
     * Sets whether this bot was created by Meta.
     *
     * @param isMetaCreated {@code true} if Meta created this bot
     */
    public void setMetaCreated(boolean isMetaCreated) {
        this.isMetaCreated = isMetaCreated;
    }

    /**
     * Sets the display name of the bot's creator.
     *
     * @param creatorName the creator name, or {@code null} to clear
     */
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    /**
     * Sets the profile URL of the bot's creator.
     *
     * @param creatorProfileUrl the profile URL, or {@code null} to clear
     */
    public void setCreatorProfileUrl(URI creatorProfileUrl) {
        this.creatorProfileUrl = creatorProfileUrl;
    }

    /**
     * Sets the professional-status classification for this bot.
     *
     * @param professionalStatus the status, or {@code null} to clear
     */
    public void setProfessionalStatus(BotProfessionalStatus professionalStatus) {
        this.professionalStatus = professionalStatus;
    }
}
