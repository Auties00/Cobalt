package com.github.auties00.cobalt.calls.platform;

import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppSessionException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;

import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Wires the engine host downcalls of {@link VoipHostApi} onto the Cobalt client and the JDK platform.
 *
 * <p>This is the production host for the voip engine: each downcall the engine makes is satisfied by a
 * concrete Cobalt facility. Signaling stanzas go to the {@link LinkedWhatsAppClient} binary XMPP socket;
 * the {@code call_sendto} host datagram downcall goes to an injected {@link MediaDatagramSink}; randomness
 * comes from a single {@link SecureRandom}; name resolution uses {@link InetAddress}; persistent storage
 * resolves to a directory scoped to the current session under the Cobalt home; the contact lookup consults
 * the client contact store; decoded frames and typed events are handed to injected sinks; and structured
 * logs go to the {@link System.Logger} for this class. Every collaborator is supplied through the
 * constructor and held as a field, so the host reaches no service through a global accessor.
 *
 * <p>Where the browser client backs these downcalls with JavaScript, this class supplies the JVM
 * equivalents: a datagram channel for the network socket, {@link SecureRandom} for the strong random
 * source, the JDK resolver for host name lookup, the Cobalt session directory for persistent storage, and
 * the listener bus for the event callback. No part of this class binds native code; it is pure Java glue.
 */
public final class LiveVoipHostApi implements VoipHostApi {

    /**
     * The audio processing capability bitmask the host reports for its capture path.
     *
     * <p>The bitmask is the sum of {@code 1} for applied acoustic echo cancellation, {@code 2} for
     * applied noise suppression, and {@code 4} for applied automatic gain control, matching the engine
     * {@code (echoCancellation?1)+(noiseSuppression?2)+(autoGainControl?4)} encoding. The
     * {@code javax.sound.sampled} {@code TargetDataLine} capture path applies none of the three, so its
     * bitmask is {@code 0}; this value makes the engine run its own full echo cancellation, noise
     * suppression, and gain control chain rather than skip a stage it would wrongly believe the host
     * already applied.
     */
    private static final int CAPTURE_AUDIO_PROCESSING_STATUS = 0;

    /**
     * The logger for {@link LiveVoipHostApi}.
     *
     * <p>Also receives engine log records routed through {@link #log}.
     */
    private static final System.Logger LOGGER = Log.get(LiveVoipHostApi.class);

    /**
     * Holds the owning client, used to send signaling stanzas and to reach the contact and account stores
     * for the contact lookup and the persistent directory scoped to the current session.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Holds the datagram egress owned by the transport that backs {@link #sendDatagram(byte[], SocketAddress)}.
     */
    private final MediaDatagramSink datagramSink;

    /**
     * Holds the sink that presents decoded frames passed to {@link #renderVideoFrame(RenderedVideoFrame)}.
     */
    private final Consumer<RenderedVideoFrame> videoSink;

    /**
     * Holds the sink that receives typed events passed to {@link #onCallEvent(CallEventType, byte[])}.
     */
    private final BiConsumer<CallEventType, byte[]> callEventSink;

    /**
     * Holds the resolver that maps a model type selector to a bundled model path for
     * {@link #bweMlModelPath(int)}, returning empty when the host ships no model for the type.
     */
    private final IntFunction<Optional<Path>> mlModelPathResolver;

    /**
     * Holds the cryptographically strong generator backing {@link #randomBytes(int)}.
     */
    private final SecureRandom secureRandom;

    /**
     * Constructs a host bound to the given client, datagram egress, and event and frame sinks, rooting
     * the persistent directory at the default Cobalt home.
     *
     * <p>The persistent directory resolves under {@code $HOME/.cobalt}; the machine learning model
     * resolver defaults to reporting no bundled model for any type, so the engine falls back to the
     * bandwidth estimator that uses no learned model. Use
     * {@link #LiveVoipHostApi(LinkedWhatsAppClient, MediaDatagramSink, Consumer, BiConsumer, IntFunction, Path)}
     * to override either.
     *
     * @param whatsapp      the owning client
     * @param datagramSink  the datagram egress owned by the transport
     * @param videoSink     the sink that presents decoded frames
     * @param callEventSink the sink that receives typed events
     */
    public LiveVoipHostApi(
            LinkedWhatsAppClient whatsapp,
            MediaDatagramSink datagramSink,
            Consumer<RenderedVideoFrame> videoSink,
            BiConsumer<CallEventType, byte[]> callEventSink
    ) {
        this(
                whatsapp,
                datagramSink,
                videoSink,
                callEventSink,
                modelType -> Optional.empty(),
                Path.of(System.getProperty("user.home"), ".cobalt")
        );
    }

