package com.github.auties00.cobalt.wire.stanza.usync;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Per-protocol backoff registry shared across USync queries.
 *
 * <p>A single instance is owned by the client and shared by every
 * {@link UsyncQuery} it dispatches; sharing the registry is what makes the
 * backoff state persistent across calls. The client calls
 * {@link #waitForBackoff(UsyncQuery)} before each dispatch and
 * {@link #setProtocolBackoffMs(String, long)} when the relay attaches an
 * {@code error_backoff} attribute to a per-protocol error. {@link #clear(String)}
 * and {@link #clearAll()} drop active windows, primarily for tests and for
 * logout/reconnect reset paths.
 *
 * @implNote
 * WA Web stores a {@code Promise} per protocol that resolves after a
 * {@code setTimeout}, so awaiting the promise inherently sleeps. This
 * implementation stores the absolute expiry {@link Instant} instead and sleeps
 * on the difference.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncBackoff")
public final class UsyncBackoff {
    /**
     * Logger that mirrors the backoff start/end traces emitted by the JS
     * module.
     */
    private static final Logger LOGGER = Logger.getLogger(UsyncBackoff.class.getName());

    /**
     * Maps protocol wire name to the absolute expiry instant of its active
     * backoff window.
     */
    private final ConcurrentMap<String, Instant> backoffs;

    /**
     * Creates an empty backoff registry.
     */
    public UsyncBackoff() {
        this.backoffs = new ConcurrentHashMap<>();
    }

    /**
     * Records a backoff window for the named protocol.
     *
     * <p>Driven by the client when it observes an {@code error_backoff}
     * attribute on a per-protocol error in the response. Subsequent
     * {@link UsyncQuery} dispatches for the same protocol either block in
     * {@link #waitForBackoff(UsyncQuery)} until the window elapses or, for
     * {@link UsyncContext#INTERACTIVE} contexts, skip the wait.
     *
     * @param protocolName the protocol wire name (e.g. {@code "devices"},
     *                     {@code "contact"})
     * @param backoffMs    the window duration in milliseconds
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBackoff",
            exports = "setProtocolBackoffMs", adaptation = WhatsAppAdaptation.ADAPTED)
    public void setProtocolBackoffMs(String protocolName, long backoffMs) {
        var expiry = Instant.now().plusMillis(backoffMs);
        backoffs.put(protocolName, expiry);
        LOGGER.fine(() -> "usync: " + protocolName + " protocol: " + backoffMs + "ms backoff started");
    }

    /**
     * Sleeps the current thread until every backoff window relevant to the
     * given query has elapsed.
     *
     * <p>Three cases short-circuit the wait: {@link UsyncContext#INTERACTIVE}
     * skips the wait entirely because the user is waiting on the result;
     * {@link UsyncContext#MESSAGE} and {@link UsyncContext#VOIP} exempt the
     * {@code devices} protocol because failing here would block message
     * encryption; and protocols whose window has already elapsed are removed
     * from the map and skipped.
     *
     * @implNote
     * This implementation runs sequentially per protocol because Cobalt
     * dispatches USync on a virtual thread; the JS counterpart fans out via
     * per-protocol promises that share the same wall clock, so the observable
     * total wait is identical.
     *
     * @param query the query about to be dispatched
     * @throws InterruptedException if the current thread is interrupted while
     *                              sleeping
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncBackoff",
            exports = "waitForBackoff", adaptation = WhatsAppAdaptation.ADAPTED)
    public void waitForBackoff(UsyncQuery query) throws InterruptedException {
        if (query.context() == UsyncContext.INTERACTIVE) {
            return;
        }

        for (var protocol : query.protocols()) {
            var name = protocol.name();
            if ((query.context() == UsyncContext.MESSAGE || query.context() == UsyncContext.VOIP)
                    && "devices".equals(name)) {
                continue;
            }
            var expiry = backoffs.get(name);
            if (expiry == null) {
                continue;
            }
            var remaining = Duration.between(Instant.now(), expiry);
            if (remaining.isZero() || remaining.isNegative()) {
                backoffs.remove(name, expiry);
                LOGGER.fine(() -> "usync: " + name + " protocol backoff ended");
                continue;
            }
            Thread.sleep(remaining);
            backoffs.remove(name, expiry);
            LOGGER.fine(() -> "usync: " + name + " protocol backoff ended");
        }
    }

    /**
     * Drops any active backoff for the named protocol.
     *
     * <p>The next {@link #waitForBackoff(UsyncQuery)} returns immediately for
     * the named protocol. WA Web has no equivalent surface: the JS map is
     * cleared implicitly when each timer resolves.
     *
     * @param protocolName the protocol wire name
     */
    public void clear(String protocolName) {
        backoffs.remove(protocolName);
    }

    /**
     * Drops every active backoff window.
     *
     * <p>Called from logout and reconnect reset paths.
     */
    public void clearAll() {
        backoffs.clear();
    }
}
