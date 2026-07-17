package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.telemetry.log.Log;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Drives a JDK DTLS {@link SSLEngine} over a datagram seam, running the handshake with retransmission and then
 * carrying application records both ways.
 *
 * <p>The engine is a state machine driven by {@link SSLEngine#wrap} and {@link SSLEngine#unwrap} rather than a
 * blocking transport, so this class supplies the loop the WhatsApp Web relay leg needs: outbound handshake
 * flights and application records are produced by {@code wrap} and pushed to the {@link Datagrams} sink, inbound
 * records are pulled from the {@link Datagrams} source and fed to {@code unwrap}, and because the underlying
 * transport is a lossy datagram flow the handshake retransmits its last flight whenever an inbound read times
 * out. Once {@link #handshake(long)} returns the connection is established; {@link #send(byte[], int, int)}
 * encrypts one application message into a DTLS record and {@link #receive(byte[], int, int, int)} decrypts one.
 *
 * <p>The outbound side ({@code wrap}, guarded by {@link #outboundLock}) and the inbound side ({@code unwrap},
 * guarded by {@link #inboundLock}) use separate locks and separate scratch buffers, so a sender thread and the
 * receive pump run concurrently, which is the concurrency contract {@link SSLEngine} supports.
 *
 * @implNote This implementation delegates the whole record and handshake protocol to
 *           {@code SSLContext.getInstance("DTLSv1.2")} configured by {@link VoipDtlsCertificates}; only the
 *           datagram pump and the DTLS retransmission timer live here. Retransmission follows the JDK's
 *           documented DTLS model: while the engine is in {@link SSLEngineResult.HandshakeStatus#NEED_UNWRAP} and
 *           an inbound read times out, calling {@code wrap} emits the cached last flight again. The handshake is
 *           bounded by an overall deadline so a relay that stops answering fails the connection setup rather than
 *           retransmitting forever.
 */
public final class VoipDtlsTransport {
    /**
     * The logger for {@link VoipDtlsTransport}.
     */
    private static final System.Logger LOGGER = Log.get(VoipDtlsTransport.class);

    /**
     * The per read timeout, in milliseconds, the handshake waits for an inbound flight before retransmitting.
     */
    private static final int HANDSHAKE_READ_TIMEOUT_MILLIS = 400;

    /**
     * The maximum number of datagrams a single {@code wrap} burst may emit, a backstop against a misbehaving
     * engine looping without converging.
     */
    private static final int MAX_FLIGHT_DATAGRAMS = 64;

    /**
     * The empty source buffer handed to {@code wrap} when producing handshake output and to {@code unwrap} when
     * draining already buffered records.
     */
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    /**
     * The DTLS engine whose record layer this transport drives.
     */
    private final SSLEngine engine;

    /**
     * The datagram seam DTLS records leave through and arrive on.
     */
    private final Datagrams datagrams;

    /**
     * The scratch buffer that captures one outbound DTLS record from {@code wrap}, guarded by
     * {@link #outboundLock}.
     */
    private final ByteBuffer netBuffer;

    /**
     * The scratch buffer that captures the plaintext one inbound DTLS record decrypts to, guarded by
     * {@link #inboundLock}.
     */
    private final ByteBuffer appBuffer;

    /**
     * The still undecoded tail of the last inbound datagram when the peer coalesced more than one DTLS
     * record into it, so a following {@link #receive} decodes the next record rather than reading a new
     * datagram and dropping the tail; {@code null} when the last datagram was fully consumed. Guarded by
     * {@link #inboundLock}.
     */
    private ByteBuffer pendingInbound;

    /**
     * Serialises the outbound {@code wrap} side so concurrent senders cannot corrupt {@link #netBuffer}.
     */
    private final ReentrantLock outboundLock = new ReentrantLock();

    /**
     * Serialises the inbound {@code unwrap} side so the receive pump owns {@link #appBuffer} exclusively.
     */
    private final ReentrantLock inboundLock = new ReentrantLock();

    /**
     * Whether the transport has been closed, after which sends are dropped and receives report end of stream.
     */
    private volatile boolean closed;

    /**
     * The raw datagram transport the DTLS records ride on, isolating the record layer from the host UDP flow.
     */
    public interface Datagrams {
        /**
         * Sends one DTLS record datagram to the peer.
         *
         * @param record the record bytes to put on the wire
         */
        void send(byte[] record);

        /**
         * Receives one inbound DTLS record datagram, waiting up to {@code waitMillis} for one to arrive.
         *
         * @param waitMillis the maximum time to wait, in milliseconds
         * @return the inbound record bytes, or {@code null} if none arrived within the timeout
         */
        byte[] receive(int waitMillis);
    }

    /**
     * Constructs a transport over the given DTLS engine and datagram seam.
     *
     * @param engine    the DTLS engine, already configured for its role by {@link VoipDtlsCertificates}
     * @param datagrams the datagram seam records ride on
     * @throws NullPointerException if {@code engine} or {@code datagrams} is {@code null}
     */
    public VoipDtlsTransport(SSLEngine engine, Datagrams datagrams) {
        this.engine = Objects.requireNonNull(engine, "engine cannot be null");
        this.datagrams = Objects.requireNonNull(datagrams, "datagrams cannot be null");
        var session = engine.getSession();
        this.netBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
        this.appBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
    }

    /**
     * Runs the DTLS handshake to completion, retransmitting on loss until it establishes or the deadline passes.
     *
     * <p>The loop drives the engine's handshake status: it wraps and sends each outbound flight, runs delegated
     * tasks, and unwraps inbound flights; when it is waiting for the peer and the read times out it emits the
     * last flight again. The certificate pin is enforced inside {@code unwrap} by the engine's trust manager, so a
     * relay whose certificate does not pin fails the handshake here with an {@link IOException}.
     *
     * @param timeoutMillis the overall handshake deadline, in milliseconds
     * @throws IOException if the handshake fails, the peer certificate does not pin, or the deadline passes
     */
    public void handshake(long timeoutMillis) throws IOException {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dtls handshake starting, timeout={0}ms", timeoutMillis);
        }
        outboundLock.lock();
        inboundLock.lock();
        try {
            engine.beginHandshake();
            var deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
            while (true) {
                var status = engine.getHandshakeStatus();
                switch (status) {
                    case FINISHED, NOT_HANDSHAKING -> {
                        if (Log.DEBUG) {
                            LOGGER.log(Level.DEBUG, "dtls handshake completed, status={0}", status);
                        }
                        return;
                    }
                    case NEED_TASK -> runDelegatedTasks();
                    case NEED_WRAP -> emitHandshakeFlight();
                    case NEED_UNWRAP_AGAIN -> unwrapRecord(EMPTY);
                    case NEED_UNWRAP -> {
                        var datagram = datagrams.receive(HANDSHAKE_READ_TIMEOUT_MILLIS);
                        if (datagram == null) {
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG, "dtls handshake read timed out, retransmitting last flight");
                            }
                            retransmitFlight();
                        } else {
                            var source = ByteBuffer.wrap(datagram);
                            do {
                                unwrapRecord(source);
                            } while (source.hasRemaining()
                                    && (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP
                                    || engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN));
                        }
                    }
                }
                if (System.nanoTime() > deadline) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "dtls handshake timed out, timeout={0}ms", timeoutMillis);
                    }
                    throw new IOException("DTLS handshake timed out");
                }
            }
        } finally {
            inboundLock.unlock();
            outboundLock.unlock();
        }
    }

    /**
     * Encrypts one application message into a DTLS record and sends it.
     *
     * <p>A message larger than one record is split across records; a closed transport drops the message.
     *
     * @param buffer the source bytes
     * @param offset the offset of the first byte
     * @param length the number of bytes to send
     * @throws IOException          if the engine fails to wrap the message
     * @throws NullPointerException if {@code buffer} is {@code null}
     */
    void send(byte[] buffer, int offset, int length) throws IOException {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        // TODO: WA frames each outbound DTLS record through DtlsPacketEnvelope.wrapOutbound before it leaves;
        //  that envelope is not yet applied on this send path.
        if (closed) {
            return;
        }
        outboundLock.lock();
        try {
            var source = ByteBuffer.wrap(buffer, offset, length);
            while (source.hasRemaining()) {
                netBuffer.clear();
                var result = engine.wrap(source, netBuffer);
                if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
                    closed = true;
                    if (Log.DEBUG) {
                        LOGGER.log(Level.DEBUG, "dtls send found engine closed, length={0}", length);
                    }
                    return;
                }
                flushNetBuffer();
            }
        } finally {
            outboundLock.unlock();
        }
    }

    /**
     * Receives and decrypts one application message, waiting up to {@code waitMillis} for an inbound record.
     *
     * <p>A record that decrypts to no application data (a DTLS control record) yields {@code 0} so the caller
     * retries; a timeout or a closed transport yields {@code -1}. When the peer coalesces several DTLS records
     * into one datagram this decodes exactly the next record and holds the datagram's tail in
     * {@link #pendingInbound}, so a following call returns the coalesced records in order instead of dropping
     * them.
     *
     * @param buffer     the destination for the decrypted plaintext
     * @param offset     the offset at which to write the first plaintext byte
     * @param length     the maximum number of plaintext bytes to write
     * @param waitMillis the maximum time to wait for an inbound record, in milliseconds
     * @return the number of plaintext bytes written, {@code 0} if the record carried none, or {@code -1} on
     *         timeout or close
     * @throws IOException          if the engine fails to unwrap the record or the peer closed the connection
     * @throws NullPointerException if {@code buffer} is {@code null}
     */
    int receive(byte[] buffer, int offset, int length, int waitMillis) throws IOException {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        if (closed) {
            return -1;
        }
        inboundLock.lock();
        try {
            if (pendingInbound == null || !pendingInbound.hasRemaining()) {
                var datagram = datagrams.receive(waitMillis);
                if (datagram == null) {
                    return -1;
                }
                pendingInbound = ByteBuffer.wrap(datagram);
            }
            var before = pendingInbound.remaining();
            var produced = unwrapRecord(pendingInbound);
            if (!pendingInbound.hasRemaining() || pendingInbound.remaining() == before) {
                pendingInbound = null;
            }
            if (produced <= 0) {
                return produced;
            }
            var count = Math.min(length, produced);
            appBuffer.flip();
            appBuffer.get(buffer, offset, count);
            appBuffer.clear();
            return count;
        } finally {
            inboundLock.unlock();
        }
    }

    /**
     * Returns whether the transport has been closed, so a datagram adapter draining it can tell a
     * permanently closed engine (report end of stream) apart from a transient read timeout (keep
     * waiting).
     *
     * @return {@code true} once the transport is closed, either by {@link #close()} or by a peer
     *         {@code close_notify} observed while unwrapping a record
     */
    boolean isClosed() {
        return closed;
    }

    /**
     * Closes the transport, sending a {@code close_notify} and dropping subsequent traffic.
     *
     * <p>The method is idempotent.
     */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dtls transport closing");
        }
        outboundLock.lock();
        try {
            engine.closeOutbound();
            retransmitFlight();
        } catch (IOException exception) {
            // A failure to flush the close_notify is best effort on teardown and must not mask the caller's
            // own close path.
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "dtls close_notify flush failed", exception);
            }
        } finally {
            outboundLock.unlock();
        }
    }

    /**
     * Wraps and sends the current outbound handshake flight, stopping as soon as the engine leaves the
     * {@link SSLEngineResult.HandshakeStatus#NEED_WRAP} state.
     *
     * <p>This is the fresh flight path: it must stop the moment the flight is complete, because calling
     * {@code wrap} again once the engine is waiting on the peer emits the flight again as a retransmission, which
     * would flood the peer with duplicates.
     *
     * @throws IOException if the engine reports an unexpected closed status while producing handshake output
     */
    private void emitHandshakeFlight() throws IOException {
        for (var emitted = 0;
             emitted < MAX_FLIGHT_DATAGRAMS
                     && engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP;
             emitted++) {
            netBuffer.clear();
            var result = engine.wrap(EMPTY, netBuffer);
            if (result.getStatus() == SSLEngineResult.Status.CLOSED && !closed) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "dtls engine closed unexpectedly while producing handshake output");
                }
                throw new IOException("DTLS engine closed while producing handshake output");
            }
            flushNetBuffer();
        }
    }

    /**
     * Wraps and sends every record the engine currently has pending, the retransmission and {@code close_notify}
     * path.
     *
     * <p>Unlike {@link #emitHandshakeFlight()}, this keeps wrapping while the engine produces output even when
     * it is waiting on the peer, so it emits the cached last flight again on a handshake read timeout and flushes
     * the {@code close_notify} at teardown.
     *
     * @throws IOException if the engine reports an unexpected closed status while producing output
     */
    private void retransmitFlight() throws IOException {
        for (var emitted = 0; emitted < MAX_FLIGHT_DATAGRAMS; emitted++) {
            netBuffer.clear();
            var result = engine.wrap(EMPTY, netBuffer);
            if (result.getStatus() == SSLEngineResult.Status.CLOSED && !closed) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "dtls engine closed unexpectedly while retransmitting");
                }
                throw new IOException("DTLS engine closed while producing handshake output");
            }
            if (result.bytesProduced() == 0) {
                return;
            }
            flushNetBuffer();
        }
    }

    /**
     * Unwraps one inbound record, returning the plaintext byte count and running any delegated tasks the record
     * triggers.
     *
     * @param source the record bytes, or {@link #EMPTY} to drain already buffered records
     * @return the number of plaintext bytes the record decrypted to, left in {@link #appBuffer}
     * @throws IOException if the engine fails to unwrap the record or the peer closed the connection
     */
    private int unwrapRecord(ByteBuffer source) throws IOException {
        appBuffer.clear();
        var result = engine.unwrap(source, appBuffer);
        if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            closed = true;
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dtls peer sent close_notify");
            }
            return -1;
        }
        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            runDelegatedTasks();
        }
        return result.bytesProduced();
    }

    /**
     * Runs every delegated task the engine has queued.
     */
    private void runDelegatedTasks() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            task.run();
        }
    }

    /**
     * Sends the datagram currently staged in {@link #netBuffer} to the datagram seam and resets the buffer.
     */
    private void flushNetBuffer() {
        if (netBuffer.position() == 0) {
            return;
        }
        netBuffer.flip();
        var record = new byte[netBuffer.remaining()];
        netBuffer.get(record);
        datagrams.send(record);
    }
}
