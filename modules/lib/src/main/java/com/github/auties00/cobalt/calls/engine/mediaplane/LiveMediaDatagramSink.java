package com.github.auties00.cobalt.calls.engine.mediaplane;

import com.github.auties00.cobalt.calls.platform.LiveVoipHostApi;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends engine media datagrams through the {@code call_sendto} host seam over an unconnected
 * {@link DatagramChannel}.
 *
 * <p>Satisfies the engine's {@code call_sendto} host downcall exposed by
 * {@link LiveVoipHostApi.MediaDatagramSink} by writing each datagram to the destination the engine
 * supplies on a single unconnected channel. The channel is opened lazily on the first send and reused
 * for the lifetime of the host.
 *
 * <p>The web transport media plane does not exercise this downcall: the per call
 * {@link LiveMediaSession} owns the host socket through which the ICE connectivity checks and DTLS
 * records leave, and media rides as SCTP DATA over the data channel wrapped by DTLS rather than as raw
 * UDP.
 */
public final class LiveMediaDatagramSink implements LiveVoipHostApi.MediaDatagramSink {
    /**
     * The logger for {@link LiveMediaDatagramSink}.
     */
    private static final System.Logger LOGGER = Log.get(LiveMediaDatagramSink.class);

    /**
     * Holds the datagram channel, {@code null} until the first send opens it.
     *
     * <p>An {@link AtomicReference} so that concurrent first sends race to install exactly one channel
     * through a compare and set; the loser closes its own channel and adopts the winner's.
     */
    private final AtomicReference<DatagramChannel> channel = new AtomicReference<>();

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation opens one unconnected {@link DatagramChannel} on the first call,
     * reuses it thereafter, and returns the byte count the channel accepted. Any {@link IOException}
     * during channel open or send is logged and reported as zero bytes sent rather than propagated.
     */
    @Override
    public int send(byte[] payload, SocketAddress destination) {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        var open = channel.get();
        if (open == null) {
            try {
                open = DatagramChannel.open();
            } catch (IOException exception) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls host datagram channel open failed", exception);
                return 0;
            }
            if (!channel.compareAndSet(null, open)) {
                closeQuietly(open);
                open = channel.get();
            } else if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "calls host datagram channel opened");
            }
        }
        try {
            var sent = open.send(ByteBuffer.wrap(payload), destination);
            if (Log.TRACE) LOGGER.log(Level.TRACE, "calls host datagram sent {0} of {1} bytes", sent, payload.length);
            return sent;
        } catch (IOException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls host datagram send failed", exception);
            return 0;
        }
    }

    /**
     * Closes a datagram channel, logging and swallowing any close failure.
     *
     * <p>Discards a channel that lost the compare and set open race so two concurrent first sends never
     * leak a socket.
     *
     * @param toClose the channel to close
     */
    private static void closeQuietly(DatagramChannel toClose) {
        try {
            toClose.close();
        } catch (IOException exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calls host datagram channel close failed", exception);
        }
    }
}
