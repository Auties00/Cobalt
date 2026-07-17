package com.github.auties00.cobalt.calls.engine.context;

import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.lang.System.Logger.Level;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.engine.state.CallLinkState;
import com.github.auties00.cobalt.calls.telemetry.CallResult;

/**
 * The per call mutable aggregate the call engine keeps for one call in progress.
 *
 * <p>This is the single mutable object that holds everything about one call's lifecycle. It binds the
 * call's immutable identity (the {@linkplain #callId() call id}, the {@linkplain #role() manager role},
 * the {@linkplain #direction() direction}, the {@linkplain #peer() peer}, {@linkplain #creator() creator},
 * {@linkplain #self() self}, and {@linkplain #chatJid() chat} JIDs, and the {@linkplain #group() group},
 * {@linkplain #video() video}, and {@linkplain #outgoing() outgoing} topology) to its mutable runtime
 * fields (the internal {@linkplain #state() state}, the {@linkplain #linkState() link sub state}, the
 * {@linkplain #result() result}, the {@linkplain #acceptReceived() accept received} and
 * {@linkplain #mediaStarted() media started} flags, and the {@linkplain #activeDurationMillis() active}
 * and {@linkplain #lonelyDurationMillis() lonely} duration accumulators), its
 * {@linkplain #membership() membership} set, and the {@linkplain #connectedLonelyConfig() connected lonely
 * timeout configuration} the state machine reads when entering the lonely state.
 *
 * <p>Every mutation and every read of the runtime fields is serialized behind the context's own
 * {@linkplain #lock() lock}: the state machine, the timer callbacks, and the lifecycle controller all
 * take it for the duration of an operation, so a state transition and a duration accounting update never
 * interleave with a concurrent snapshot. The {@link CallManager} pairs each of its two slots with one
 * such lock; this class owns the lock so the same instance can be moved between manager slots (a dual
 * call promotion) without rebinding it.
 *
 * <p>The context projects its public view onto the reused {@link Call} model: the same {@link Call}
 * instance the application observes is updated whenever this context's state, mute flags, or end reason
 * change, so a listener reading {@link Call#state()} sees the projection of {@link #state()} through
 * {@link CallLifecycleState#toPublic()}. The heavyweight media and transport objects, and the per call
 * timers, are not modeled as typed fields here because they are owned by sibling subsystems; the context
 * holds them only as opaque {@linkplain #attachResource(AutoCloseable) resource} slots it closes on
 * teardown, so this class stays decoupled from those subsystems while still owning their lifecycle.
 *
 * <p>This class is not thread safe by itself; callers serialize through {@link #lock()}. It is an
 * internal engine collaborator and is never exposed to embedders, who observe a call only through the
 * projected {@link Call}.
 *
 * @implNote This implementation holds each element of the per call state as a discrete typed field
 * rather than as one flat memory blob. The timer handles, the media and transport handles, and the
 * participant slots are owned by Cobalt's timer, media, transport, and {@link CallMembership} subsystems
 * respectively; this aggregate holds only the lifecycle owning state plus the opaque resource and timer
 * seams those subsystems plug into.
 */
public final class CallContext {
    /**
     * The logger for {@link CallContext}.
     */
    private static final System.Logger LOGGER = Log.get(CallContext.class);

    /**
     * The shared call id random source.
     *
     * <p>A single {@link SecureRandom} draws the sixteen call id bytes for every context.
     */
    private static final SecureRandom CALL_ID_RANDOM = new SecureRandom();

    /**
     * The number of random bytes drawn for a call id.
     *
     * <p>Sixteen bytes are drawn and each is hex encoded into two characters.
     */
    public static final int CALL_ID_BYTE_LENGTH = 16;

    /**
     * The character length of a hex encoded call id.
     *
     * <p>Each of the {@value #CALL_ID_BYTE_LENGTH} bytes encodes to two characters, giving an identifier
     * of 32 characters.
     */
    public static final int CALL_ID_CHAR_LENGTH = CALL_ID_BYTE_LENGTH * 2;