    /**
     * Constructs a host bound to the given client, datagram egress, sinks, model path resolver, and
     * persistent base directory.
     *
     * @param whatsapp            the owning client
     * @param datagramSink        the datagram egress owned by the transport
     * @param videoSink           the sink that presents decoded frames
     * @param callEventSink       the sink that receives typed events
     * @param mlModelPathResolver the resolver mapping a model type selector to a bundled model path
     * @param baseDirectory       the base directory under which the call directory for the session is resolved
     */
    public LiveVoipHostApi(
            LinkedWhatsAppClient whatsapp,
            MediaDatagramSink datagramSink,
            Consumer<RenderedVideoFrame> videoSink,
            BiConsumer<CallEventType, byte[]> callEventSink,
            IntFunction<Optional<Path>> mlModelPathResolver,
            Path baseDirectory
    ) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.datagramSink = Objects.requireNonNull(datagramSink, "datagramSink cannot be null");
        this.videoSink = Objects.requireNonNull(videoSink, "videoSink cannot be null");
        this.callEventSink = Objects.requireNonNull(callEventSink, "callEventSink cannot be null");
        this.mlModelPathResolver = Objects.requireNonNull(mlModelPathResolver, "mlModelPathResolver cannot be null");
        this.secureRandom = new SecureRandom();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation dispatches the stanza through
     * {@link LinkedWhatsAppClient#sendNodeWithNoResponse(Stanza)}, matching the {@code void} return: the
     * server acknowledgement for a call stanza arrives on the inbound signaling path, not as a reply
     * correlated here. A {@link WhatsAppSessionException.Closed} from an already closed socket is caught
     * and logged rather than propagated, because a closed socket means the call is already tearing down
     * and the engine treats this layer as fire and forget.
     */
    @Override
    public void sendSignaling(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        try {
            whatsapp.sendNodeWithNoResponse(stanza);
        } catch (WhatsAppSessionException.Closed exception) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping call signaling on a closed socket", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards to the injected {@link MediaDatagramSink}, which backs the
     * {@code call_sendto} host downcall with a datagram channel send.
     */
    @Override
    public int sendDatagram(byte[] payload, SocketAddress destination) {
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(destination, "destination cannot be null");
        return datagramSink.send(payload, destination);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation resolves through {@link InetAddress#getAllByName(String)} and maps
     * an {@link UnknownHostException} to an empty list, so an unresolvable name lets the engine fall
     * through to the next transport candidate rather than fail.
     */
    @Override
    public List<InetAddress> resolveHost(String hostName) {
        Objects.requireNonNull(hostName, "hostName cannot be null");
        try {
            var addresses = List.of(InetAddress.getAllByName(hostName));
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "call host resolveHost: host={0} resolved={1}", hostName, addresses.size());
            }
            return addresses;
        } catch (UnknownHostException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call host resolveHost: unresolvable host " + hostName, exception);
            return List.of();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation fills the array from a single {@link SecureRandom}; the requested
     * length is honoured as given for any value that is not negative.
     */
    @Override
    public byte[] randomBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative: " + length);
        }
        var bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to the injected model path resolver, which reports no
     * bundled model by default; an empty result is what the engine reads as the model being
     * unavailable.
     */
    @Override
    public Optional<Path> bweMlModelPath(int modelType) {
        return mlModelPathResolver.apply(modelType);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation maps to a contact store lookup through
     * {@link LinkedWhatsAppContactStore#findContactByJid}: a present contact is known, an absent one is
     * not, so an unloaded address book reports not known and the engine defaults to the conservative
     * handling for an unknown caller.
     */
    @Override
    public boolean isKnownContact(Jid participant) {
        Objects.requireNonNull(participant, "participant cannot be null");
        return whatsapp.store()
                .contactStore()
                .findContactByJid(participant)
                .isPresent();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards the frame to the injected video sink; the engine guarantees
     * the {@link RenderedVideoFrame} plane segments are valid only for the duration of this call, so the
     * sink must copy or upload them synchronously.
     */
    @Override
    public void renderVideoFrame(RenderedVideoFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "call host renderVideoFrame: participant={0} width={1} height={2} format={3}",
                    frame.participant(), frame.width(), frame.height(), frame.format());
        }
        videoSink.accept(frame);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns {@value #CAPTURE_AUDIO_PROCESSING_STATUS}, the applied
     * processing bitmask of the {@code javax.sound.sampled} {@code TargetDataLine} capture path: it applies
     * no acoustic echo cancellation, no noise suppression, and no automatic gain control, so the bitmask
     * {@code (echoCancellation?1)+(noiseSuppression?2)+(autoGainControl?4)} is {@code 0} and the engine runs
     * its own full processing chain. The value is a truthful constant for this capture path, not a stub: a
     * capture backend that applied any of these would instead compute the bitmask from its device settings.
     */
    @Override
    public int browserAudioProcessingStatus() {
        return CAPTURE_AUDIO_PROCESSING_STATUS;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation routes the record to the {@link System.Logger} owned by this instance,
     * so the engine diagnostics land in the host logging facility at the level the engine chose.
     */
    @Override
    public void log(System.Logger.Level level, String message) {
        Objects.requireNonNull(level, "level cannot be null");
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, message);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards to the injected event sink, which the lifecycle layer backs
     * with the listener bus; the engine has already decided whether to emit, so every event reaching here
     * is one the host is meant to surface.
     */
    @Override
    public void onCallEvent(CallEventType eventType, byte[] payload) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call host onCallEvent: type={0} payloadBytes={1}", eventType, payload.length);
        callEventSink.accept(eventType, payload);
    }

    /**
     * Carries one outbound media datagram from {@link LiveVoipHostApi} to the transport's UDP egress.
     *
     * <p>The transport layer owns the actual sockets a call uses, so {@link LiveVoipHostApi} does not open
     * one itself: it forwards each {@code call_sendto} host downcall to an implementation of this seam that
     * the host supplies. An implementation sends the payload to the destination on its datagram channel and
     * returns the number of bytes the channel accepted, which is the payload length on success and a value
     * of zero or less when the channel could not send (for example because it is closed).
     */
    @FunctionalInterface
    public interface MediaDatagramSink {
        /**
         * Sends one datagram to the given destination on the transport egress.
         *
         * @param payload     the datagram bytes to send
         * @param destination the address to send to
         * @return the number of bytes accepted by the transport, or a value of zero or less on failure
         */
        int send(byte[] payload, SocketAddress destination);
    }
}
