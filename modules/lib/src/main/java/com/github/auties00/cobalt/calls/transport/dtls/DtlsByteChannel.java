package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;

/**
 * Adapts a {@link VoipDtlsTransport} to a {@link ByteChannel} that moves exactly one SCTP record per read
 * and one per write, the one datagram per read and per write transport contract the pure Java
 * {@code com.github.auties00.sctp} {@code SctpSocket} builds its association over.
 *
 * <p>The relay leg runs SCTP over DTLS: the SCTP socket owns this channel as its transport, so its writer
 * thread hands one serialized SCTP packet at a time to {@link #write(ByteBuffer)} (encrypted into one DTLS
 * application data record and put on the host UDP egress by the underlying {@link VoipDtlsTransport}), while
 * its reader thread pulls one decrypted SCTP packet at a time from {@link #read(ByteBuffer)}. The DTLS
 * engine already supports one concurrently blocked reader alongside writes from other threads, so this
 * adapter adds no locking of its own; it only serves as the shape conversion between the byte count
 * {@link VoipDtlsTransport} API and the {@link ByteChannel} API.
 *
 * <p>{@link #read(ByteBuffer)} follows the one datagram per read contract: it returns exactly one SCTP
 * record (a positive byte count), keeps waiting across read timeouts (so the SCTP reader parks on the
 * transport rather than spinning), skips DTLS control records that decrypt to no application data, and
 * reports end of stream ({@code -1}) once the transport is closed. {@link #write(ByteBuffer)} drains and
 * sends the whole remaining buffer as one record.
 *
 * @implNote This implementation composes the pure Java {@code sctp} library over the JDK {@code DTLSv1.2}
 *           engine driven by {@link VoipDtlsTransport}: the SCTP stack speaks to any {@link ByteChannel}
 *           that moves one datagram per read and per write, which is exactly the shape this adapter exposes.
 */
public final class DtlsByteChannel implements ByteChannel {
    /**
     * The logger for {@link DtlsByteChannel}.
     */
    private static final System.Logger LOGGER = Log.get(DtlsByteChannel.class);

    /**
     * The wait for one inbound DTLS record, in milliseconds. On a timeout the read keeps waiting, so a short
     * value only bounds how promptly a {@link #close()} unblocks a parked reader without busy spinning.
     */
    private static final int RECEIVE_TIMEOUT_MILLIS = 200;

    /**
     * The reusable inbound plaintext buffer, sized to the largest DTLS application record (~16 KiB), so one
     * decrypted SCTP record always fits.
     */
    private static final int MAX_RECORD_SIZE = 16 * 1024;

    /**
     * The DTLS transport whose record layer this channel drives.
     */
    private final VoipDtlsTransport dtls;

    /**
     * The reusable scratch buffer one inbound DTLS record decrypts into, owned by the single SCTP reader
     * thread that calls {@link #read(ByteBuffer)}.
     */
    private final byte[] receiveBuffer = new byte[MAX_RECORD_SIZE];

    /**
     * Whether this channel has been closed; a closed channel reports end of stream on read and rejects
     * writes.
     */
    private volatile boolean closed;

    /**
     * Constructs a channel over the given established DTLS transport.
     *
     * @param dtls the established DTLS transport records ride on
     * @throws NullPointerException if {@code dtls} is {@code null}
     */
    public DtlsByteChannel(VoipDtlsTransport dtls) {
        this.dtls = Objects.requireNonNull(dtls, "dtls cannot be null");
    }

    /**
     * Sends the buffer's whole remaining content as one DTLS application record.
     *
     * @param src the source bytes; drained and their position advanced to the limit
     * @return the number of bytes written, the buffer's remaining count on entry
     * @throws ClosedChannelException if this channel is closed
     * @throws IOException            if the DTLS engine fails to wrap the record
     * @throws NullPointerException   if {@code src} is {@code null}
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        Objects.requireNonNull(src, "src cannot be null");
        if (closed) {
            throw new ClosedChannelException();
        }
        var length = src.remaining();
        var bytes = new byte[length];
        src.get(bytes);
        dtls.send(bytes, 0, length);
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "dtls byte channel wrote {0} bytes", length);
        }
        return length;
    }

    /**
     * Reads exactly one inbound SCTP record into {@code dst}, blocking until one arrives or the transport
     * closes.
     *
     * <p>The loop keeps waiting across read timeouts (a timeout is not end of stream, just no record yet)
     * and skips DTLS control records that decrypt to no application data; it returns end of stream only
     * once this channel or the underlying DTLS transport is closed.
     *
     * @param dst the destination for the one SCTP record; must have room for a full record
     * @return the number of bytes read, always one whole SCTP record, or {@code -1} at end of stream
     * @throws IOException          if the DTLS engine fails to unwrap a record
     * @throws NullPointerException if {@code dst} is {@code null}
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {
        Objects.requireNonNull(dst, "dst cannot be null");
        while (true) {
            if (closed) {
                return -1;
            }
            var read = dtls.receive(receiveBuffer, 0, receiveBuffer.length, RECEIVE_TIMEOUT_MILLIS);
            if (read < 0) {
                // A negative result is a read timeout or a closed transport; keep waiting on a timeout so
                // the SCTP reader parks here, and report end of stream once the transport is truly closed.
                if (closed || dtls.isClosed()) {
                    return -1;
                }
                continue;
            }
            if (read == 0) {
                // A DTLS control record that decrypted to no application data: no SCTP record to deliver.
                continue;
            }
            dst.put(receiveBuffer, 0, read);
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "dtls byte channel read {0} bytes", read);
            }
            return read;
        }
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Closes this channel and the underlying DTLS transport. Idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dtls byte channel closed");
        }
        dtls.close();
    }
}
