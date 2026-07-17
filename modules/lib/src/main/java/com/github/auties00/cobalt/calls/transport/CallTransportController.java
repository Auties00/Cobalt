package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.calls.platform.VoipCryptoNative;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.datachannel.RxSubscriptions;

import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import com.github.auties00.cobalt.calls.transport.congestion.bwe.signal.BweHistory;
import com.github.auties00.cobalt.calls.transport.datachannel.AppDataController;
import com.github.auties00.cobalt.calls.transport.stun.StunMessage;
import com.github.auties00.cobalt.calls.transport.subscription.LiveSubscriptionPublisher;
import com.github.auties00.cobalt.calls.transport.subscription.SubscriptionEnvelope;
import com.github.auties00.cobalt.calls.transport.subscription.SubscriptionStunAttribute;
import com.github.auties00.cobalt.calls.transport.warp.BweConfigSender;

/**
 * Drives one call's media transport startup and runs its transport state machine.
 *
 * <p>The controller sequences a call's media plane from the bootstrap request to flowing media: it posts
 * the {@code start_session_request} through the {@link LiveCallHttpSignaler}, starts the
 * {@link MediaTransport} (the ICE/DTLS/SCTP data channel web transport), seeds the bandwidth estimate from
 * the {@link BweHistory}, and reacts to the transport's {@link TransportEvent}s by advancing its
 * {@link State} and notifying the application. Inbound application data bytes are delivered by the data
 * channel layer ({@link AppDataController}) driven off the SCTP association rather than through this
 * controller's event path; a {@link TransportEvent#RX_APP_DATA} only marks liveness here.
 *
 * <p>Once the relay leg is active the controller keeps it live and adapts it: {@link #tick(long)} drives
 * the relay keepalive watchdog (through the relay transport) and the periodic receive subscription resend
 * (through the {@link LiveSubscriptionPublisher}); {@link #onDownlinkBweDrop(int, int)} sends a standalone
 * WARP bandwidth configuration when the downlink estimate drops; and a relay bind failure after the leg was
 * active is surfaced for failover. The relay election that precedes relay create success runs inside the
 * transport through its relay binder; this controller drives only the upkeep that follows once the leg is
 * active.
 *
 * <p>The controller is driven by the single call transport thread; the transport hands it events on that
 * same thread.
 *
 * @implNote This implementation folds the intermediate data channel ready milestone of the Web P2P path
 *           into {@link State#RELAY_CREATE_SUCCESS} and gates the data channel setup steps on
 *           {@link TransportEvent#RELAY_CREATE_SUCCESS}. A startup failure surfaces as a non fatal
 *           {@link WhatsAppCallException.DataChannel} rather than an inline recovery.
 */
public final class CallTransportController implements AutoCloseable {
    /**
     * The logger for {@link CallTransportController}.
     */
    private static final System.Logger LOGGER = Log.get(CallTransportController.class);

    /**
     * Holds the signaler that posts the call bootstrap request.
     */
    private final LiveCallHttpSignaler signaler;

    /**
     * Holds the media transport this controller drives.
     */
    private final MediaTransport transport;

    /**
     * Holds the bandwidth estimate history seeding this call.
     */
    private final BweHistory bweHistory;

    /**
     * Holds the consumer notified of each transport state transition.
     */
    private final Consumer<State> stateObserver;

    /**
     * Holds the subscription publisher whose receive subscription resend this controller drives, or
     * {@code null} when the call publishes no subscriptions.
     *
     * <p>Present on a group/SFU call so {@link #tick(long)} can resend the cached receive subscription on
     * the relay leg; {@code null} on a one to one call, which subscribes to no SFU.
     */
    private final LiveSubscriptionPublisher subscriptionPublisher;

    /**
     * Holds the relay {@code <key>} keying the {@code 0x0003} subscription envelope's {@code HMAC-SHA1}
     * message integrity, or {@code null} when the call publishes no subscription.
     */
    private final byte[] relayKey;

    /**
     * Holds the selected relay endpoint's reflexive transport address carried as the {@code 0x0003}
     * subscription envelope's {@code 0x0016} XOR MAPPED ADDRESS, or {@code null} when the call publishes no
     * subscription.
     */
    private final InetSocketAddress relayReflexiveAddress;

    /**
     * Holds the current transport state.
     */
    private volatile State state;

