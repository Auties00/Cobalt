package com.github.auties00.cobalt.wire.linked.business.aichannel;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One slash-command advertised by a WhatsApp GenAI agent channel.
 *
 * <p>A GenAI agent connected to a WhatsApp channel can publish a catalog of
 * named slash-commands that followers may invoke from the chat input; tapping
 * one expands its underlying prompt template into the message the user sends
 * to the agent. This model is one such advertised command: its server-issued
 * {@linkplain #id() identifier}, the {@linkplain #name() invoked name},
 * the {@linkplain #description() description} shown in the picker, and the
 * {@linkplain #prompt() prompt template} the command expands into.
 */
@ProtobufMessage(name = "AiChannelCommand")
public final class AiChannelCommand {
    /**
     * Server-issued command identifier, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Command name (the slash-command token the user invokes), or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Human-readable command description shown in the picker, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String description;

    /**
     * Prompt template the command expands into when invoked, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String prompt;

    /**
     * Constructs a new {@code AiChannelCommand}. Any reference argument may
     * be {@code null} when the server omitted the corresponding field.
     *
     * @param id          the server-issued command identifier, or
     *                    {@code null}
     * @param name        the invoked command name, or {@code null}
     * @param description the human-readable description, or {@code null}
     * @param prompt      the prompt template, or {@code null}
     */
    AiChannelCommand(String id, String name, String description, String prompt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prompt = prompt;
    }

    /**
     * Returns the server-issued command identifier.
     *
     * @return the command identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the command name the user invokes.
     *
     * @return the command name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the human-readable command description shown in the picker.
     *
     * @return the description, or empty when the server omitted it
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the prompt template the command expands into when invoked.
     *
     * @return the prompt template, or empty when the server omitted it
     */
    public Optional<String> prompt() {
        return Optional.ofNullable(prompt);
    }
}
