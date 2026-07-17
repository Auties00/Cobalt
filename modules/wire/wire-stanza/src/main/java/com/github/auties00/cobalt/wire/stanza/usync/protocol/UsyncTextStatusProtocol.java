package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.TextStatusResult;

import java.time.Duration;
import java.util.Optional;

/**
 * Describes the USync {@code text_status} protocol.
 *
 * This descriptor asks the relay for each peer's modern text-status payload
 * (text, emoji, ephemeral lifetime, last-update timestamp). The descriptor is
 * stateless and carries no per-user request payload; pair it with any
 * {@link UsyncUser} that carries an addressing slot.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncTextStatus")
public final class UsyncTextStatusProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "text_status";

    /**
     * Creates a text-status-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncTextStatus",
            exports = "USyncTextStatusProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncTextStatusProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncTextStatus",
            exports = "USyncTextStatusProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits an empty {@code <text_status/>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncTextStatus",
            exports = "USyncTextStatusProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder().description(NAME).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because the
     * text-status protocol has no per-user payload on the request side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncTextStatus",
            exports = "USyncTextStatusProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * This override parses the {@code text} attribute, the {@code content}
     * attribute of the optional {@code <emoji>} child, the
     * {@code ephemeral_duration_sec} attribute (boxed into a {@link Duration}),
     * and the {@code last_update_time} attribute as a raw string into a
     * {@link TextStatusResult}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncTextStatus",
            exports = "textStatusParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        var text = child.getAttributeAsString("text").orElse(null);
        var emoji = child.getChild("emoji")
                .flatMap(e -> e.getAttributeAsString("content")).orElse(null);
        var ephemeralDuration = child.getAttributeAsLong("ephemeral_duration_sec").stream().boxed()
                .map(Duration::ofSeconds).findFirst().orElse(null);
        var lastUpdateTime = child.getAttributeAsString("last_update_time").orElse(null);
        return new TextStatusResult(text, emoji, ephemeralDuration, lastUpdateTime);
    }
}