    /**
     * Holds whether the relay leg has ever reached the active state, so a later bind failure is treated as
     * failover rather than an initial bind failure.
     */
    private boolean everActive;

    /**
     * Holds whether the controller has been closed.
     */
    private boolean closed;

    /**
     * Constructs a controller over the given collaborators with no subscription publisher.
     *
     * <p>Equivalent to {@link #CallTransportController(LiveCallHttpSignaler, MediaTransport, BweHistory, Consumer, LiveSubscriptionPublisher, byte[], InetSocketAddress)}
     * with a {@code null} publisher and no relay subscription envelope material; suitable for a one to one
     * call that subscribes to no SFU.
     *
     * @param signaler      the signaler posting the call bootstrap request
     * @param transport     the media transport to drive
     * @param bweHistory    the bandwidth estimate history seeding this call
     * @param stateObserver the consumer notified of each state transition
     * @throws NullPointerException if any argument is {@code null}
     */
    public CallTransportController(LiveCallHttpSignaler signaler,
                                  MediaTransport transport,
                                  BweHistory bweHistory,
                                  Consumer<State> stateObserver) {
        this(signaler, transport, bweHistory, stateObserver, null, null, null);
    }

    /**
     * Constructs a controller over the given collaborators and subscription publisher with no relay
     * subscription envelope material.
     *
     * <p>Equivalent to {@link #CallTransportController(LiveCallHttpSignaler, MediaTransport, BweHistory, Consumer, LiveSubscriptionPublisher, byte[], InetSocketAddress)}
     * with a {@code null} relay key and reflexive address; the controller drives the resend cadence but cannot
     * emit a {@code 0x0003} envelope until the relay subscription envelope material is supplied.
     *
     * @param signaler              the signaler posting the call bootstrap request
     * @param transport             the media transport to drive
     * @param bweHistory            the bandwidth estimate history seeding this call
     * @param stateObserver         the consumer notified of each state transition
     * @param subscriptionPublisher the subscription publisher whose receive subscription resend the
     *                              controller drives, or {@code null} when the call publishes none
     * @throws NullPointerException if {@code signaler}, {@code transport}, {@code bweHistory}, or
     *                              {@code stateObserver} is {@code null}
     */
    public CallTransportController(LiveCallHttpSignaler signaler,
                                  MediaTransport transport,
                                  BweHistory bweHistory,
                                  Consumer<State> stateObserver,
                                  LiveSubscriptionPublisher subscriptionPublisher) {
        this(signaler, transport, bweHistory, stateObserver, subscriptionPublisher, null, null);
    }

    /**
     * Constructs a controller over the given collaborators, subscription publisher, and relay subscription
     * envelope material.
     *
     * @param signaler              the signaler posting the call bootstrap request
     * @param transport             the media transport to drive
     * @param bweHistory            the bandwidth estimate history seeding this call
     * @param stateObserver         the consumer notified of each state transition
     * @param subscriptionPublisher the subscription publisher whose receive subscription resend the
     *                              controller drives, or {@code null} when the call publishes none
     * @param relayKey              the relay {@code <key>} keying the {@code 0x0003} subscription envelope's
     *                              {@code HMAC-SHA1} message integrity, or {@code null} when the call
     *                              publishes none
     * @param relayReflexiveAddress the selected relay endpoint's reflexive transport address carried as the
     *                              {@code 0x0003} envelope's {@code 0x0016} XOR MAPPED ADDRESS, or
     *                              {@code null} when the call publishes none
     * @throws NullPointerException if {@code signaler}, {@code transport}, {@code bweHistory}, or
     *                              {@code stateObserver} is {@code null}
     */
    public CallTransportController(LiveCallHttpSignaler signaler,
                                  MediaTransport transport,
                                  BweHistory bweHistory,
                                  Consumer<State> stateObserver,
                                  LiveSubscriptionPublisher subscriptionPublisher,
                                  byte[] relayKey,
                                  InetSocketAddress relayReflexiveAddress) {
        this.signaler = Objects.requireNonNull(signaler, "signaler cannot be null");
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        this.bweHistory = Objects.requireNonNull(bweHistory, "bweHistory cannot be null");
        this.stateObserver = Objects.requireNonNull(stateObserver, "stateObserver cannot be null");
        this.subscriptionPublisher = subscriptionPublisher;
        this.relayKey = relayKey == null ? null : relayKey.clone();
        this.relayReflexiveAddress = relayReflexiveAddress;
        this.state = State.UNINITIALIZED;
        transport.onTransportEvent(this::onTransportEvent);
    }

