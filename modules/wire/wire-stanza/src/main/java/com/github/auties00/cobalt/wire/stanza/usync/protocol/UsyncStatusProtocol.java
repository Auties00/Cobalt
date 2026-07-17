package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.StatusResult;

import java.util.Optional;

/**
 * Describes the USync {@code status} protocol.
 *
 * This descriptor asks the relay for each peer's legacy "about" string. The
 * response distinguishes "no status set" from "status hidden by privacy" via
 * a {@code code="401"} marker. The descriptor is stateless; pair it with any
 * {@link UsyncUser} that carries an addressing slot and, when required by the
 * relay, a trusted-contact token set through
 * {@link UsyncUser#withTrustedContactToken(byte[])}.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncStatus")
public final class UsyncStatusProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "status";

    /**
     * Creates a status-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncStatus",
            exports = "USyncStatusProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncStatusProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncStatus",
            exports = "USyncStatusProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits an empty {@code <status/>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncStatus",
            exports = "USyncStatusProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder().description(NAME).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits a per-user {@code <tctoken>} carrying the
     * trusted-contact token whenever the user has one set, leaving the
     * scraping-protection gate to the relay rather than suppressing the token
     * client-side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncStatus",
            exports = "USyncStatusProtocol.getUserElement", adaptation = WhatsAppAdaptation.ADAPTED)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return user.trustedContactToken().map(token -> new StanzaBuilder()
                .description("tctoken")
                .content(token)
                .build());
    }

    /**
     * {@inheritDoc}
     *
     * This override distinguishes three response shapes: inline content
     * yields the live status text (or {@code null} when the content is the
     * empty string); no content with {@code code="401"} yields the empty
     * string to mark "hidden by peer privacy"; any other shape yields
     * {@code null} to mark "no status set".
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncStatus",
            exports = "statusParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        if (child.hasContent()) {
            var text = child.toContentString().orElse("");
            return new StatusResult(text.isEmpty() ? null : text);
        }
        if (child.getAttributeAsInt("code", -1) == 401) {
            return new StatusResult("");
        }
        return new StatusResult(null);
    }
}
