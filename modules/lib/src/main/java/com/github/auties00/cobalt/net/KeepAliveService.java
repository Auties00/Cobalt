package com.github.auties00.cobalt.net;

/**
 * Periodically probes the WhatsApp socket so a silently-dropped link (one that
 * never produced a TCP reset) is detected instead of leaving the reader thread
 * blocked forever.
 *
 * <p>Started once the socket is open and stopped on every disconnect; a fresh
 * instance or restart accompanies each new connection.
 */
public interface KeepAliveService {
    /**
     * Starts the keepalive loop if it is not already running.
     */
    void start();

    /**
     * Stops the keepalive loop, waking it from its sleep so its thread exits.
     *
     * <p>Idempotent: stopping an already-stopped service has no effect.
     */
    void stop();
}