    /**
     * Brings up the call's media transport.
     *
     * <p>This posts the {@code start_session_request}, transitions to {@link State#PREPARED}, and starts
     * the media transport, which begins relay election (relay path) or ICE and the DTLS handshake (Web P2P
     * path). The connection becoming usable is reported asynchronously through the transport events that
     * advance the state. The calling virtual thread blocks for the bootstrap round trip.
     *
     * @return the bootstrap result the signaler returned
     * @throws IllegalStateException if the controller has been closed or bring up has already started
     * @throws WhatsAppCallException if the bootstrap request fails or the transport cannot start
     */
    public LiveCallHttpSignaler.StartSessionResult start() {
        ensureOpen();
        if (state != State.UNINITIALIZED) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "transport bring-up already started in state {0}", state);
            }
            throw new IllegalStateException("transport bring-up already started in state " + state);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting call transport bring-up");
        var result = signaler.sendStartSessionRequest();
        transitionTo(State.PREPARED);
        transport.start();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call transport bring-up started");
        return result;
    }

    /**
     * Returns the current transport state.
     *
     * @return the current state
     */
    public State state() {
        return state;
    }

    /**
     * Returns whether the transport has reached a state in which media flows in both directions.
     *
     * @return {@code true} when the state is {@link State#TX_TRAFFIC_STARTED}
     */
    public boolean isBidirectional() {
        return state == State.TX_TRAFFIC_STARTED;
    }

    /**
     * Returns the bandwidth estimate seed for this call, in kilobits per second.
     *
     * <p>This is the mean of the call's recent bandwidth history, used to start the estimator above the
     * conservative floor; it is {@code 0} when the history is empty.
     *
     * @return the seed estimate in kilobits per second, or {@code 0} when no history exists
     */
    public int bandwidthSeedKbps() {
        return bweHistory.seedEstimateKbps();
    }

    /**
     * Records an uplink authorization signal so the controller can begin the send pipeline.
     *
     * <p>The controller transitions to {@link State#TX_TRAFFIC_STARTED} once downlink media has started
     * and uplink is authorized; a caller invokes this when the chosen pair or relay is ready to receive
     * the client's media.
     *
     * @throws IllegalStateException if the controller has been closed
     */
    public void authorizeUplink() {
        ensureOpen();
        if (state == State.RX_TRAFFIC_STARTED) {
            transitionTo(State.TX_TRAFFIC_STARTED);
        }
    }

    /**
     * Drives the relay leg's periodic upkeep for one transport thread tick.
     *
     * <p>Once the relay leg is active this drives the relay keepalive watchdog through the relay transport
     * (which emits a keepalive ping on its cadence and surfaces a relay bind failure when the relay stops
     * answering) and resends the cached receive subscription through the subscription publisher's resend
     * timer. Before the leg is active, on the Web P2P path, or after {@link #close()}, this is a no op. It
     * is called by the single transport thread on its tick cadence.
     *
     * @param nowNanos the current time in the transport thread's monotonic nanosecond timebase
     */
    public void tick(long nowNanos) {
        if (closed || !isRelayActive()) {
            return;
        }
        if (transport instanceof LiveRelayTransport relay) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "ticking relay leg upkeep, state {0}", state);
            relay.tick(nowNanos);
        }
    }

    /**
     * Sends a standalone WARP bandwidth estimation configuration when the downlink estimate drops.
     *
     * <p>When the client's downlink bandwidth estimate falls, it asks the selective forwarding unit to
     * lower the minimum rate it allocates to remote senders by sending a standalone WARP message carrying
     * the bandwidth report attribute. This builds that message through {@link BweConfigSender} and ships it
     * over the transport; it is a no op before the relay leg is active, on a transport that does not carry
     * WARP, or after {@link #close()}. The client never accepts an inbound server BWE config; this is the
     * outbound direction only.
     *
     * @param index            the report index byte, in {@code 0..255}
     * @param minRemoteBweKbps the minimum remote bandwidth estimate to request, in kilobits per second,
     *                         in {@code 0..65535}
     * @throws IllegalArgumentException if {@code index} or {@code minRemoteBweKbps} is out of range
     */
    public void onDownlinkBweDrop(int index, int minRemoteBweKbps) {
        if (closed || !isRelayActive()) {
            return;
        }
        // TODO: no production path drives onDownlinkBweDrop yet; it builds a BweConfigSender message and
        //  sends it as a standalone WARP, but it is not yet fed by a live downlink estimator.
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "downlink bwe dropped, sending warp config index {0}, min remote bwe {1}kbps",
                    index, minRemoteBweKbps);
        }
        var message = BweConfigSender.build(index, minRemoteBweKbps);
        try {
            transport.sendStandaloneWarp(message);
        } catch (IllegalStateException unsupported) {
            // The Web P2P transport does not carry WARP and rejects a standalone WARP send; the downlink
            // BWE config is a relay path control message, so an unsupported transport simply skips it.
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "transport does not carry warp, skipping bwe config send");
        }
    }

    /**
     * Publishes the given receive subscription and sends it on the relay leg when it changed.
     *
     * <p>Frames the subscription through the {@link LiveSubscriptionPublisher} (which suppresses an unchanged
     * resend and arms the resend timer) and ships the framed attribute on the relay leg when the publish
     * was not suppressed. It is a no op when the call publishes no subscriptions, before the relay leg is
     * active, or after {@link #close()}.
     *
     * @param rxSubscriptions the receive subscription to publish; never {@code null}
     * @param nowNanos        the current time in the resend timer's nanosecond timebase
     * @return whether a framed attribute was produced (the subscription changed and was published)
     * @throws NullPointerException if {@code rxSubscriptions} is {@code null}
     */
    public boolean resendSubscription(RxSubscriptions rxSubscriptions, long nowNanos) {
        Objects.requireNonNull(rxSubscriptions, "rxSubscriptions cannot be null");
        if (closed || subscriptionPublisher == null || !isRelayActive()) {
            return false;
        }
        var attribute = subscriptionPublisher.publishRxSubscription(rxSubscriptions, nowNanos);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "resend subscription evaluated, sent {0}", attribute.isPresent());
        }
        attribute.ifPresent(this::sendSubscriptionAttribute);
        return attribute.isPresent();
    }

    /**
     * Returns the subscription publisher this controller drives, if one was supplied.
     *
     * @return an {@link Optional} holding the subscription publisher, or empty on a one to one call
     */
    public Optional<LiveSubscriptionPublisher> subscriptionPublisher() {
        return Optional.ofNullable(subscriptionPublisher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Idempotent: closes the subscription publisher and the media transport, transitions to
     * {@link State#CLOSED}, and does nothing on a second call.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing call transport controller, state {0}", state);
        closed = true;
        if (subscriptionPublisher != null) {
            subscriptionPublisher.close();
        }
        transport.close();
        transitionTo(State.CLOSED);
    }

    /**
     * Reacts to one transport event, advancing the state machine.
     *
     * <p>A relay create success moves to {@link State#RELAY_CREATE_SUCCESS}; downlink traffic start to
     * {@link State#RX_TRAFFIC_STARTED} and marks the leg as having been active; uplink start to
     * {@link State#TX_TRAFFIC_STARTED}; a relay bind failure to {@link State#RELAY_BINDS_FAILED}. Traffic
     * stop events move back toward the stopped states. An inbound application data event marks liveness only;
     * the bytes are delivered by the data channel layer, not through this path. A relay bind failure that
     * arrives after the leg was active is the relay failing over a live leg rather than an initial bind
     * failure; the state is the same, but the {@code everActive} flag lets the observer distinguish a
     * failover from a cold bind failure.
     *
     * @param event the transport event to react to
     */
    private void onTransportEvent(TransportEvent event) {
        if (closed) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call transport event {0} in state {1}", event, state);
        switch (event) {
            case RELAY_CREATE_SUCCESS -> transitionTo(State.RELAY_CREATE_SUCCESS);
            case RX_TRAFFIC_STARTED -> {
                everActive = true;
                transitionTo(State.RX_TRAFFIC_STARTED);
            }
            case TX_TRAFFIC_START -> transitionTo(State.TX_TRAFFIC_STARTED);
            case RX_TRAFFIC_STOPPED, TX_TRAFFIC_STOPPED -> transitionTo(State.RELAY_CREATE_SUCCESS);
            case RELAY_BINDS_FAILED -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "relay bind failed, ever active {0}", everActive);
                }
                transitionTo(State.RELAY_BINDS_FAILED);
            }
            case RX_APP_DATA -> {
                // The transport surfaces an inbound application data arrival; the bytes themselves are
                // delivered by the data channel layer, so the controller only needs to mark liveness.
            }
        }
    }

    /**
     * Returns whether the relay leg is currently carrying media, the gate for periodic upkeep.
     *
     * <p>The leg is active in the downlink started, uplink started, or relay create success states once
     * downlink traffic has ever started; the {@code everActive} guard keeps the keepalive and resend from
     * running on a leg that only reached relay create success without media flowing.
     *
     * @return {@code true} when the relay leg is active and its upkeep should run
     */
    private boolean isRelayActive() {
        var current = state;
        return everActive
                && (current == State.RX_TRAFFIC_STARTED
                || current == State.TX_TRAFFIC_STARTED
                || current == State.RELAY_CREATE_SUCCESS);
    }

    /**
     * Ships one framed subscription attribute inside a {@code 0x0003} subscription envelope over the
     * transport's SCTP data channel.
     *
     * <p>Wraps the framed subscription attribute in the {@code type 0x0003 + magic + txid + subscription +
     * WA_XOR_MAPPED_ADDRESS(0x0016) + MESSAGE_INTEGRITY(0x0008)} envelope
     * ({@link SubscriptionEnvelope#subscriptionEnvelope(byte[], StunMessage.Attribute, InetSocketAddress, byte[])})
     * keyed by the relay {@code <key>} this controller was constructed with, and writes it as one SCTP DATA
     * message through the relay transport's data channel send seam. The relay's own envelopes additionally
     * carry a leading {@code 0x4000} WARP control attribute this client does not emit; its body is sealed by
     * the relay hop by hop SRTP layer with a seal that cannot be reproduced (see {@link SubscriptionEnvelope}),
     * and the SFU treats it as an optional piggybacked rate control report. It is a no op when this controller
     * was given no relay key or reflexive address, or the transport is not the relay transport that carries
     * SCTP DATA application messages.
     *
     * @param attribute the framed subscription attribute to ship
     */
    private void sendSubscriptionAttribute(SubscriptionStunAttribute attribute) {
        if (relayKey == null || relayReflexiveAddress == null
                || !(transport instanceof LiveRelayTransport relay)) {
            return;
        }
        var framed = new StunMessage.Attribute(attribute.attributeType(), attribute.value());
        var envelope = SubscriptionEnvelope.subscriptionEnvelope(relayKey, framed, relayReflexiveAddress,
                VoipCryptoNative.randomBytes(StunMessage.TRANSACTION_ID_LENGTH));
        if (Log.TRACE) LOGGER.log(Level.TRACE, "sending subscription envelope, {0} bytes", envelope.length);
        relay.sendAppData(envelope);
    }

    /**
     * Transitions to a new state and notifies the observer when the state actually changes.
     *
     * @param next the state to move to
     */
    private void transitionTo(State next) {
        if (state == next) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call transport state {0} -> {1}", state, next);
        state = next;
        stateObserver.accept(next);
    }

    /**
     * Validates that the controller is open.
     *
     * @throws IllegalStateException if the controller has been closed
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("transport controller is closed");
        }
    }

    /**
     * Enumerates the states of the call transport bring up sequence.
     *
     * @implNote This implementation folds the intermediate data channel ready state of the Web P2P path into
     *           {@link #RELAY_CREATE_SUCCESS} because the relay path, the default, has no separate data
     *           channel ready milestone.
     */
    public enum State {
        /**
         * The transport has not begun bring up.
         */
        UNINITIALIZED,

        /**
         * The bootstrap request was posted and the transport descriptor is prepared.
         */
        PREPARED,

        /**
         * The relay was created (or the Web P2P connection reached its ready milestone).
         */
        RELAY_CREATE_SUCCESS,

        /**
         * Downlink media has started for the winning pair or relay.
         */
        RX_TRAFFIC_STARTED,

        /**
         * Uplink media has started; the transport carries media in both directions.
         */
        TX_TRAFFIC_STARTED,

        /**
         * No relay answered the bind requests; the media plane could not come up.
         */
        RELAY_BINDS_FAILED,

        /**
         * The transport is closed.
         */
        CLOSED
    }
}