    /**
     * The dual case hex alphabet used to encode a call id.
     *
     * <p>The high nibble of each byte indexes the first sixteen (upper case) characters and the low
     * nibble indexes the trailing sixteen (lower case) characters, so a single byte's two hex digits
     * differ in case and the case pattern varies across the identifier.
     */
    private static final char[] CALL_ID_ALPHABET = "0123456789ABCDEF0123456789abcdef".toCharArray();

    /**
     * The default short connected lonely interval, in milliseconds.
     *
     * <p>This is the default for the first connected lonely interval; the state machine picks it for the
     * caller direction when entering the connected lonely state.
     */
    public static final long CONNECTED_LONELY_DEFAULT_SHORT_MILLIS = 30_000L;

    /**
     * The default long connected lonely interval, in milliseconds.
     *
     * <p>This is the default for the long connected lonely interval, picked for the callee direction when
     * entering the connected lonely state.
     */
    public static final long CONNECTED_LONELY_DEFAULT_LONG_MILLIS = 270_000L;

    /**
     * The default maximum connected lonely interval, in milliseconds.
     *
     * <p>This is the default ceiling for the connected lonely timer, the last interval the timer steps
     * through before the call ends on a lonely timeout.
     */
    public static final long CONNECTED_LONELY_DEFAULT_MAX_MILLIS = 300_000L;

    /**
     * The fixed call identifier, sixteen random bytes hex encoded into 32 characters.
     */
    private final String callId;

    /**
     * The manager role this context occupies (primary or secondary).
     */
    private final CallRole role;

    /**
     * The direction of this call (outbound or inbound).
     */
    private final CallDirection direction;

    /**
     * The peer device JID.
     */
    private final Jid peer;

    /**
     * The creator device JID, the user who started the call with its device suffix.
     */
    private final Jid creator;

    /**
     * The local self JID for this call.
     */
    private final Jid self;

    /**
     * The chat the call belongs to: the peer JID for a one to one call or the group JID for a group call.
     */
    private final Jid chatJid;

    /**
     * Whether this is a group call.
     */
    private final boolean group;

    /**
     * Whether this call was placed or accepted with video enabled.
     */
    private final boolean video;

    /**
     * Whether the local user placed this call.
     */
    private final boolean outgoing;

    /**
     * The per call lock serializing all runtime field reads and mutations.
     */
    private final ReentrantLock lock;

    /**
     * The membership set of this call.
     */
    private final CallMembership membership;

    /**
     * The connected lonely timeout configuration the state machine reads on entering the lonely state.
     */
    private final ConnectedLonelyConfig connectedLonelyConfig;

    /**
     * The public projection of this call, the {@link Call} the application observes.
     */
    private final Call call;

    /**
     * The opaque per call resources closed on teardown, held in attachment order.
     */
    private final List<AutoCloseable> resources;

    /**
     * The current internal call state.
     */
    private CallLifecycleState state;

    /**
     * The current link join sub state, meaningful only while {@link #state} is {@link CallLifecycleState#LINK}.
     */
    private CallLinkState linkState;

    /**
     * The current call result, or {@code null} until a result is recorded.
     */
    private CallResult result;

    /**
     * Whether an accept has been received for this call.
     */
    private boolean acceptReceived;

    /**
     * Whether media and transport have been brought up, guarding against a duplicate accept.
     */
    private boolean mediaStarted;

    /**
     * Whether a companion device of the local account has terminated this call, so a later companion
     * terminate is ignored.
     */
    private boolean companionTerminated;

    /**
     * The wall clock instant the call was placed (outgoing) or accepted (incoming), stamped once when the
     * call reaches that lifecycle point, or {@code null} until then.
     *
     * <p>This is the call's start instant for the call history record, distinct from the per segment
     * {@link #activeSegmentStartMillis} accumulator timestamps: it marks the offer/placement (or accept)
     * moment the {@code CallLog} records as its start time, whereas the segment timestamps only measure
     * elapsed active and lonely durations.
     */
    private Instant startedAt;

    /**
     * The accumulated time, in milliseconds, the call has spent in the active state across segments.
     */
    private long activeDurationMillis;

    /**
     * The accumulated time, in milliseconds, the call has spent in the connected lonely state across
     * segments.
     */
    private long lonelyDurationMillis;

    /**
     * The wall clock millisecond timestamp at which the current active segment began, or {@code -1} when
     * the call is not active.
     */
    private long activeSegmentStartMillis;

