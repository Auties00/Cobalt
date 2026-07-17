package com.github.auties00.cobalt.wire.linked.bot.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a slash-command registered on a WhatsApp AI bot's profile.
 *
 * <p>Bots advertise a set of slash-commands that users can invoke by
 * typing a forward slash followed by the command name (e.g.
 * {@code /imagine}, {@code /translate}). Each command has a name and an
 * optional description explaining what it does.
 *
 * <p>When a user sends a message starting with {@code /commandName}, the
 * message's body type should be set to {@code COMMAND} instead of
 * {@code PROMPT} to indicate that the message is a command invocation
 * rather than a free-text prompt.
 *
 * @see BotProfile#commands()
 * @see BotProfile#isCommand(String)
 */
@ProtobufMessage
public final class BotProfileCommand {
    /**
     * The command name without the leading slash
     * (e.g. {@code "imagine"}, {@code "translate"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String name;

    /**
     * A human-readable description of what this command does
     * (e.g. {@code "Generate an image from a text prompt"}).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String description;

    /**
     * Constructs a new {@code BotProfileCommand}. Prefer using the generated
     * {@code BotProfileCommandBuilder} instead of calling this constructor
     * directly.
     *
     * @param name        the command name without the leading slash,
     *                    must not be {@code null}
     * @param description a human-readable description of the command,
     *                    or {@code null} if none is available
     */
    BotProfileCommand(String name, String description) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
    }

    /**
     * Returns the command name without the leading slash.
     *
     * @return a non-{@code null} command name (e.g. {@code "imagine"})
     */
    public String name() {
        return name;
    }

    /**
     * Returns the human-readable description of this command.
     *
     * @return an {@code Optional} containing the description if present and
     *         non-empty, otherwise an empty {@code Optional}
     */
    public Optional<String> description() {
        return Optional.ofNullable(description).filter(d -> !d.isEmpty());
    }

    /**
     * Sets the command name (without the leading slash).
     *
     * @param name the command name, must not be {@code null}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the human-readable description of this command.
     *
     * @param description the description, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
