package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stream.NodeStreamService;

import java.lang.System.Logger.Level;

/**
 * Handles the {@code <xmlstreamend>} stanza that the WhatsApp server emits to signal a graceful end of the encrypted
 * stream.
 *
 * <p>The handler is registered under the {@code "xmlstreamend"} tag inside {@link NodeStreamService} and runs as the final
 * stanza on every clean server-initiated close. It only logs the stanza and sends no acknowledgement back; the reader
 * loop recognises this stanza as the terminal frame and stops reading once it has been dispatched, so the underlying
 * socket teardown needs no reply sent here.
 *
 * @implNote This implementation is a pure log call because {@link NodeStreamService} never auto-acks on its own, so a no-op
 * handler reproduces WA Web's {@code "NO_ACK"} wire behaviour for the {@code "xmlstreamend"} branch.
 */
@WhatsAppWebModule(moduleName = "WAWebCommsHandleLoggedInStanza")
public final class XmlStreamEndStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link XmlStreamEndStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(XmlStreamEndStreamHandler.class);

    /**
     * Constructs a new {@code <xmlstreamend>} stanza handler.
     */
    public XmlStreamEndStreamHandler() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs that the server has signalled end-of-stream and returns without dispatching any reply. The reader loop
     * stops reading once this stanza has been dispatched, so the socket teardown that follows needs no reply sent here.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebCommsHandleLoggedInStanza", exports = "handleLoggedInStanza", adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        if (Log.INFO) {
            LOGGER.log(Level.INFO, "received xmlstreamend, no ack sent");
        }
    }
}
