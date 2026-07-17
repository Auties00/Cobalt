package com.github.auties00.cobalt.net;

import java.time.Duration;

/**
 * Sends one keepalive ping and blocks for its reply, supplied to
 * {@link LiveKeepAliveService} so it need not depend on the client directly.
 *
 * <p>The implementation issues the WhatsApp {@code w:p} ping and waits up to the
 * given timeout; it returns normally when the reply arrives and throws a runtime
 * exception (a stanza timeout or a closed session) when the link is dead.
 */
@FunctionalInterface
public interface KeepAlivePinger {
    /**
     * Sends a keepalive ping and waits for its reply.
     *
     * @param timeout the maximum time to wait for the reply
     * @throws RuntimeException if the ping cannot be sent or no reply arrives in
     *         time, signalling a dead link
     */
    void ping(Duration timeout);
}
