package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.transport.datachannel.AppDataController;
import com.github.auties00.cobalt.calls.transport.RelayLatencyState;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaPlane;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.GroupCallOutbound;
import com.github.auties00.cobalt.calls.engine.IncomingMessageRouter;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallContextRegistry;

/**
 * Holds the lifecycle controller's per call orchestration state for one in flight call.
 *
 * <p>This is the controller owned handle that ties together everything the {@link LifecycleController}
 * needs to drive a single call across its phases: the public {@link Call} data view the application
 * observes, the raw 32 byte call key the media plane keys from, the decoded inbound
 * {@link OfferStanza} a callee answers from, the relay block once it is learned, the live
 * {@link MediaPlane.Session} once the media plane is up, the peer device JIDs an outbound call fans
 * a terminate to, the peer device JID that authors the peer's signaling, and the controller's own mirror
 * of the call's internal {@link CallLifecycleState}. It is the lightweight orchestration record the
 * controller registers per call; it is distinct from the heavyweight engine call context the state and
 * timer units own, carrying only the wiring the controller threads between the signaling, crypto,
 * transport, and media units.
 *
 * <p>Each handle owns its own {@link ReentrantLock}, the per call info mutex analogue: the controller
 * holds it while it reads and mutates a single call's orchestration fields so a signaling event and a
 * user action on the same call never interleave a half applied phase change. The primary and any
 * secondary (dual) call hold independent locks. The handle never leaks its mutable fields by reference to
 * other units; the controller reads them under the lock and hands out only the immutable {@link Call} and
 * value copies.
 *
 * @implNote This implementation holds only the cross unit wiring the controller threads between the
 * signaling, crypto, transport, and media units: the call key the participant crypto chain consumes, the
 * peer and creator JIDs, and the per call lock. The heavy remainder of a call's context (its timer
 * entries, durations, group info roster, and configuration) lives in the state, timer, and info manager
 * units; a call's context is composed from those sub objects rather than reproduced as one flat structure.
 */
public final class OrchestratedCall {
    /**
     * The public data view of this call the application observes and the controller publishes
     * transitions onto.
     *
     * <p>Replaced once when a call link join adopts the relay assigned call id and creator from the join
     * ack: the {@link Call} identity is immutable, so the controller swaps in a rebuilt view under the
     * resolved identity rather than mutating the id in place.
     */
    private volatile Call call;

    /**
     * Whether the local side placed this call (the transport client) rather than answered it.
     */
    private final boolean caller;

    /**
     * Serializes reads and mutations of this call's orchestration fields; the per call info mutex
     * analogue.
     */
    private final ReentrantLock lock;

    /**
     * The peer device JID that authors the peer's signaling for this call, used as the Signal decryption
     * sender for an inbound key envelope and as the recipient of outbound point to point signaling.
     */
    private volatile Jid peerDeviceJid;

    /**
     * The raw 32 byte end to end call key, or {@code null} until it is minted (caller) or
     * decrypted from the offer (callee).
     */
    private volatile byte[] callKey;

    /**
     * The decoded inbound offer a callee answers from, or {@code null} for an outbound call.
     */
    private volatile OfferStanza incomingOffer;

    /**
     * The {@code <voip_settings>} bundle subtrees the offer acknowledgement delivered to the caller, in
     * wire order; empty until an ack is applied and on the callee side.
     *
     * <p>On the caller side the engine parameter bundles arrive denormalised in the synchronous offer ack,
     * not in the later accept; the controller records them when it applies the ack so the media plane
     * bring up the accept triggers can feed them to the call's voip param manager. The callee side reads its
     * bundles from the inbound offer instead ({@link OfferStanza#voipSettings()}) and leaves this empty.
     */
    private volatile List<Stanza> offerAckVoipSettings = List.of();

    /**
     * The call's {@code <relay>} block subtree once it is learned (from the offer ack on the caller side,
     * the inbound offer or a group update on the callee side), or {@code null} until then.
     */
    private volatile Stanza relay;

