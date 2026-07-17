package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.NodeStreamService;

import java.lang.System.Logger.Level;

/**
 * Handles top-level {@code <error>} stanzas that report stanza-level protocol problems not scoped to a specific
 * request.
 *
 * <p>The handler is registered under the {@code "error"} tag inside {@link NodeStreamService} and runs whenever the server
 * emits a bare {@code <error code="..."/>} stanza. The common payload is {@code code=479} ({@code smax-invalid}),
 * reported when the client's last outbound stanza failed schema-driven validation on the server. The stanza is
 * informational only: the handler logs it and leaves the session up, never dispatching to the configured error handler.
 * Unrecognised codes are logged at {@code ERROR} severity; a missing or non-numeric {@code code} is logged at
 * {@code WARNING}.
 */
@WhatsAppWebModule(moduleName = "WABackendHandleError")
public final class ErrorStreamHandler extends SocketStreamHandler.Concurrent {
    /**
     * The logger for {@link ErrorStreamHandler}, used to record the diagnostic line for every inbound
     * {@code <error>} stanza.
     */
    private static final System.Logger LOGGER = Log.get(ErrorStreamHandler.class);

    /**
     * The reason code emitted by the server when the client sent a stanza that failed validation against the SMAX
     * schema.
     *
     * <p>Observing this code almost always indicates a wire-encoding bug on the Cobalt side rather than a transient
     * server condition.
     */
    @WhatsAppWebExport(moduleName = "WABackendHandleError", exports = "SMAX_INVALID", adaptation = WhatsAppAdaptation.DIRECT)
    private static final int SMAX_INVALID_CODE = 479;

    /**
     * Constructs a new {@code <error>} stanza handler.
     *
     * <p>The handler is stateless and holds no dependencies because it only logs.
     */
    public ErrorStreamHandler() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs the received {@code code} attribute and returns without taking any other action. A {@code code} value of
     * {@link #SMAX_INVALID_CODE} is logged at {@code ERROR} with a fixed message; any other code is logged at
     * {@code ERROR} with a generic message; a missing or non-numeric {@code code} is logged at {@code WARNING} as a
     * defensive fallback. The method never throws and never delegates to the pluggable error handler, so the socket
     * stays up.
     *
     * @implNote This implementation logs and returns rather than rejecting the stanza: WA Web's deprecated parser
     * would raise an XMPP parsing failure when the {@code code} attribute is missing, whereas Cobalt's {@link Stanza}
     * accessor returns {@code null} and the handler logs that case at {@code WARNING}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WABackendHandleError", exports = "handleError", adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        var code = stanza.getAttributeAsInt("code", null);
        if (code == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "received error stanza without code: {0}", stanza);
            return;
        }

        if (code == SMAX_INVALID_CODE) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "invalid stanza sent (smax-invalid)");
            return;
        }

        if (Log.ERROR) LOGGER.log(Level.ERROR, "unknown error code: {0}", code);
    }
}