    /**
     * The wall clock millisecond timestamp at which the current connected lonely segment began, or
     * {@code -1} when the call is not in the connected lonely state.
     */
    private long lonelySegmentStartMillis;

    /**
     * Constructs a call context with a freshly generated call id and the given identity and topology.
     *
     * <p>The context starts in {@link CallLifecycleState#NONE} with {@link CallLinkState#NONE}, no
     * recorded result, no accept received, no media started, zeroed duration accumulators, the default
     * {@linkplain ConnectedLonelyConfig#defaults() connected lonely configuration}, and a fresh
     * {@link CallMembership}. The projected {@link Call} is created in the public phase the initial state
     * projects to. The connected lonely timer is armed and cancelled by the lifecycle controller around
     * its state transitions, not through this context.
     *
     * @param role     the manager role this context occupies
     * @param direction the direction of the call
     * @param peer     the peer device JID
     * @param creator  the creator device JID
     * @param self     the local self JID
     * @param chatJid  the chat the call belongs to
     * @param group    whether this is a group call
     * @param video    whether the call carries video
     * @throws NullPointerException if {@code role}, {@code direction}, {@code peer}, {@code creator},
     *                              {@code self}, or {@code chatJid} is {@code null}
     */
    public CallContext(CallRole role, CallDirection direction, Jid peer, Jid creator,
                             Jid self, Jid chatJid, boolean group, boolean video) {
        this(generateCallId(), role, direction, peer, creator, self, chatJid, group, video);
    }