    /**
     * The call's peer aware relay election state once the relay block is learned and the latency exchange
     * begins, or {@code null} until then.
     *
     * <p>Built lazily from the offered relay endpoints the first time the call holds its {@code <relay>} block
     * and knows the peer, then fed the peer's {@code <relaylatency>} reports as they arrive; the media plane
     * bring up reads its {@linkplain RelayLatencyState#electBestRelayName(com.github.auties00.cobalt.calls.transport.RelayElection.Mode)
     * election} to bind the relay both ends share rather than the locally fastest one. A call whose relay block
     * or peer is not yet known leaves this {@code null}, and the bring up falls back to its local lowest latency
     * pick.
     */
    private volatile RelayLatencyState relayLatencyState;

    /**
     * The live media plane session once the media plane is brought up, or {@code null} until then.
     */
    private volatile MediaPlane.Session mediaSession;

    /**
     * The live media plane application data controller once the media plane is brought up, or {@code null}
     * until then or when the call's transport carries no app data plane.
     *
     * <p>Recorded from the brought up {@link MediaPlane.Session} so the controller's in call control
     * units (reactions, transcription) can attach themselves to the app data side channel after they are
     * built, which happens later in the call lifecycle than the media plane bring up.
     */
    private volatile AppDataController appDataController;

    /**
     * The peer device JIDs an outbound call fans a terminate out to, in offer order.
     */
    private volatile List<Jid> peerDevices;

    /**
     * The controller's mirror of this call's internal lifecycle state, advanced alongside the
     * state machine unit's authoritative state.
     */
    private volatile CallLifecycleState state;

    /**
     * The membership manager reconciling this call's participant roster against each inbound
     * {@code <group_info>}, for a group call; {@code null} for a one to one call, which has no roster.
     *
     * <p>A group call allocates one {@link CallMembership} for the lifetime of the call and reconciles it
     * from the placement roster and every later {@code <group_update>}; a one to one call has a single fixed
     * peer and never reconciles, so it leaves this {@code null}.
     */
    private volatile CallMembership membership;

    /**
     * The application capture sources and playback sinks the media plane drives for this call, defaulting
     * to {@link MediaStreams#none()} until the placing or accepting call supplies them.
     *
     * <p>An outbound one to one call records the streams when it is placed and the media plane consumes
     * them later, once the peer answers and the relay block is known; an accepted call records them as it
     * answers. The handle never hands the streams to another unit by reference except the media plane it
     * passes them to at bring up.
     */
    private volatile MediaStreams mediaStreams = MediaStreams.none();

    /**
     * The call's in call control units once they are built (when the call becomes answerable), or
     * {@code null} until then.
     *
     * <p>The controller builds the control bundle (mute, video, screen share, raise hand) lazily the first
     * time the call is answerable and closes it on teardown; a one to one or group call that never reaches an
     * answerable state, or one whose host event bus is unbound, leaves this {@code null}.
     */
    private volatile LifecycleController.CallControls controls;

    /**
     * The call's outbound group call unit once it is built, or {@code null} for a one to one call.
     *
     * <p>The controller builds one {@link GroupCallOutbound} when a group call is placed (or joined),
     * binding it to the call's {@link CallMembership}; it owns the per peer offer send timestamps, the
     * unanswered offer sweep the watchdog drives, the per peer terminate fanout, and the
     * {@code <group_update>} membership update build. The handle holds it for the call's lifetime and it is
     * dropped with the handle on teardown; a one to one call leaves this {@code null}.
     */
    private volatile GroupCallOutbound groupOutbound;

    /**
     * The engine call context the manager allocated for this call, or {@code null} when the registry
     * allocated none (a test registry that builds no context).
     *
     * <p>This is the heavyweight {@link CallContext} the manager and state units own, surfaced here
     * by reference so the lifecycle controller can hand it to the outbound call log sink at teardown after
     * the call's durations have been closed out. The orchestration handle reads it under the call's lock
     * and hands it out only to the call log sink; it never mutates the context itself, which the state
     * machine and timer callbacks own.
     */
    private volatile CallContext engineContext;

