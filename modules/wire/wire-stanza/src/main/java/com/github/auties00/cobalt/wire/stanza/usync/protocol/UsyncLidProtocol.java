package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.LidResult;

import java.util.Optional;

/**
 * Describes the USync {@code lid} protocol.
 *
 * This descriptor asks the relay to resolve each peer's LID identifier or to
 * confirm a hint the client already holds. The descriptor is stateless;
 * per-user state, specifically the optional LID hint set through
 * {@link UsyncUser#withLid(com.github.auties00.cobalt.wire.core.jid.Jid)}, lives
 * on each {@link UsyncUser}. It is added idempotently to many other USync
 * queries through
 * {@link com.github.auties00.cobalt.wire.stanza.usync.UsyncQuery#withLidProtocol()}.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncLid")
public final class UsyncLidProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "lid";

    /**
     * Creates a LID-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "USyncLidProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncLidProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "USyncLidProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits an empty {@code <lid/>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "USyncLidProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder().description(NAME).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits the per-user element only when the user
     * carries a LID hint; the hint is shipped on the {@code jid} attribute of
     * the per-user {@code <lid>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "USyncLidProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return user.lid().map(lid -> new StanzaBuilder()
                .description(NAME)
                .attribute("jid", lid.toString())
                .build());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads the resolved LID from the {@code val}
     * attribute.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncLid",
            exports = "lidParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        return new LidResult(child.getAttributeAsJid("val").orElse(null));
    }
}
