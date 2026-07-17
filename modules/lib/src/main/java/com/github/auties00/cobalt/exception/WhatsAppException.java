package com.github.auties00.cobalt.exception;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorHandler;
import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.linked.WhatsAppLinkedException;

/**
 * Sealed root of every exception thrown by Cobalt while talking to WhatsApp.
 * <p>
 * The two direct subtypes split the hierarchy by transport:
 * {@link WhatsAppLinkedException} roots every failure of the Linked
 * transport (WhatsApp Web/Desktop and Mobile over the encrypted
 * binary-XMPP socket), and {@link WhatsAppCloudException} roots every
 * failure of Meta's WhatsApp Cloud API transport. Each concrete leaf names
 * a single failure mode (a session conflict, a media download failure, an
 * account device verification mismatch, and so on) and reports through
 * {@link #toErrorResult()} the {@link WhatsAppLinkedClientErrorResult} that
 * mirrors WhatsApp Web's native reaction to that failure. The permits list
 * is closed at every level, so a {@code switch} over a
 * {@code WhatsAppException} can be exhaustive.
 * <p>
 * Cobalt does not pin a recovery action to any exception. When a subtype
 * is thrown, the configurable {@link WhatsAppLinkedClientErrorHandler} decides
 * the recovery: the bundled factories log the exception and return its
 * {@link #toErrorResult()}, while a custom handler may pattern-match the
 * concrete subtype and choose differently among discarding the event,
 * disconnecting, reconnecting, logging out, or treating the account as
 * banned.
 *
 * @apiNote
 * Most embedders observe these exceptions through the configured
 * {@link WhatsAppLinkedClientErrorHandler} rather than through {@code try}/{@code catch}
 * blocks around individual calls; pattern-match on the concrete subtype
 * there to react to a specific failure mode.
 *
 * @implNote
 * This implementation is deliberately redesigned versus WhatsApp Web. WA
 * Web embeds inline recovery logic at each error site (try/catch with
 * retry, disconnect, or ignore); Cobalt throws the appropriate sealed
 * subtype and lets the user-configurable error handler decide the
 * recovery policy.
 *
 * @see #toErrorResult()
 * @see WhatsAppLinkedClientErrorHandler
 */
public abstract sealed class WhatsAppException extends RuntimeException
        permits WhatsAppLinkedException, WhatsAppCloudException {

    /**
     * Constructs a new WhatsApp exception with no detail message.
     */
    protected WhatsAppException() {
        super();
    }

    /**
     * Constructs a new WhatsApp exception with the specified detail message.
     *
     * @param message the detail message describing the error condition
     */
    protected WhatsAppException(String message) {
        super(message);
    }

    /**
     * Constructs a new WhatsApp exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error condition
     * @param cause   the underlying cause of this exception
     */
    protected WhatsAppException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new WhatsApp exception wrapping the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    protected WhatsAppException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns the recovery action that mirrors WhatsApp Web's native
     * reaction to this failure.
     *
     * Each concrete subtype maps its single failure mode to the
     * {@link WhatsAppLinkedClientErrorResult} WhatsApp Web would apply: swallow
     * the event and keep the session running
     * ({@link WhatsAppLinkedClientErrorResult#DISCARD}), tear the session down
     * ({@link WhatsAppLinkedClientErrorResult#DISCONNECT}), drop and immediately
     * re-establish the connection
     * ({@link WhatsAppLinkedClientErrorResult#RECONNECT}), log the device out
     * ({@link WhatsAppLinkedClientErrorResult#LOG_OUT}), or treat the account as
     * banned ({@link WhatsAppLinkedClientErrorResult#BAN}).
     *
     * @apiNote
     * This is the default action the bundled
     * {@link WhatsAppLinkedClientErrorHandler} factories
     * ({@link WhatsAppLinkedClientErrorHandler#toTerminal()},
     * {@link WhatsAppLinkedClientErrorHandler#toFile()}) return after logging the
     * exception; a custom handler may inspect the concrete subtype and
     * decide differently.
     *
     * @implSpec
     * Every concrete subtype returns a constant or per-instance result. A
     * subtype whose failure is local to a single operation (one message,
     * one media transfer, one AB-prop lookup, one sync collection) returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}; a subtype that leaves the
     * encrypted Noise channel unusable returns one of the session-control
     * results ({@link WhatsAppLinkedClientErrorResult#DISCONNECT},
     * {@link WhatsAppLinkedClientErrorResult#RECONNECT},
     * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}, or
     * {@link WhatsAppLinkedClientErrorResult#BAN}).
     *
     * @return the recovery action for this failure, never {@code null}
     */
    public abstract WhatsAppLinkedClientErrorResult toErrorResult();
}
