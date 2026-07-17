package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.DisappearingModeResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Describes the USync {@code disappearing_mode} protocol.
 *
 * This descriptor asks the relay for each peer's current disappearing-message
 * timer. The descriptor is stateless and carries no per-user request payload;
 * pair it with any {@link UsyncUser} that carries an addressing slot.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncDisappearingMode")
public final class UsyncDisappearingModeProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "disappearing_mode";

    /**
     * Creates a disappearing-mode-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncDisappearingMode",
            exports = "USyncDisappearingModeProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncDisappearingModeProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncDisappearingMode",
            exports = "USyncDisappearingModeProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits an empty {@code <disappearing_mode/>}
     * element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncDisappearingMode",
            exports = "USyncDisappearingModeProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder().description(NAME).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because the
     * disappearing-mode protocol has no per-user payload on the request side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncDisappearingMode",
            exports = "USyncDisappearingModeProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * This override parses the {@code duration} attribute (seconds, defaulting
     * to zero), the {@code t} timestamp, and the {@code ephemerality_disabled}
     * flag into a {@link DisappearingModeResult}.
     *
     * @implNote
     * This implementation reads {@code ephemerality_disabled} unconditionally;
     * exposing the raw wire value keeps the parser independent of client-side
     * gating that would otherwise suppress the flag.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncDisappearingMode",
            exports = "disappearingModeParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        var duration = Duration.ofSeconds(child.getAttributeAsLong("duration", 0L));
        var timestamp = Instant.ofEpochSecond(child.getRequiredAttributeAsLong("t"));
        var ephemeralityDisabled = "true".equals(child.getAttributeAsString("ephemerality_disabled", ""));
        return new DisappearingModeResult(duration, timestamp, ephemeralityDisabled);
    }
}