    /**
     * Constructs a call context bound to a known call id and the given identity and topology.
     *
     * <p>This overload pins the call id rather than generating one, for an inbound call whose id the peer
     * assigned in its offer; the runtime fields start in the same initial state as the generating
     * constructor above.
     *
     * @param callId    the fixed call identifier
     * @param role      the manager role this context occupies
     * @param direction the direction of the call
     * @param peer      the peer device JID
     * @param creator   the creator device JID
     * @param self      the local self JID
     * @param chatJid   the chat the call belongs to
     * @param group     whether this is a group call
     * @param video     whether the call carries video
     * @throws NullPointerException if any reference argument is {@code null}
     */
    public CallContext(String callId, CallRole role, CallDirection direction, Jid peer,
                             Jid creator, Jid self, Jid chatJid, boolean group, boolean video) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.direction = Objects.requireNonNull(direction, "direction cannot be null");
        this.peer = Objects.requireNonNull(peer, "peer cannot be null");
        this.creator = Objects.requireNonNull(creator, "creator cannot be null");
        this.self = Objects.requireNonNull(self, "self cannot be null");
        this.chatJid = Objects.requireNonNull(chatJid, "chatJid cannot be null");
        this.group = group;
        this.video = video;
        this.outgoing = direction == CallDirection.OUTGOING;
        this.lock = new ReentrantLock();
        this.membership = new CallMembership(callId);
        this.connectedLonelyConfig = ConnectedLonelyConfig.defaults();
        this.state = CallLifecycleState.NONE;
        this.linkState = CallLinkState.NONE;
        this.result = null;
        this.acceptReceived = false;
        this.mediaStarted = false;
        this.companionTerminated = false;
        this.startedAt = null;
        this.activeDurationMillis = 0L;
        this.lonelyDurationMillis = 0L;
        this.activeSegmentStartMillis = -1L;
        this.lonelySegmentStartMillis = -1L;
        this.resources = new ArrayList<>();
        this.call = new Call(callId, peer.toUserJid(), chatJid, creator, outgoing, group, video,
                state.toPublic());
    }

    /**
     * Generates a fresh call id: sixteen random bytes hex encoded into 32 dual case characters.
     *
     * <p>Each byte's high nibble selects an upper case hex character and its low nibble selects a lower
     * case hex character from the {@link #CALL_ID_ALPHABET}, so the case alternates within each byte and
     * the case pattern varies across the identifier.
     *
     * @return a freshly generated call id of {@value #CALL_ID_CHAR_LENGTH} characters
     * @implNote This implementation encodes each of the sixteen bytes with the dual case alphabet
     * {@code 0123456789ABCDEF0123456789abcdef}, indexing it by the high nibble and then the low nibble so
     * the two hex digits of one byte differ in case. The sixteen bytes are drawn from a
     * {@link SecureRandom}.
     */
    public static String generateCallId() {
        var bytes = new byte[CALL_ID_BYTE_LENGTH];
        CALL_ID_RANDOM.nextBytes(bytes);
        var chars = new char[CALL_ID_CHAR_LENGTH];
        for (var i = 0; i < CALL_ID_BYTE_LENGTH; i++) {
            var value = bytes[i] & 0xff;
            chars[i * 2] = CALL_ID_ALPHABET[value >>> 4];
            chars[i * 2 + 1] = CALL_ID_ALPHABET[16 + (value & 0x0f)];
        }
        return new String(chars);
    }

    /**
     * Returns the fixed call identifier.
     *
     * @return the call id, never {@code null}
     */
    public String callId() {
        return callId;
    }

    /**
     * Returns the manager role this context occupies.
     *
     * @return the role, never {@code null}
     */
    public CallRole role() {
        return role;
    }

    /**
     * Returns the direction of this call.
     *
     * @return the direction, never {@code null}
     */
    public CallDirection direction() {
        return direction;
    }

    /**
     * Returns the peer device JID.
     *
     * @return the peer JID, never {@code null}
     */
    public Jid peer() {
        return peer;
    }

    /**
     * Returns the creator device JID.
     *
     * @return the creator JID, never {@code null}
     */
    public Jid creator() {
        return creator;
    }

    /**
     * Returns the local self JID for this call.
     *
     * @return the self JID, never {@code null}
     */
    public Jid self() {
        return self;
    }

    /**
     * Returns the chat the call belongs to.
     *
     * @return the chat JID, never {@code null}
     */
    public Jid chatJid() {
        return chatJid;
    }

    /**
     * Returns whether this is a group call.
     *
     * @return {@code true} for a group call
     */
    public boolean group() {
        return group;
    }

    /**
     * Returns whether this call carries video.
     *
     * @return {@code true} for a video call
     */
    public boolean video() {
        return video;
    }

    /**
     * Returns whether the local user placed this call.
     *
     * @return {@code true} for an outbound call
     */
    public boolean outgoing() {
        return outgoing;
    }

    /**
     * Returns this context's per call lock.
     *
     * <p>Callers take this lock for the duration of any operation that reads or mutates the runtime
     * fields, so a state transition, a duration update, and a snapshot never interleave. The lock is
     * reentrant, so a caller already holding it may enter the context again.
     *
     * @return the per call lock, never {@code null}
     */
    public ReentrantLock lock() {
        return lock;
    }

    /**
     * Returns the membership set of this call.
     *
     * @return the membership manager, never {@code null}
     */
    public CallMembership membership() {
        return membership;
    }

    /**
     * Returns the connected lonely timeout configuration.
     *
     * @return the connected lonely configuration, never {@code null}
     */
    public ConnectedLonelyConfig connectedLonelyConfig() {
        return connectedLonelyConfig;
    }

    /**
     * Returns the public projection of this call.
     *
     * <p>The same {@link Call} instance is updated as this context's state, mute flags, and end reason
     * change, so the application observes the projection through it; it is the only call object the engine
     * exposes outside the membership and lifecycle layers.
     *
     * @return the projected call, never {@code null}
     */
    public Call call() {
        return call;
    }

    /**
     * Returns the current internal call state.
     *
     * @return the current state, never {@code null}
     */
    public CallLifecycleState state() {
        return state;
    }

    /**
     * Sets the internal call state and republishes the projected public phase.
     *
     * <p>This setter writes the raw state field and projects it onto the {@link Call} model through
     * {@link CallLifecycleState#toPublic()}; it performs no transition guarding or duration accounting, which
     * are the {@link CallStateMachine}'s responsibility. Callers other than the state machine should
     * drive transitions through {@link CallStateMachine#transition(CallContext, CallLifecycleState)}
     * rather than this setter. The caller holds {@link #lock()}.
     *
     * @param state the new internal state
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public void state(CallLifecycleState state) {
        Objects.requireNonNull(state, "state cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state {1} -> {2}", callId, this.state, state);
        this.state = state;
        call.setState(state.toPublic());
    }

    /**
     * Returns the current link join sub state.
     *
     * @return the link sub state, never {@code null}
     */
    public CallLinkState linkState() {
        return linkState;
    }

    /**
     * Sets the link join sub state.
     *
     * <p>The sub state is meaningful only while {@link #state()} is {@link CallLifecycleState#LINK}; outside
     * that phase it stays {@link CallLinkState#NONE}. The caller holds {@link #lock()}.
     *
     * @param linkState the new link sub state
     * @throws NullPointerException if {@code linkState} is {@code null}
     */
    public void linkState(CallLinkState linkState) {
        this.linkState = Objects.requireNonNull(linkState, "linkState cannot be null");
    }

    /**
     * Returns the current call result, if one has been recorded.
     *
     * <p>The result records why a call ended (or that it succeeded) and is tracked separately from the
     * call state; it is absent until a result is recorded.
     *
     * @return an {@link Optional} holding the current result, or empty when none has been recorded
     */
    public Optional<CallResult> result() {
        return Optional.ofNullable(result);
    }

    /**
     * Records the call result and republishes the projected end reason.
     *
     * <p>This writes the result field, distinct from the state field, and projects it onto the
     * {@link Call} model through {@link CallResult#toEndReason()}. The caller holds {@link #lock()}.
     *
     * @param result the result to record
     * @throws NullPointerException if {@code result} is {@code null}
     */
    public void result(CallResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} result {1}", callId, result);
        this.result = result;
        call.setEndReason(result.toEndReason());
    }

    /**
     * Stamps the call's start instant the first time the call is placed or accepted.
     *
     * <p>Records {@link Instant#now()} as {@link #startedAt()} on the first invocation and is a no op on
     * every later one, so the earliest placed/accepted moment is kept. The lifecycle controller calls this
     * on the transition into the placing ({@link CallLifecycleState#CALLING}) or accepted
     * ({@link CallLifecycleState#ACCEPT_SENT}) state, under the call lock, so the call history record can
     * stamp the real start time rather than the teardown time.
     */
    public void markStarted() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    /**
     * Returns the instant the call was placed or accepted, if it has reached that point.
     *
     * @return an {@link Optional} holding the start instant, or empty when the call was never placed or
     *         accepted
     */
    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    /**
     * Returns whether an accept has been received for this call.
     *
     * @return {@code true} once an accept has been received
     */
    public boolean acceptReceived() {
        return acceptReceived;
    }

    /**
     * Sets whether an accept has been received for this call.
     *
     * @param acceptReceived whether an accept has been received
     */
    public void acceptReceived(boolean acceptReceived) {
        this.acceptReceived = acceptReceived;
    }

    /**
     * Returns whether media and transport have been brought up for this call.
     *
     * @return {@code true} once media and transport are up
     */
    public boolean mediaStarted() {
        return mediaStarted;
    }

    /**
     * Sets whether media and transport have been brought up for this call.
     *
     * <p>The engine sets this flag once accept handling brings up media and transport, and guards on it
     * so a duplicate accept does not restart the media plane.
     *
     * @param mediaStarted whether media and transport are up
     */
    public void mediaStarted(boolean mediaStarted) {
        this.mediaStarted = mediaStarted;
    }

    /**
     * Returns whether a companion device of the local account has terminated this call.
     *
     * @return {@code true} once a companion device has terminated the call
     */
    public boolean companionTerminated() {
        return companionTerminated;
    }

    /**
     * Sets whether a companion device of the local account has terminated this call.
     *
     * <p>The engine reads this flag in terminate handling to ignore a later companion device terminate
     * once the call is already being torn down by one.
     *
     * @param companionTerminated whether a companion device has terminated the call
     */
    public void companionTerminated(boolean companionTerminated) {
        this.companionTerminated = companionTerminated;
    }

    /**
     * Returns the accumulated active duration, in milliseconds, across all active segments.
     *
     * <p>This does not include the open active segment, if any; it is the sum of completed active
     * segments closed by {@link #closeActiveSegment(long)}.
     *
     * @return the accumulated active duration in milliseconds, never negative
     */
    public long activeDurationMillis() {
        return activeDurationMillis;
    }

    /**
     * Returns the accumulated connected lonely duration, in milliseconds, across all lonely segments.
     *
     * <p>This does not include the open lonely segment, if any; it is the sum of completed lonely
     * segments closed by {@link #closeLonelySegment(long)}.
     *
     * @return the accumulated lonely duration in milliseconds, never negative
     */
    public long lonelyDurationMillis() {
        return lonelyDurationMillis;
    }

    /**
     * Opens an active duration segment at the given timestamp.
     *
     * <p>Called by the state machine on entering {@link CallLifecycleState#CALL_ACTIVE} to capture the
     * segment start; the segment is later closed by {@link #closeActiveSegment(long)}, which adds the
     * elapsed time to {@link #activeDurationMillis()}. The caller holds {@link #lock()}.
     *
     * @param nowMillis the wall clock millisecond timestamp at which the segment begins
     */
    public void openActiveSegment(long nowMillis) {
        this.activeSegmentStartMillis = nowMillis;
    }

    /**
     * Closes the open active duration segment at the given timestamp, accumulating its elapsed time.
     *
     * <p>Called by the state machine on leaving {@link CallLifecycleState#CALL_ACTIVE}: the elapsed time
     * since the segment was opened is added to {@link #activeDurationMillis()} and the segment is cleared.
     * A close with no open segment, or with a timestamp before the open timestamp, accumulates nothing.
     * The caller holds {@link #lock()}.
     *
     * @param nowMillis the wall clock millisecond timestamp at which the segment ends
     * @return the elapsed milliseconds added to the accumulator, never negative
     */
    public long closeActiveSegment(long nowMillis) {
        if (activeSegmentStartMillis < 0) {
            return 0L;
        }
        var elapsed = Math.max(0L, nowMillis - activeSegmentStartMillis);
        activeDurationMillis += elapsed;
        activeSegmentStartMillis = -1L;
        return elapsed;
    }

    /**
     * Opens a connected lonely duration segment at the given timestamp.
     *
     * <p>Called by the state machine on entering {@link CallLifecycleState#CONNECTED_LONELY} to capture the
     * segment start; the segment is later closed by {@link #closeLonelySegment(long)}. The caller holds
     * {@link #lock()}.
     *
     * @param nowMillis the wall clock millisecond timestamp at which the segment begins
     */
    public void openLonelySegment(long nowMillis) {
        this.lonelySegmentStartMillis = nowMillis;
    }

    /**
     * Closes the open connected lonely duration segment at the given timestamp, accumulating its elapsed
     * time.
     *
     * <p>Called by the state machine on leaving {@link CallLifecycleState#CONNECTED_LONELY}: the elapsed
     * time since the segment was opened is added to {@link #lonelyDurationMillis()} and the segment is
     * cleared. A close with no open segment, or with a timestamp before the open timestamp, accumulates
     * nothing. The caller holds {@link #lock()}.
     *
     * @param nowMillis the wall clock millisecond timestamp at which the segment ends
     * @return the elapsed milliseconds added to the accumulator, never negative
     */
    public long closeLonelySegment(long nowMillis) {
        if (lonelySegmentStartMillis < 0) {
            return 0L;
        }
        var elapsed = Math.max(0L, nowMillis - lonelySegmentStartMillis);
        lonelyDurationMillis += elapsed;
        lonelySegmentStartMillis = -1L;
        return elapsed;
    }

    /**
     * Attaches a per call resource to be closed when this context tears down.
     *
     * <p>The context closes attached resources in reverse attachment order during {@link #close()},
     * giving the lifecycle controller one place to register the media engine, the transport, the data
     * channel, and any other {@link AutoCloseable} the call owns without this aggregate depending on
     * their types. The caller holds {@link #lock()}.
     *
     * @param resource the resource to close on teardown
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    public void attachResource(AutoCloseable resource) {
        Objects.requireNonNull(resource, "resource cannot be null");
        resources.add(resource);
    }

    /**
     * Closes all attached resources and clears the lonely timer seams.
     *
     * <p>Resources are closed in reverse attachment order so a resource that depends on an earlier one is
     * torn down first; a resource that throws on close is logged and the teardown continues with the
     * remaining resources. After this returns the resource list is empty and the lonely timer seams are
     * cleared. The caller holds {@link #lock()}.
     *
     * @implNote This implementation closes only the opaque resource slots the lifecycle controller
     * attached; the call's timers are stopped by the timer subsystem, not here.
     */
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing call {0} context, {1} resource(s)", callId, resources.size());
        for (var index = resources.size() - 1; index >= 0; index--) {
            var resource = resources.get(index);
            try {
                resource.close();
            } catch (Exception exception) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to close call resource for call " + callId, exception);
            }
        }
        resources.clear();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallContext that && this.callId.equals(that.callId));
    }

    @Override
    public int hashCode() {
        return callId.hashCode();
    }

    @Override
    public String toString() {
        return "CallContext[callId=" + callId
                + ", role=" + role
                + ", direction=" + direction
                + ", state=" + state
                + ", group=" + group
                + ']';
    }

    /**
     * Enumerates the manager slot a call context occupies.
     *
     * <p>The {@link CallManager} holds at most a {@link #PRIMARY} call and one optional
     * {@link #SECONDARY} (dual) call; the role records which slot a context belongs to so teardown can end
     * the secondary before the primary and a dual call promotion can move a context between slots.
     */
    public enum CallRole {
        /**
         * The primary (first) call context.
         */
        PRIMARY,

        /**
         * The optional secondary (dual) call context.
         */
        SECONDARY
    }

    /**
     * Enumerates the direction of a call.
     *
     * <p>The direction selects which connected lonely interval the state machine picks on entering the
     * lonely state (the short interval for the caller, the long interval for the callee) and is projected
     * into the {@link Call#isOutgoing()} flag.
     */
    public enum CallDirection {
        /**
         * An outbound call the local user placed.
         */
        OUTGOING,

        /**
         * An inbound call the local user received.
         */
        INCOMING
    }

    /**
     * Holds the connected lonely timeout configuration of a call.
     *
     * <p>The state machine reads this configuration on entering {@link CallLifecycleState#CONNECTED_LONELY}
     * to pick the interval by direction and schedule the connected lonely timer. The three values are the
     * connected lonely timeouts: the {@linkplain #shortMillis() short} interval for the caller, the
     * {@linkplain #longMillis() long} interval for the callee, and the {@linkplain #maxMillis() maximum}
     * ceiling the timer steps up to.
     *
     * @param shortMillis the short connected lonely interval, in milliseconds
     * @param longMillis  the long connected lonely interval, in milliseconds
     * @param maxMillis   the maximum connected lonely interval, in milliseconds
     */
    public record ConnectedLonelyConfig(long shortMillis, long longMillis, long maxMillis) {
        /**
         * Returns the default connected lonely configuration.
         *
         * <p>The defaults are the connected lonely timeouts: a
         * {@value #CONNECTED_LONELY_DEFAULT_SHORT_MILLIS} ms short interval, a
         * {@value #CONNECTED_LONELY_DEFAULT_LONG_MILLIS} ms long interval, and a
         * {@value #CONNECTED_LONELY_DEFAULT_MAX_MILLIS} ms maximum.
         *
         * @return the default connected lonely configuration, never {@code null}
         */
        public static ConnectedLonelyConfig defaults() {
            return new ConnectedLonelyConfig(CONNECTED_LONELY_DEFAULT_SHORT_MILLIS,
                    CONNECTED_LONELY_DEFAULT_LONG_MILLIS, CONNECTED_LONELY_DEFAULT_MAX_MILLIS);
        }

        /**
         * Returns the connected lonely interval to use for the given direction.
         *
         * <p>The caller direction uses the {@linkplain #shortMillis() short} interval and the callee
         * direction uses the {@linkplain #longMillis() long} interval, mirroring the pick by direction on
         * entering the lonely state.
         *
         * @param direction the call direction
         * @return the connected lonely interval in milliseconds for the direction
         * @throws NullPointerException if {@code direction} is {@code null}
         */
        public long intervalForDirection(CallDirection direction) {
            Objects.requireNonNull(direction, "direction cannot be null");
            return direction == CallDirection.OUTGOING ? shortMillis : longMillis;
        }
    }
}
