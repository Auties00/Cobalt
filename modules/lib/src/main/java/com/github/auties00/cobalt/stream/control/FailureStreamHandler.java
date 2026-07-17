package com.github.auties00.cobalt.stream.control;

import com.github.auties00.cobalt.stream.SocketStreamHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppConnectionException;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stream.NodeStreamService;

import java.lang.System.Logger.Level;

/**
 * Handles {@code <failure>} stanzas that report session-fatal error conditions during or after the post-handshake
 * exchange.
 *
 * <p>The handler is registered under the {@code "failure"} tag inside {@link NodeStreamService}. Failure stanzas are the WA
 * wire signal that the session cannot continue: the server pushes a numeric {@code reason} and the handler maps it to
 * the recovery exception that matches it. Logout-style reasons surface as {@link WhatsAppSessionException.LoggedOut},
 * the temporary-ban reason as {@link WhatsAppSessionException.Banned}, version mismatches as
 * {@link WhatsAppConnectionException}, and anything unrecognised as {@link WhatsAppServerRuntimeException}. The chosen
 * exception is handed to {@link LinkedWhatsAppClient#handleFailure(WhatsAppException)} so the configured error handler picks
 * the next action (logout, ban, reconnect, disconnect, discard).
 *
 * @implNote This implementation does not replicate WA Web's inline UI/storage recovery (logout banners, in-app updater
 * modal, temporary-ban screen, service-unavailable banner): the exception subtype is the only signal handed to the
 * configured error handler. A missing or out-of-range {@code reason} attribute is mapped to
 * {@link WhatsAppServerRuntimeException} rather than rejecting the parse.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleFailure")
@WhatsAppWebModule(moduleName = "WAWebFailureErrorCodes")
public final class FailureStreamHandler extends SocketStreamHandler.Concurrent {

    /**
     * The logger for {@link FailureStreamHandler}.
     */
    private static final System.Logger LOGGER = Log.get(FailureStreamHandler.class);

    /**
     * The {@code reason=400} (generic failure) code, logged at {@code WARNING} with no recovery dispatch.
     */
    private static final int REASON_GENERIC_FAILURE = 400;

    /**
     * The {@code reason=401} (not authorized) code, dispatched as a {@link WhatsAppSessionException.LoggedOut}.
     */
    private static final int REASON_NOT_AUTHORIZED = 401;

    /**
     * The {@code reason=402} (temporary ban) code, dispatched as a {@link WhatsAppSessionException.Banned} carrying the
     * {@code code}, {@code expire}, {@code message} and {@code url} attributes when present.
     */
    private static final int REASON_TEMP_BANNED = 402;

    /**
     * The {@code reason=403} (locked account) code, dispatched as a {@link WhatsAppSessionException.LoggedOut} carrying
     * any logout-message header, subtext and locale attributes the server provided.
     */
    private static final int REASON_LOCKED = 403;

    /**
     * The {@code reason=405} (client too old) code, dispatched as a {@link WhatsAppConnectionException} so the embedder
     * republishes with a newer pinned WA Web version.
     */
    private static final int REASON_CLIENT_TOO_OLD = 405;

    /**
     * The {@code reason=406} (permanent ban) code, dispatched as a {@link WhatsAppSessionException.LoggedOut} on the
     * same path as {@link #REASON_NOT_AUTHORIZED}.
     */
    private static final int REASON_BANNED = 406;

    /**
     * The {@code reason=409} (bad user agent) code, dispatched as a {@link WhatsAppConnectionException} so the embedder
     * updates the pinned WA Web bundle or the spoofed user agent string.
     */
    private static final int REASON_BAD_USER_AGENT = 409;

    /**
     * The {@code reason=500} (internal server error) code, logged at {@code WARNING} with no recovery dispatch.
     */
    private static final int REASON_INTERNAL_SERVER_ERROR = 500;

    /**
     * The {@code reason=501} (experimental) code, logged at {@code WARNING} with no recovery dispatch.
     */
    private static final int REASON_EXPERIMENTAL = 501;

    /**
     * The {@code reason=503} (service unavailable) code, logged at {@code WARNING} with no recovery dispatch.
     */
    private static final int REASON_SERVICE_UNAVAILABLE = 503;

    /**
     * The {@link LinkedWhatsAppClient} used to dispatch the parsed failure exception through the pluggable error handler.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Constructs a new failure stream handler bound to the given client.
     *
     * @param whatsapp the {@link LinkedWhatsAppClient} on which the parsed failure exception is dispatched
     */
    public FailureStreamHandler(LinkedWhatsAppClient whatsapp) {
        this.whatsapp = whatsapp;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Parses the {@code reason}, {@code location}, {@code code}, {@code expire}, {@code message}, {@code url} and
     * three {@code logout_message_*} attributes from the {@code <failure>} stanza and dispatches the recovery exception
     * that matches the {@code reason} value. Logout-style reasons surface as {@link WhatsAppSessionException.LoggedOut};
     * the temporary-ban reason surfaces as {@link WhatsAppSessionException.Banned}; version mismatches surface as
     * {@link WhatsAppConnectionException}; informational 4xx/5xx reasons are logged without dispatch; anything else is
     * wrapped in a {@link WhatsAppServerRuntimeException}.
     *
     * @implNote This implementation maps a missing or out-of-range {@code reason} attribute to
     * {@link WhatsAppServerRuntimeException} rather than rejecting the parse as WA Web's deprecated WAP parser does.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebHandleFailure", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    public void handle(Stanza stanza) {
        var reason = stanza.getAttributeAsInt("reason", (Integer) null);
        var location = stanza.getAttributeAsString("location", null);
        var code = stanza.getAttributeAsInt("code", (Integer) null);
        var expire = stanza.getAttributeAsInt("expire", (Integer) null);
        var message = stanza.getAttributeAsString("message", null);
        var url = stanza.getAttributeAsString("url", null);
        var logoutMessageHeader = stanza.getAttributeAsString("logout_message_header", null);
        var logoutMessageSubtext = stanza.getAttributeAsString("logout_message_subtext", null);
        var logoutMessageLocale = stanza.getAttributeAsString("logout_message_locale", null);

        if (Log.WARNING) {
            LOGGER.log(Level.WARNING,
                    "received failure stanza reason={0} location={1} code={2} expire={3} message={4} url={5}",
                    reason,
                    location,
                    code,
                    expire,
                    message,
                    url);
        }

        if (reason == null) {
            whatsapp.handleFailure(new WhatsAppServerRuntimeException(
                    "Malformed failure stanza: missing reason attribute"));
            return;
        }

        switch (reason) {
            case REASON_LOCKED -> {
                whatsapp.handleFailure(new WhatsAppSessionException.LoggedOut(
                        "Account locked: reason=" + reason
                                + (logoutMessageHeader != null ? ", header=" + logoutMessageHeader : "")
                                + (logoutMessageSubtext != null ? ", subtext=" + logoutMessageSubtext : "")
                                + (logoutMessageLocale != null ? ", locale=" + logoutMessageLocale : "")));
            }
            case REASON_NOT_AUTHORIZED, REASON_BANNED -> {
                whatsapp.handleFailure(new WhatsAppSessionException.LoggedOut(
                        "Server reported logout, reason=" + reason));
            }
            case REASON_CLIENT_TOO_OLD, REASON_BAD_USER_AGENT -> {
                whatsapp.handleFailure(new WhatsAppConnectionException(
                        "Client version rejected by server, reason=" + reason));
            }
            case REASON_TEMP_BANNED -> {
                if (code != null && expire != null) {
                    whatsapp.handleFailure(new WhatsAppSessionException.Banned(
                            "Temporary ban: code=" + code + ", expire=" + expire
                                    + (message != null ? ", message=" + message : "")
                                    + (url != null ? ", url=" + url : "")));
                } else {
                    whatsapp.handleFailure(new WhatsAppServerRuntimeException(
                            "Incorrect temporary ban data: code=" + code + ", expire=" + expire));
                }
            }
            case REASON_GENERIC_FAILURE, REASON_INTERNAL_SERVER_ERROR, REASON_EXPERIMENTAL -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "server failure code={0}, no action taken", reason);
                }
            }
            case REASON_SERVICE_UNAVAILABLE -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING,
                            "service unavailable reason={0}", reason);
                }
            }
            default -> {
                whatsapp.handleFailure(new WhatsAppServerRuntimeException(
                        "Failure reason " + reason + " not implemented yet"));
            }
        }
    }
}
