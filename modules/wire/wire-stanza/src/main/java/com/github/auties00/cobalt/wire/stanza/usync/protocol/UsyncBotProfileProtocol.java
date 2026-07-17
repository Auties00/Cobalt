package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.BotProfileResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Describes the USync {@code bot} protocol.
 *
 * This descriptor asks the relay for each peer's bot profile metadata
 * (display name, description, prompts, slash-commands, creator block, persona
 * id). The descriptor itself is stateless; per-user state, specifically the
 * persona id set through {@link UsyncUser#withPersonaId(String)}, lives on
 * each {@link UsyncUser} so a single instance can be paired with users that
 * target different personas of a multi-persona bot.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncBotProfile")
public final class UsyncBotProfileProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "bot";

    /**
     * Holds the wire-protocol version emitted on the {@code v} attribute of
     * the inner {@code <profile/>} query element.
     */
    public static final String PROFILE_VERSION = "1";

    /**
     * Creates a bot-profile-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBotProfile",
            exports = "USyncBotProfileProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncBotProfileProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBotProfile",
            exports = "USyncBotProfileProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation wraps a versioned {@code <profile v="1"/>} child
     * inside the {@code <bot>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBotProfile",
            exports = "USyncBotProfileProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder()
                .description(NAME)
                .content(List.of(new StanzaBuilder()
                        .description("profile")
                        .attribute("v", PROFILE_VERSION)
                        .build()))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always attaches a per-user {@code <bot>} child
     * carrying an inner {@code <profile/>} with the optional
     * {@code persona_id} attribute; the wrap is emitted unconditionally
     * whether or not a persona id is set.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBotProfile",
            exports = "USyncBotProfileProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        var inner = new StanzaBuilder().description("profile");
        user.personaId().ifPresent(p -> inner.attribute("persona_id", p));
        return Optional.of(new StanzaBuilder()
                .description(NAME)
                .content(List.of(inner.build()))
                .build());
    }

    /**
     * {@inheritDoc}
     *
     * This override reads every optional sub-element of the {@code <profile>}
     * child (name, attributes, description, category, default flag, prompts,
     * persona id, commands block with its description header, meta-created
     * flag, creator block, posing-as-professional type) and projects them
     * into the typed {@link BotProfileResult} record.
     *
     * @implNote
     * This implementation lets missing nodes fall back to empty strings or to
     * {@code null}, matching the optional-chaining defaults of the source
     * parser.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBotProfile",
            exports = "botProfileParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        var profile = child.getRequiredChild("profile");

        var name = textOf(profile.getChild("name").orElse(null), "");
        var attributes = textOf(profile.getChild("attributes").orElse(null), "");
        var description = textOf(profile.getChild("description").orElse(null), "");
        var category = textOf(profile.getChild("category").orElse(null), "");
        var isDefault = "true".equals(textOf(profile.getChild("default").orElse(null), ""));
        var prompts = parsePrompts(profile.getChild("prompts"));
        var personaId = profile.getAttributeAsString("persona_id", "");
        var commandsParsed = parseCommands(profile.getChild("commands"));
        var isMetaCreatedNode = profile.getChild("is_meta_created");
        var isMetaCreated = isMetaCreatedNode.map(n -> "true".equals(textOf(n, ""))).orElse(null);
        var creatorNode = profile.getChild("creator");
        var creatorName = creatorNode
                .flatMap(n -> n.getChild("name"))
                .map(n -> textOf(n, "")).orElse(null);
        var creatorProfileUrl = creatorNode
                .flatMap(n -> n.getChild("profile_url"))
                .map(n -> textOf(n, "")).orElse(null);
        var posing = profile.getChild("posing_as_professional")
                .flatMap(n -> n.getAttributeAsString("type"))
                .map(this::posingFromWire).orElse(null);

        return new BotProfileResult(
                name, attributes, description, category, isDefault,
                prompts, personaId, commandsParsed.commands, commandsParsed.commandsDescription,
                isMetaCreated, creatorName, creatorProfileUrl, posing);
    }

    /**
     * Reads a stanza's inline text content, falling back when the stanza is
     * absent or empty.
     *
     * Used by the bot-profile parser to coalesce a missing or empty child
     * into a single default value in one place.
     *
     * @param stanza         the stanza to read
     * @param defaultValue the value to return when the stanza is {@code null}
     *                     or empty
     * @return the inline text or the fallback
     */
    private static String textOf(Stanza stanza, String defaultValue) {
        return stanza == null ? defaultValue : stanza.toContentString().orElse(defaultValue);
    }

    /**
     * Parses the optional {@code <prompts>} child into a list of typed
     * {@link BotProfileResult.Prompt} entries.
     *
     * Each prompt carries an {@code <emoji>} and a {@code <text>}; both fall
     * back to the empty string when absent.
     *
     * @param promptsChild the optional {@code <prompts>} child
     * @return the parsed prompts, never {@code null}
     */
    private static List<BotProfileResult.Prompt> parsePrompts(Optional<Stanza> promptsChild) {
        if (promptsChild.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<BotProfileResult.Prompt>();
        promptsChild.get().streamChildren("prompt").forEach(prompt -> {
            var emoji = textOf(prompt.getChild("emoji").orElse(null), "");
            var text = textOf(prompt.getChild("text").orElse(null), "");
            out.add(new BotProfileResult.Prompt(emoji, text));
        });
        return List.copyOf(out);
    }

    /**
     * Parses the optional {@code <commands>} child into a list of typed
     * {@link BotProfileResult.Command} entries plus the block's descriptive
     * header.
     *
     * The header lives on a {@code <description>} sibling of the
     * {@code <command>} entries, not inside each command.
     *
     * @param commandsChild the optional {@code <commands>} child
     * @return the parsed commands and their header
     */
    private static CommandsParsed parseCommands(Optional<Stanza> commandsChild) {
        if (commandsChild.isEmpty()) {
            return new CommandsParsed(List.of(), "");
        }
        var node = commandsChild.get();
        var description = textOf(node.getChild("description").orElse(null), "");
        var out = new ArrayList<BotProfileResult.Command>();
        node.streamChildren("command").forEach(cmd -> {
            var name = textOf(cmd.getChild("name").orElse(null), "");
            var desc = textOf(cmd.getChild("description").orElse(null), "");
            out.add(new BotProfileResult.Command(name, desc));
        });
        return new CommandsParsed(List.copyOf(out), description);
    }

    /**
     * Maps the {@code type} attribute on the {@code <posing_as_professional>}
     * child to its typed enum value.
     *
     * Any value other than {@code "yes"} or {@code "no"} resolves to
     * {@link BotProfileResult.PosingAsProfessional#UNKNOWN}.
     *
     * @param wire the wire literal
     * @return the matching enum value
     */
    private BotProfileResult.PosingAsProfessional posingFromWire(String wire) {
        return switch (wire) {
            case "yes" -> BotProfileResult.PosingAsProfessional.YES;
            case "no"  -> BotProfileResult.PosingAsProfessional.NO;
            default    -> BotProfileResult.PosingAsProfessional.UNKNOWN;
        };
    }

    /**
     * Pairs the parsed command list with its descriptive header.
     *
     * Returned by {@link #parseCommands(Optional)} and kept private because
     * callers always destructure both values together.
     *
     * @param commands            the parsed command list
     * @param commandsDescription the free-form header above the list
     */
    private record CommandsParsed(List<BotProfileResult.Command> commands, String commandsDescription) {
    }
}
