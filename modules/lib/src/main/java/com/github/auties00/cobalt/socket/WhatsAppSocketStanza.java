package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.exception.linked.WhatsAppStreamException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Pairs an outbound {@link Stanza} with the inbound response that
 * completes it, exposing a blocking send-then-await primitive over the
 * otherwise fully asynchronous reader pipeline.
 *
 * <p>Every request-response stanza Cobalt sends on the WhatsApp socket uses
 * this pairing: the sender thread parks on {@link #waitForResponse()} after
 * dispatching the request, and the inbound dispatcher pumps each arriving
 * {@link Stanza} through {@link #complete(Stanza)} until the filter accepts one.
 * The filter is what distinguishes a stanza-pair from a fire-and-forget
 * broadcast: a {@code null} filter accepts the first stanza offered, an
 * explicit predicate keeps unrelated traffic from waking up the waiter
 * early. The two-arg {@link #waitForResponse(Duration)} overload exists so
 * callers that issue long-running operations (history sync chunk fetches,
 * large media downloads) can extend the default 60-second budget.
 *
 * @implNote
 * This implementation maps WA Web's per-stanza callback registry to a
 * blocking primitive on a virtual thread; the {@code synchronized}
 * block plus {@code wait}/{@code notifyAll} is the simplest correct
 * pairing, and parking a virtual thread is essentially free under
 * Project Loom.
 */
public final class WhatsAppSocketStanza {
    /**
     * The logger for {@link WhatsAppSocketStanza}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppSocketStanza.class);

    /**
     * Default upper bound on how long {@link #waitForResponse()} parks
     * before raising {@link WhatsAppStreamException.NodeTimeout}.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    /**
     * The outbound {@link Stanza} this stanza tracks, retained so
     * {@link WhatsAppStreamException.NodeTimeout} can report which
     * request expired.
     */
    private final Stanza body;

    /**
     * Predicate consulted to decide whether an arriving {@link Stanza}
     * satisfies this stanza, or {@code null} to accept any stanza.
     */
    private final Function<Stanza, Boolean> filter;

    /**
     * The accepted response, set by {@link #complete(Stanza)} and read
     * by the waiter under {@code this} monitor.
     */
    private volatile Stanza response;

    /**
     * Creates a stanza tracker for the given outbound {@link Stanza}.
     *
     * <p>One tracker is constructed per outbound request that expects a
     * reply. The constructor does not send the request; the caller is
     * responsible for dispatching {@code body} and only then parking on
     * {@link #waitForResponse()}.
     *
     * @param body   the outbound stanza, retained for timeout reporting
     * @param filter predicate that returns {@code true} when an
     *               inbound stanza is the matching response, or
     *               {@code null} to accept the first stanza offered
     */
    public WhatsAppSocketStanza(Stanza body, Function<Stanza, Boolean> filter) {
        this.body = body;
        this.filter = filter;
    }

    /**
     * Offers an inbound {@link Stanza} as the candidate response for
     * this stanza and wakes any waiter if the filter accepts it.
     *
     * <p>The inbound dispatcher calls this for every stanza that might match.
     * A {@code null} response unconditionally completes the stanza (used by
     * the disconnect path to release waiters). The return value lets the
     * dispatcher know whether to keep dispatching the same stanza to the next
     * candidate stanza or to stop because it has been consumed.
     *
     * @implNote
     * This implementation evaluates the user-supplied {@code filter}
     * outside the monitor so callers do not pay synchronization cost
     * for rejected candidates.
     *
     * @param response the candidate response, or {@code null} to
     *                 unconditionally complete the stanza
     * @return {@code true} if the response was accepted and a waiter
     *         (if any) was notified
     */
    public boolean complete(Stanza response) {
        var acceptable = response == null
                || filter == null
                || filter.apply(response);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "stanza candidate offered, acceptable={0}", acceptable);
        if (acceptable) {
            synchronized (this) {
                this.response = response;
                notifyAll();
            }
        }
        return acceptable;
    }

    /**
     * Parks the calling virtual thread until a response arrives or
     * the default timeout elapses.
     *
     * <p>This is equivalent to {@link #waitForResponse(Duration)} with the
     * 60-second default. Most stanzas (presence ack, simple IQ round-trips)
     * complete in milliseconds; the overload is for requests that can
     * legitimately take longer.
     *
     * @return the accepted response
     * @throws WhatsAppStreamException.NodeTimeout if no acceptable
     *         response arrives within the default timeout
     */
    public Stanza waitForResponse() {
        return waitForResponse(TIMEOUT);
    }

    /**
     * Parks the calling virtual thread until a response arrives or
     * the supplied {@code timeout} elapses.
     *
     * <p>The longer-timeout overload covers inherently slow operations
     * (initial history sync chunk fetches, large media uploads, server-side
     * pairing flows that block on user action). The default-timeout
     * {@link #waitForResponse()} covers ordinary IQ round-trips.
     *
     * @implNote
     * This implementation treats {@link InterruptedException} as a
     * timeout: the interrupt flag is re-asserted on the current
     * thread and a {@link WhatsAppStreamException.NodeTimeout} is
     * thrown so callers see a uniform failure mode regardless of
     * whether the wait expired naturally or was unblocked by a
     * shutdown.
     *
     * @param timeout the maximum amount of time to wait
     * @return the accepted response
     * @throws WhatsAppStreamException.NodeTimeout if no acceptable
     *         response arrives within {@code timeout} or the wait is
     *         interrupted
     */
    public Stanza waitForResponse(Duration timeout) {
        synchronized (this) {
            var end = Instant.now().plus(timeout);
            while (response == null) {
                var remainingMs = Duration.between(Instant.now(), end).toMillis();
                if (remainingMs <= 0) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "stanza timed out waiting for response to {0}", body);
                    throw new WhatsAppStreamException.NodeTimeout(body);
                }
                try {
                    wait(remainingMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "stanza wait interrupted for {0}", body);
                    throw new WhatsAppStreamException.NodeTimeout(body);
                }
            }
            if (Log.TRACE) LOGGER.log(Level.TRACE, "stanza response received for {0}", body);
            return response;
        }
    }
}
