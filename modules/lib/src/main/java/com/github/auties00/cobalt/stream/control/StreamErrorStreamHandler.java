package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stream.NodeStreamService;

import java.lang.System.Logger.Level;

/**
 * Handles {@code <stream:error>} stanzas that report protocol-level failures requiring the socket to close.
 *
 * <p>The handler is registered under the {@code "stream:error"} tag inside {@link NodeStreamService} and classifies the
 * inbound stanza into one of three buckets: a {@code <conflict/>} child (another session took over or the device was
 * removed), a numeric {@code code} attribute (5xx server stream error, with {@code 515} meaning reconnect and
 * {@code 516} meaning logout), or an XML-validity child ({@code <ack/>} or {@code <xml-not-well-formed/>}). Each bucket
 * is dispatched as the matching {@link WhatsAppSessionException} subtype; both XML-validity children surface as
 * {@link WhatsAppSessionException.Closed} (a server-reported stream error tears the stream down, so the channel is
 * re-established). The dispatched exception is handed to
 * {@link LinkedWhatsAppClient#handleFailure(com.github.auties00.cobalt.exception.WhatsAppException)}, and the configured error
 * handler decides between {@code DISCARD}, {@code DISCONNECT}, {@code RECONNECT}, {@code LOG_OUT} or {@code BAN}.
 *
 * @implNote This implementation does not perform WA Web's inline recovery (stop-comms, restart-login, start-logout,
 * credential clearing): the socket close is left to the configured error handler reacting to the dispatched exception.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleStreamError")
public final class StreamErrorStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link StreamErrorStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(StreamErrorStreamHandler.class);

    /**
     * The {@code code=515} stream error code that asks the client to tear down the current socket and re-login.
     *
     * <p>Surfaced as {@link WhatsAppSessionException.Reconnect}.
     */
    private static final int STREAM_ERROR_RESTART_LOGIN = 515;

    /**
     * The {@code code=516} stream error code that forces the companion device to log out.
     *
     * <p>Surfaced as {@link WhatsAppSessionException.LoggedOut}.
     */
    private static final int STREAM_ERROR_LOGOUT = 516;

    /**
     * The {@link LinkedWhatsAppClient} used to dispatch the parsed failure exception through the pluggable error handler.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a new stream error handler bound to the given client.
     *
     * @param whatsapp the {@link LinkedWhatsAppClient} on which the parsed failure exception is dispatched
     */
    public StreamErrorStreamHandler(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Classifies the {@code <stream:error>} stanza in priority order ({@code <conflict/>} child first, then
     * {@code code} attribute, then {@code <ack/>} or {@code <xml-not-well-formed/>} child, otherwise unrecognised) and
     * dispatches the matching {@link WhatsAppSessionException} subtype.
     *
     * @implNote This implementation never returns the close-socket sentinel that WA Web resolves with: the socket close
     * is left to the configured error handler reacting to the dispatched exception.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebHandleStreamError", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        var conflict = stanza.getChild("conflict").orElse(null);
        if (conflict != null) {
            handleConflict(conflict.getAttributeAsString("type", null));
            return;
        }

        var code = stanza.getAttributeAsInt("code", (Integer) null);
        if (code != null) {
            handleCode(code);
            return;
        }

        var ack = stanza.getChild("ack").orElse(null);
        if (ack != null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING,
                        "received stream:error ack for id={0}",
                        ack.getAttributeAsString("id", null));
            }
            whatsapp.handleFailure(new WhatsAppSessionException.Closed("Stream error: ack"));
            return;
        }

        if (stanza.hasChild("xml-not-well-formed")) {
            whatsapp.handleFailure(new WhatsAppSessionException.Closed("Stream error: xml-not-well-formed"));
            return;
        }

        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "received unrecognized stream:error stanza {0}", stanza);
        }
        whatsapp.handleFailure(new WhatsAppSessionException.Closed("Stream error: unrecognized"));
    }

    /**
     * Dispatches the appropriate {@link WhatsAppSessionException} subtype for a
     * {@code <stream:error><conflict type=".../></stream:error>} stanza.
     *
     * <p>A {@code type} of {@code "replaced"} (another session took over the mutex) surfaces as
     * {@link WhatsAppSessionException.Conflict}; any other value, including {@code "device_removed"} and unknown values,
     * surfaces as {@link WhatsAppSessionException.LoggedOut}.
     *
     * @implNote This implementation has no equivalent of WA Web's current-tab-mutex check because Cobalt has no tab
     * concept; the {@code "replaced"} branch is always surfaced verbatim.
     *
     * @param type the {@code type} attribute of the {@code <conflict/>} child, or {@code null} when absent
     */
    private void handleConflict(String type) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "stream:error conflict type={0}", type);
        }
        if ("replaced".equals(type)) {
            whatsapp.handleFailure(new WhatsAppSessionException.Conflict("Stream replaced by another active session"));
            return;
        }

        whatsapp.handleFailure(new WhatsAppSessionException.LoggedOut("Server removed or invalidated this device"));
    }

    /**
     * Dispatches the appropriate {@link WhatsAppSessionException} subtype for a {@code <stream:error code="..."/>}
     * stanza.
     *
     * <p>Within the 500-599 range, {@link #STREAM_ERROR_RESTART_LOGIN} drives a
     * {@link WhatsAppSessionException.Reconnect}, {@link #STREAM_ERROR_LOGOUT} drives a
     * {@link WhatsAppSessionException.LoggedOut}, and other 5xx values surface as
     * {@link WhatsAppSessionException.Closed}. Codes outside the 5xx range also surface as
     * {@link WhatsAppSessionException.Closed}.
     *
     * @implNote This implementation does not emit the device-link WAM event that WA Web fires before the forced logout;
     * that event is already covered when pairing fails through the IQ handler's companion-stage reporting.
     *
     * @param code the {@code code} attribute on the stanza
     */
    private void handleCode(int code) {
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "received stream:error code={0}", code);
        }
        switch (code) {
            case STREAM_ERROR_RESTART_LOGIN -> whatsapp.handleFailure(new WhatsAppSessionException.Reconnect("Server requested reconnect"));
            case STREAM_ERROR_LOGOUT -> whatsapp.handleFailure(new WhatsAppSessionException.LoggedOut("Server requested logout"));
            default ->  whatsapp.handleFailure(new WhatsAppSessionException.Closed("Server stream error " + code));
        }
    }
}