    /**
     * The per call inbound deduplication state the message router reads and the controller advances, never
     * {@code null}; starts at {@link IncomingMessageRouter.DedupState#INITIAL}.
     *
     * <p>The controller threads this into {@link IncomingMessageRouter#route(com.github.auties00.cobalt.calls.signaling.CallMessage,
     * Jid, IncomingMessageRouter.DedupState, java.util.function.Function)} on every inbound signal for
     * the call and swaps the reference for the advanced state the router's
     * {@link IncomingMessageRouter.DedupState#withTransactionId(int)} or
     * {@link IncomingMessageRouter.DedupState#markRejected()} returns. The record is immutable, so the
     * handle publishes a new reference rather than mutating in place.
     */
    private volatile IncomingMessageRouter.DedupState dedupState =
            IncomingMessageRouter.DedupState.INITIAL;

    /**
     * Constructs an orchestration handle for one call.
     *
     * @param call          the public data view of the call
     * @param caller        whether the local side placed the call
     * @param peerDeviceJid the peer device JID authoring the peer's signaling, or {@code null} when not
     *                      yet known
     * @param initialState  the call's initial internal lifecycle state
     * @throws NullPointerException if {@code call} or {@code initialState} is {@code null}
     */
    OrchestratedCall(Call call, boolean caller, Jid peerDeviceJid, CallLifecycleState initialState) {
        this.call = Objects.requireNonNull(call, "call cannot be null");
        this.caller = caller;
        this.peerDeviceJid = peerDeviceJid;
        this.state = Objects.requireNonNull(initialState, "initialState cannot be null");
        this.lock = new ReentrantLock();
        this.peerDevices = List.of();
    }

    /**
     * Returns the public data view of this call.
     *
     * @return the call view, never {@code null}
     */
    Call call() {
        return call;
    }

    /**
     * Replaces the public data view of this call.
     *
     * <p>Used only when a call link join adopts the relay assigned identity from the join ack: the new view
     * carries the resolved call id and creator while preserving the live phase and mute flags. The
     * {@link Call} identity is immutable, so adoption swaps the whole view rather than mutating its id.
     *
     * @param call the rebuilt public data view under the resolved identity
     * @throws NullPointerException if {@code call} is {@code null}
     */
    void call(Call call) {
        this.call = Objects.requireNonNull(call, "call cannot be null");
    }

    /**
     * Returns this call's identifier.
     *
     * @return the call id, never {@code null}
     */
    String callId() {
        return call.callId();
    }

    /**
     * Returns whether the local side placed this call.
     *
     * @return {@code true} when the local side is the caller
     */
    boolean isCaller() {
        return caller;
    }

    /**
     * Returns this call's per call lock, the info mutex analogue.
     *
     * @return the lock, never {@code null}
     */
    public ReentrantLock lock() {
        return lock;
    }

    /**
     * Returns the peer device JID authoring the peer's signaling, if known.
     *
     * @return an {@link Optional} holding the peer device JID, or empty when not yet known
     */
    Optional<Jid> peerDeviceJid() {
        return Optional.ofNullable(peerDeviceJid);
    }

    /**
     * Records the peer device JID authoring the peer's signaling.
     *
     * @param peerDeviceJid the peer device JID
     */
    void peerDeviceJid(Jid peerDeviceJid) {
        this.peerDeviceJid = peerDeviceJid;
    }

    /**
     * Returns the raw call key, if it has been minted or recovered.
     *
     * <p>The returned array, when present, is a defensive copy so no caller can mutate the stored key.
     *
     * @return an {@link Optional} holding a copy of the call key, or empty until the key is set
     */
    Optional<byte[]> callKey() {
        return Optional.ofNullable(callKey).map(byte[]::clone);
    }

    /**
     * Records the raw call key, storing a defensive copy.
     *
     * @param callKey the raw call key
     * @throws NullPointerException if {@code callKey} is {@code null}
     */
    void callKey(byte[] callKey) {
        this.callKey = Objects.requireNonNull(callKey, "callKey cannot be null").clone();
    }

    /**
     * Returns the decoded inbound offer, if this is an answered call.
     *
     * @return an {@link Optional} holding the inbound offer, or empty for an outbound call
     */
    Optional<OfferStanza> incomingOffer() {
        return Optional.ofNullable(incomingOffer);
    }

