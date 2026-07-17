package com.github.auties00.cobalt.net;

import java.io.IOException;

/**
 * A single attempt to re-establish the WhatsApp socket, supplied to the
 * {@link ReconnectSupervisor} so it need not depend on the client directly.
 *
 * <p>The implementation opens a fresh socket and restarts the per-connection
 * machinery; it returns normally on success and throws {@link IOException} on a
 * transport failure that the supervisor should retry with backoff.
 */
@FunctionalInterface
public interface ConnectAttempt {
    /**
     * Performs one connection attempt.
     *
     * @throws IOException if the socket cannot be opened, so the supervisor
     *         backs off and retries
     */
    void run() throws IOException;
}
