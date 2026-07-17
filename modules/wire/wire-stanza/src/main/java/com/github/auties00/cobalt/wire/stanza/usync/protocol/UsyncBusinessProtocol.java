package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.BusinessResult;

import java.util.List;
import java.util.Optional;

/**
 * Describes the USync {@code business} protocol.
 *
 * This descriptor asks the relay for each peer's signed verified-name
 * certificate. The descriptor is stateless and carries no per-user request
 * payload; it is paired with the {@link UsyncUser} addressing that selects
 * the peer whose certificate is wanted.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncBusiness")
public final class UsyncBusinessProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "business";

    /**
     * Creates a business-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBusiness",
            exports = "USyncBusinessProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncBusinessProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBusiness",
            exports = "USyncBusinessProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation wraps a single empty {@code <verified_name/>} child
     * inside the {@code <business>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBusiness",
            exports = "USyncBusinessProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder()
                .description(NAME)
                .content(List.of(new StanzaBuilder().description("verified_name").build()))
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because the
     * business protocol has no per-user payload on the request side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBusiness",
            exports = "USyncBusinessProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns the raw {@code <verified_name>} stanza inside
     * {@link BusinessResult}; the verified-name parsing (signature check,
     * certificate decode) is deferred to the caller.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncBusiness",
            exports = "businessParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        return new BusinessResult(child.getChild("verified_name").orElse(null));
    }
}