    /**
     * Records the decoded inbound offer a callee answers from.
     *
     * @param incomingOffer the inbound offer
     */
    void incomingOffer(OfferStanza incomingOffer) {
        this.incomingOffer = incomingOffer;
    }

    /**
     * Returns the {@code <voip_settings>} bundle subtrees the offer acknowledgement delivered to the caller.
     *
     * @return an unmodifiable list of the offer ack engine parameter bundles, in wire order; empty when no
     *         ack carried any and on the callee side
     */
    List<Stanza> offerAckVoipSettings() {
        return offerAckVoipSettings;
    }

    /**
     * Records the {@code <voip_settings>} bundle subtrees the offer acknowledgement delivered to the caller.
     *
     * @param offerAckVoipSettings the offer ack engine parameter bundles, in wire order
     * @throws NullPointerException if {@code offerAckVoipSettings} is {@code null} or contains a
     *                              {@code null} element
     */
    void offerAckVoipSettings(List<Stanza> offerAckVoipSettings) {
        this.offerAckVoipSettings = List.copyOf(offerAckVoipSettings);
    }

    /**
     * Returns the call's relay block subtree, if it has been learned.
     *
     * @return an {@link Optional} holding the relay stanza, or empty until it is learned
     */
    Optional<Stanza> relay() {
        return Optional.ofNullable(relay);
    }

    /**
     * Records the call's relay block subtree.
     *
     * @param relay the {@code <relay>} stanza
     */
    void relay(Stanza relay) {
        this.relay = relay;
    }

    /**
     * Returns the call's peer aware relay election state, if the latency exchange has begun.
     *
     * @return an {@link Optional} holding the relay latency state, or empty until it is built
     */
    Optional<RelayLatencyState> relayLatencyState() {
        return Optional.ofNullable(relayLatencyState);
    }

    /**
     * Records the call's peer aware relay election state, built once when the relay block and peer are first
     * known.
     *
     * @param relayLatencyState the relay latency state seeded from the offered relay endpoints
     * @throws NullPointerException if {@code relayLatencyState} is {@code null}
     */
    void relayLatencyState(RelayLatencyState relayLatencyState) {
        this.relayLatencyState = Objects.requireNonNull(relayLatencyState, "relayLatencyState cannot be null");
    }

    /**
     * Returns the live media plane session, if the media plane is up.
     *
     * @return an {@link Optional} holding the media session, or empty until the media plane is brought up
     */
    public Optional<MediaPlane.Session> mediaSession() {
        return Optional.ofNullable(mediaSession);
    }

    /**
     * Records the live media plane session.
     *
     * @param mediaSession the media session
     */
    public void mediaSession(MediaPlane.Session mediaSession) {
        this.mediaSession = mediaSession;
    }

    /**
     * Returns the live media plane application data controller, if the media plane is up and carries one.
     *
     * @return an {@link Optional} holding the app data controller, or empty until the media plane is brought
     * up or when the call's transport carries no app data plane
     */
    Optional<AppDataController> appDataController() {
        return Optional.ofNullable(appDataController);
    }

    /**
     * Records the live media plane application data controller.
     *
     * <p>Set during the media plane bring up from the brought up {@link MediaPlane.Session} so the
     * call's in call control units can attach to its inbound observer seams once they are built. A
     * {@code null} clears it, for a call whose transport carries no app data plane.
     *
     * @param appDataController the app data controller, or {@code null} when the session has none
     */
    void appDataController(AppDataController appDataController) {
        this.appDataController = appDataController;
    }

    /**
     * Returns the peer device JIDs an outbound call fans a terminate out to.
     *
     * @return an unmodifiable list of peer device JIDs, possibly empty
     */
    List<Jid> peerDevices() {
        return peerDevices;
    }

    /**
     * Records the peer device JIDs an outbound call fans a terminate out to.
     *
     * @param peerDevices the peer device JIDs
     * @throws NullPointerException if {@code peerDevices} is {@code null} or contains a {@code null}
     *                              element
     */
    void peerDevices(List<Jid> peerDevices) {
        this.peerDevices = List.copyOf(peerDevices);
    }

    /**
     * Returns the controller's mirror of this call's internal lifecycle state.
     *
     * @return the internal state, never {@code null}
     */
    CallLifecycleState state() {
        return state;
    }

