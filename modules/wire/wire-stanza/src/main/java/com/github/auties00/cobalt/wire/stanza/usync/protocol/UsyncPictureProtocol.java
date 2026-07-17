package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.PictureResult;

import java.util.Optional;

/**
 * Describes the USync {@code picture} protocol.
 *
 * This descriptor asks the relay for each peer's profile-picture id; the JPEG
 * payload is fetched separately through the media URL. The descriptor is
 * stateless and carries no per-user request payload; pair it with any
 * {@link UsyncUser} that carries an addressing slot.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncPicture")
public final class UsyncPictureProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "picture";

    /**
     * Creates a picture-protocol descriptor.
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncPicture",
            exports = "USyncPictureProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncPictureProtocol() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncPicture",
            exports = "USyncPictureProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits an empty {@code <picture/>} element.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncPicture",
            exports = "USyncPictureProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        return new StanzaBuilder().description(NAME).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because the
     * picture protocol has no per-user payload on the request side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncPicture",
            exports = "USyncPictureProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation reads the {@code id} attribute as a required int.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncPicture",
            exports = "pictureParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        return new PictureResult(child.getRequiredAttributeAsInt("id"));
    }
}
