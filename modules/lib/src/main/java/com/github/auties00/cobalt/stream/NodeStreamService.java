package com.github.auties00.cobalt.stream;

import com.github.auties00.cobalt.stanza.model.Stanza;

/**
 * Routes every inbound WhatsApp stanza to the {@link SocketStreamHandler} registered for its tag.
 *
 * <p>The WhatsApp wire protocol multiplexes many logically distinct stanza types on the same
 * noise-encrypted transport: {@code <iq>}, {@code <message>}, {@code <receipt>}, {@code <presence>},
 * {@code <chatstate>}, {@code <call>}, {@code <terminate>}, {@code <notification>}, {@code <ib>},
 * {@code <success>}, {@code <failure>}, {@code <stream:error>}, {@code <error>}, {@code <status>},
 * and {@code <xmlstreamend>}. An implementation owns one dedicated {@link SocketStreamHandler} per
 * stanza tag, exposes {@link #handle(Stanza)} as the single entry point fed by the noise transport
 * layer, and schedules each stanza off the socket-reader thread so a slow or blocking handler cannot
 * back up the reader. {@link #reset()} clears per-connection handler state on socket teardown so a
 * reconnecting session starts clean. {@link LiveNodeStreamService} is the production implementation.
 *
 * @implSpec
 * Implementations must be thread-safe, dispatch each stanza to the handler registered under its tag,
 * drop stanzas whose tag has no registered handler rather than failing, and propagate {@link #reset()}
 * to every registered handler.
 */
public interface NodeStreamService {
    /**
     * Dispatches the given stanza to the {@link SocketStreamHandler} registered for its tag.
     *
     * <p>The noise transport layer feeds every decrypted stanza here. Stanzas whose tag has no
     * registered handler are dropped rather than nacked.
     *
     * @implSpec
     * Implementations must schedule the matching handler off the socket-reader thread through
     * {@link SocketStreamHandler#handleAsync(Stanza)} and must not throw when no handler matches the
     * stanza tag.
     *
     * @param stanza the inbound stanza
     */
    void handle(Stanza stanza);

    /**
     * Clears per-connection handler state so every registered handler is safe to reuse after a
     * reconnection.
     *
     * <p>The socket layer calls this immediately after the underlying connection is torn down so that
     * one-shot bootstrap guards fire again on the next session and any per-key ordered chains from the
     * previous connection are dropped.
     *
     * @implSpec
     * Implementations must invoke {@link SocketStreamHandler#reset()} on every registered handler
     * exactly once.
     */
    void reset();
}