    /**
     * Records the controller's mirror of this call's internal lifecycle state.
     *
     * @param state the internal state
     * @throws NullPointerException if {@code state} is {@code null}
     */
    void state(CallLifecycleState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * Returns this call's membership manager, if it is a group call with a roster.
     *
     * @return an {@link Optional} holding the membership manager, or empty for a one to one call
     */
    Optional<CallMembership> membership() {
        return Optional.ofNullable(membership);
    }

    /**
     * Records this call's membership manager, allocated once when a group call is placed or joined.
     *
     * @param membership the membership manager reconciling this call's participant roster
     * @throws NullPointerException if {@code membership} is {@code null}
     */
    void membership(CallMembership membership) {
        this.membership = Objects.requireNonNull(membership, "membership cannot be null");
    }

    /**
     * Returns the application capture sources and playback sinks the media plane drives for this call.
     *
     * @return the media streams, never {@code null}; {@link MediaStreams#none()} until they are set
     */
    MediaStreams mediaStreams() {
        return mediaStreams;
    }

    /**
     * Records the application capture sources and playback sinks the media plane drives for this call.
     *
     * @param mediaStreams the media streams the placing or accepting call supplied
     * @throws NullPointerException if {@code mediaStreams} is {@code null}
     */
    void mediaStreams(MediaStreams mediaStreams) {
        this.mediaStreams = Objects.requireNonNull(mediaStreams, "mediaStreams cannot be null");
    }

    /**
     * Returns the call's in call control units, if they have been built.
     *
     * @return an {@link Optional} holding the control bundle, or empty until the call becomes answerable
     */
    public Optional<LifecycleController.CallControls> controls() {
        return Optional.ofNullable(controls);
    }

    /**
     * Records the call's in call control units, built once when the call becomes answerable.
     *
     * @param controls the control bundle the controller built for this call
     * @throws NullPointerException if {@code controls} is {@code null}
     */
    public void controls(LifecycleController.CallControls controls) {
        this.controls = Objects.requireNonNull(controls, "controls cannot be null");
    }

    /**
     * Returns the call's outbound group call unit, if it is a group call.
     *
     * @return an {@link Optional} holding the outbound group call unit, or empty for a one to one call
     */
    Optional<GroupCallOutbound> groupOutbound() {
        return Optional.ofNullable(groupOutbound);
    }

    /**
     * Records the call's outbound group call unit, built once when a group call is placed or joined.
     *
     * @param groupOutbound the outbound group call unit the controller built for this call
     * @throws NullPointerException if {@code groupOutbound} is {@code null}
     */
    void groupOutbound(GroupCallOutbound groupOutbound) {
        this.groupOutbound = Objects.requireNonNull(groupOutbound, "groupOutbound cannot be null");
    }

    /**
     * Returns the engine call context the manager allocated for this call, if one was allocated.
     *
     * @return an {@link Optional} holding the engine call context, or empty when the registry allocated none
     */
    Optional<CallContext> engineContext() {
        return Optional.ofNullable(engineContext);
    }

    /**
     * Records the engine call context the manager allocated for this call.
     *
     * <p>Set once when the call's context is allocated through the
     * {@link CallContextRegistry registry}, so the lifecycle controller can hand the finished context
     * to the outbound call log sink at teardown. A {@code null} leaves the handle with no context, for a
     * test registry that builds none.
     *
     * @param engineContext the engine call context, or {@code null} when none was allocated
     */
    void engineContext(CallContext engineContext) {
        this.engineContext = engineContext;
    }

    /**
     * Returns the per call inbound deduplication state the message router reads.
     *
     * @return the deduplication state, never {@code null}
     */
    IncomingMessageRouter.DedupState dedupState() {
        return dedupState;
    }

    /**
     * Records the per call inbound deduplication state the message router advanced.
     *
     * @param dedupState the advanced deduplication state
     * @throws NullPointerException if {@code dedupState} is {@code null}
     */
    void dedupState(IncomingMessageRouter.DedupState dedupState) {
        this.dedupState = Objects.requireNonNull(dedupState, "dedupState cannot be null");
    }
}
