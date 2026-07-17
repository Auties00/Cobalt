package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.engine.participant.VideoStreamState;
import com.github.auties00.cobalt.calls.capability.VoipCapabilities;
import com.github.auties00.cobalt.calls.config.param.VoipParamJsonDeserializer;
import com.github.auties00.cobalt.calls.config.param.VoipParamKey;
import com.github.auties00.cobalt.calls.config.CallsFeatureGate;
import com.github.auties00.cobalt.calls.config.VoipSettings;
import com.github.auties00.cobalt.calls.engine.control.*;
import com.github.auties00.cobalt.calls.engine.control.event.*;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipant;
import com.github.auties00.cobalt.calls.engine.participant.CallParticipantUserNode;
import com.github.auties00.cobalt.calls.crypto.CallKeyExchange;
import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.transport.datachannel.AppDataController;
import com.github.auties00.cobalt.calls.transport.RelayElection;
import com.github.auties00.cobalt.calls.transport.RelayLatencyState;
import com.github.auties00.cobalt.calls.platform.VoipHostApi;
import com.github.auties00.cobalt.calls.signaling.*;
import com.github.auties00.cobalt.calls.signaling.group.*;
import com.github.auties00.cobalt.calls.signaling.incall.*;
import com.github.auties00.cobalt.calls.signaling.link.*;
import com.github.auties00.cobalt.calls.signaling.receive.*;
import com.github.auties00.cobalt.calls.signaling.relay.*;
import com.github.auties00.cobalt.calls.signaling.session.*;
import com.github.auties00.cobalt.calls.signaling.waitingroom.*;
import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.call.*;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.info.CallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateTransition;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerScheduler;
import com.github.auties00.cobalt.calls.engine.event.LiveCallEventBus;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaPlane;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaSessionListener;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerKind;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallContextRegistry;
import com.github.auties00.cobalt.calls.engine.OrchestratedCall;
import com.github.auties00.cobalt.calls.engine.context.PendingCall;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange;
import com.github.auties00.cobalt.calls.platform.audio.LiveAudioCaptureDriver;
import com.github.auties00.cobalt.calls.platform.audio.LiveAudioPlaybackDriver;
import com.github.auties00.cobalt.calls.platform.LiveVoipHostApi;
import com.github.auties00.cobalt.calls.platform.VoipDriverManager;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.engine.info.CallInfoManager;
import com.github.auties00.cobalt.calls.engine.info.LiveCallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.event.LiveCallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.engine.mediaplane.LiveMediaSession;
import com.github.auties00.cobalt.calls.engine.mediaplane.LiveMediaDatagramSink;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.engine.context.LiveCallContextRegistry;
import com.github.auties00.cobalt.calls.engine.timer.LiveCallTimerScheduler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives a call through its phases by wiring the signaling, crypto, transport, media, state, timer, and
 * event units together.
 *
 * <p>This is the engine entry surface the call service layer calls into to place, answer, decline, join,
 * and end a call, and into which the signaling receiver forwards every decoded inbound action. It owns no
 * transport, codec, or state machine logic of its own; it orchestrates the units that do. For each call it
 * threads the pieces together in order:
 * <ul>
 *   <li>allocates and frees the engine call context through {@link CallContextRegistry} as a call starts
 *       and ends;</li>
 *   <li>mints and distributes the 32 byte end to end call key through {@link CallKeyExchange}, once in the
 *       offer for a one to one call;</li>
 *   <li>builds each signaling action with {@link CallStanza} and the typed {@link CallMessage} records and
 *       ships it, the offer through {@link OfferAckSender} for its synchronous relay bearing ack and every
 *       other leg through {@link VoipHostApi#sendSignaling(Stanza)} fire and forget;</li>
 *   <li>brings up the media plane through {@link MediaPlane} once the call is answered and the relay block
 *       is known;</li>
 *   <li>advances the internal state machine through {@link CallStateTransition} and fires the state event
 *       through {@link CallLifecycleEventSink} on every accepted change;</li>
 *   <li>arms and cancels the per call timers through {@link CallTimerScheduler};</li>
 *   <li>folds each lifecycle event into the call's result snapshot through {@link CallInfoUpdater}; and</li>
 *   <li>emits the typed lifecycle events through {@link CallLifecycleEventSink}.</li>
 * </ul>
 *
 * <p>The controller keeps the public {@link Call} view and the cross unit wiring per call in an
 * {@link OrchestratedCall} handle, registered by call identifier; the engine call context, its timer
 * entries, and its durations live in the state, timer, and info manager units this controller drives by
 * call identifier. At most a primary and one secondary call are admitted at once; a third placement or a
 * second inbound offer while two calls are live is refused. Every public method blocks on its own virtual
 * thread per the Cobalt threading model (a native await is a plain blocking call here); a single call's
 * orchestration fields are read and mutated under that call's {@linkplain OrchestratedCall#lock() per call
 * lock}, so a user action and an inbound signal on the same call never interleave a half applied phase
 * change.
 *
 * <p>Call failures are isolated, never session fatal: a transport or media bring up that cannot start
 * surfaces as a nonfatal {@link WhatsAppCallException.DataChannel} that ends only that call, so a call
 * never tears the messaging session down. A precondition violation (placing a call while not connected,
 * answering a call that does not exist) is an {@link IllegalStateException} or
 * {@link IllegalArgumentException} the caller is expected to avoid.
 *
 * @apiNote This is an internal engine surface, not a public client API; the call service layer is the only
 * caller, and it adapts the application's place, accept, reject, and end requests onto these methods and
 * wires the signaling receiver's sink onto {@link #handleIncomingMessage(CallMessage, Jid)}. Embedders
 * never call this controller directly.
 * @implNote This implementation drives the per call timers on a virtual thread scheduler, keeps state on
 * the JVM heap, and serializes each call's fields under a {@link ReentrantLock} per the Cobalt threading
 * model; the media and transport bring up is reached through the {@link MediaPlane} seam over the codec and
 * transport units rather than performed inline. The result and end reason axis is kept separate from the
 * state machine ({@link CallResult} and {@link CallInfoUpdater} versus {@link CallStateTransition}).
 */
public final class LifecycleController {
    /**
     * The logger for {@link LifecycleController}.
     */
    private static final System.Logger LOGGER = Log.get(LifecycleController.class);

    /**
     * The number of random bytes drawn to seed a call identifier.
     *
     * <p>Sixteen random bytes are hex encoded into the 32 character call identifier.
     */
    private static final int CALL_ID_RANDOM_BYTES = 16;

    /**
     * The hex digits used to encode a call identifier, with the high sixteen entries in upper case and
     * the low sixteen in lower case so the encoded identifier's case varies across its characters.
     *
     * <p>Encoding a byte's high nibble then its low nibble through this table yields the mixed case
     * identifier.
     */
    private static final char[] CALL_ID_HEX = "0123456789ABCDEF0123456789abcdef".toCharArray();

    /**
     * The {@code join-state} attribute value stamped on the single {@code link_join} request.
     *
     * <p>A call link join sends exactly one {@code link_join} stanza carrying a single {@code join-state}
     * attribute whose value is {@code 2} by default and {@code 1} under a call state condition Cobalt's
     * link join entry does not yet model, so the default {@code 2} is sent. See the
     * {@link #joinCallLink(Jid, String, CallLinkMedia, boolean, MediaStreams)} TODO for the precise blocker.
     */
    private static final int LINK_JOIN_STATE_DEFAULT = 2;

    /**
     * The all zeros sentinel call id a call link joiner registers under until the join ack supplies the
     * relay assigned id.
     *
     * <p>A joiner mints its call object under this 32 character zero string at preview time and adopts the
     * relay assigned id from the join ack.
     */
    private static final String PLACEHOLDER_CALL_ID = "00000000000000000000000000000000";

    /**
     * The wire {@code rate} of the narrowband audio format offered.
     */
    private static final int AUDIO_RATE_NARROWBAND = 8000;

    /**
     * The wire {@code rate} of the wideband audio format offered.
     */
    private static final int AUDIO_RATE_WIDEBAND = 16000;

    /**
     * The wire {@code enc} codec name of the offered audio format.
     */
    private static final String AUDIO_CODEC_OPUS = "opus";

    /**
     * The wire {@code dec} decode codec token of the offered video format.
     *
     * <p>Every video offer advertises a single
     * {@code <video dec="H264" enc="h.264" device_orientation="0" screen_width="0" screen_height="0"/>}
     * element, so the offer advertises H.264 as the decode token.
     */
    private static final String VIDEO_DEC_TOKEN = "H264";

    /**
     * The wire {@code enc} encode codec name of the offered video format, the lowercase form paired with
     * the {@link #VIDEO_DEC_TOKEN} decode token.
     */
    private static final String VIDEO_ENC_NAME = "h.264";

    /**
     * The {@code <net medium>} classification an outbound offer advertises.
     *
     * <p>Every offer carries {@code <net medium="3"/>}; the caller advertises medium three and the callee
     * selects {@link #NET_MEDIUM_ACCEPT} in its accept.
     */
    private static final int NET_MEDIUM_OFFER = 3;

    /**
     * The {@code <net medium>} classification an outbound accept selects, {@code <net medium="2"/>} on the
     * wire.
     */
    private static final int NET_MEDIUM_ACCEPT = 2;

    /**
     * The capability advertisement version stamped on the {@code <capability>} element.
     */
    private static final int CAPABILITY_VERSION = 1;

    /**
     * The wire element tag of an engine parameter bundle the offer and offer acknowledgement carry.
     */
    private static final String VOIP_SETTINGS_ELEMENT = "voip_settings";

    /**
     * Sends an offer and returns its synchronous, relay bearing call ack.
     */
    private final OfferAckSender offerAckSender;

    /**
     * Mints, wraps, distributes, and recovers the end to end call key over the Signal pipeline.
     */
    private final CallKeyExchange crypto;

    /**
     * Sends the fire and forget signaling legs and supplies the cryptographically strong randomness that
     * seeds a call identifier.
     */
    private final VoipHostApi host;

    /**
     * Allocates and frees the engine call context for each call.
     */
    private final CallContextRegistry registry;

    /**
     * Drives the internal state machine through the transition guard.
     */
    private final CallStateTransition stateMachine;

    /**
     * Arms and cancels the per call timers.
     */
    private final CallTimerScheduler timers;

    /**
     * Folds each lifecycle event into the call's result snapshot.
     */
    private final CallInfoUpdater infoUpdater;

    /**
     * Applies the should emit gate and fans each lifecycle event out to the listeners.
     */
    private final CallLifecycleEventSink events;

    /**
     * Brings up and tears down each call's media plane.
     */
    private final MediaPlane mediaPlane;

    /**
     * The listener passed into each {@link MediaPlane#bringUp bring up} that forwards the media plane's two
     * per call notifications back to this controller.
     *
     * <p>The media plane holds no reference to this controller; instead each brought up call reports its
     * media connected and local capture interrupted events through this listener, so the controller owns the
     * media plane as a plain constructor dependency with no cycle. One instance is reused across calls
     * because both callbacks forward only the call id to the controller's entry points.
     */
    private final MediaSessionListener mediaSessionListener = new MediaSessionListener() {
        @Override
        public void onConnected(String callId) {
            onMediaConnected(callId);
        }

        @Override
        public void onCaptureInterrupted(String callId) {
            onLocalCaptureInterrupted(callId);
        }
    };

    /**
     * Validates, deduplicates, and classifies each decoded inbound signaling message before dispatch.
     *
     * <p>The router is stateless (the per call deduplication state is threaded in and out through each
     * call's {@link OrchestratedCall#dedupState()}), so one instance classifies every inbound call. It is
     * parameterised on the controller's own {@link OrchestratedCall} handle and resolves a context through
     * the {@code calls} working set lookup, so its verdict's resolved context is the very handle the per
     * type inbound handlers re resolve through {@code calls.get}.
     */
    private final IncomingMessageRouter<OrchestratedCall> incomingRouter =
            new IncomingMessageRouter<>();

    /**
     * Holds the controller's per call orchestration handles, keyed by call identifier.
     *
     * <p>The map is the controller's own working set, distinct from the engine call context the state and
     * timer units own; it is bounded at {@code 2} by the dual call guard.
     */
    private final ConcurrentHashMap<String, OrchestratedCall> calls = new ConcurrentHashMap<>();

    /**
     * Holds the single buffered busy or lobby pending call, or {@code null} when none is buffered.
     *
     * <p>When an inbound offer arrives while the dual call ceiling is reached and the offer's
     * {@code <voip_settings>} enable the pending call path, the offer is not dropped but retained here as a
     * {@link PendingCall}; every later signaling message for that call is appended to it through the
     * {@link IncomingMessageRouter.RoutingClass#BUFFER_PENDING} router verdict, so nothing is lost in the
     * interval before the local user joins. The field is a single reference because one pending call context
     * is buffered at a time; it is {@code volatile} so the socket reader thread that buffers into it and any
     * join path reader see a consistent reference, and the {@link PendingCall} itself guards its own queue
     * and state.
     */
    private volatile PendingCall pendingCall;

    /**
     * The shared host event bus a call's in call control units publish their host facing events onto, or
     * {@code null} when the controller is built without a host boundary.
     *
     * <p>Passed as a constructor argument; a controller built through the bare constructor (the test
     * harnesses) leaves it {@code null}, so a call carries no in call control units and the lifecycle path
     * (offer, accept, reject, terminate) is exercisable without it.
     */
    private final LiveCallEventBus eventBus;

    /**
     * The typed read facade over the server calling AB props that gate which call operations may start, or
     * {@code null} when the controller is built without a host boundary.
     *
     * <p>Passed as a constructor argument; a controller built through the bare constructor leaves it
     * {@code null}, so the start, group start, and screen share entry points apply no feature gating.
     */
    private final CallsFeatureGate featureGate;

    /**
     * The request reply IQ sender the per call {@link CallLinkController} and {@link WaitingRoomController}
     * dispatch their {@code to="call"} link and waiting room IQs through, or {@code null} when the
     * controller is built without a host boundary.
     *
     * <p>Passed as a constructor argument; a controller built through the bare constructor leaves it
     * {@code null}, so the call link join entry point ({@link #joinCallLink(Jid, String, CallLinkMedia,
     * boolean, MediaStreams)}) cannot run, since the link query and join handshake is a blocking round trip
     * with no fire and forget fallback.
     */
    private final CallLinkIqSender callLinkIqSender;

    /**
     * The account sub-store the local self identity is read from, or {@code null} when the controller is
     * built without a host boundary.
     *
     * <p>Read live per call by {@link #selfDeviceJid()} for the JID an in call control context is stamped
     * with (so a self JID that became known after the engine was built is picked up), and by
     * {@link #isOwnDevice(Jid)} for the companion device terminate guard in
     * {@link #handlePeerTerminate(TerminateStanza, Jid)}. A controller built through the bare constructor
     * leaves it {@code null}, so an outbound call falls back to its creator for the self JID and the
     * companion guard never fires.
     */
    private final LinkedWhatsAppAccountStore accountStore;

    /**
     * The observer the three end of call outcomes are reported to, never {@code null}.
     *
     * <p>The controller reports a call's media connected, engine result, and ended events up to this
     * observer, which the call service satisfies so it can stamp the connected instant, record the result,
     * and free the call's service level state, drain its end of call WAM telemetry, and push the call log at
     * teardown. Set at construction from the {@link #create} factory (the service passes itself) or
     * {@link CallLifecycleObserver#NONE} for a bare build.
     */
    private final CallLifecycleObserver observer;

    /**
     * Constructs a lifecycle controller over its nine collaborating units with no host boundary.
     *
     * <p>This is the bare engine build the test harnesses use: the event bus, self JID supplier, feature
     * gate, own device resolver, and call link sender are left {@code null} and the call log, result,
     * teardown, and connected sinks default to no ops, so the signaling and media lifecycle is exercisable
     * with no live client, AB props service, or store. The production build goes through {@link #create},
     * which supplies every host boundary seam.
     *
     * @param offerAckSender the sender that ships an offer and returns its synchronous call ack
     * @param crypto         the call key crypto facade over the Signal pipeline
     * @param host           the host API supplying signaling egress and cryptographically strong
     *                       randomness
     * @param registry       the seam that allocates and frees the engine call context
     * @param stateMachine   the state transition guard
     * @param timers         the per call timer scheduler
     * @param infoUpdater    the info manager update that folds events into the result snapshot
     * @param events         the event sink that gates and fans out the typed events
     * @param mediaPlane     the media plane bring up and teardown seam
     * @throws NullPointerException if any argument is {@code null}
     */
    public LifecycleController(OfferAckSender offerAckSender, CallKeyExchange crypto,
                                     VoipHostApi host, CallContextRegistry registry,
                                     CallStateTransition stateMachine, CallTimerScheduler timers,
                                     CallInfoUpdater infoUpdater, CallLifecycleEventSink events,
                                     MediaPlane mediaPlane) {
        this(offerAckSender, crypto, host, registry, stateMachine, timers, infoUpdater, events, mediaPlane,
                null, null, null, null, CallLifecycleObserver.NONE);
    }

    /**
     * Constructs a lifecycle controller over its nine collaborating units, its client host, and its
     * observer.
     *
     * <p>The nine units are the engine's own collaborators; the four client host arguments (event bus,
     * feature gate, call link sender, account store) carry the client environment the engine talks up to,
     * each of which may be {@code null} for a bare engine build, and the {@link CallLifecycleObserver}
     * carries the three end of call outcomes the engine reports up to the call service. A {@code null} host
     * argument is treated as "that capability is absent"; the bare constructor passes {@code null} for all
     * four and {@link CallLifecycleObserver#NONE}.
     *
     * @param offerAckSender the sender that ships an offer and returns its synchronous call ack
     * @param crypto         the call key crypto facade over the Signal pipeline
     * @param host           the host API supplying signaling egress and cryptographically strong
     *                       randomness
     * @param registry       the seam that allocates and frees the engine call context
     * @param stateMachine   the state transition guard
     * @param timers         the per call timer scheduler
     * @param infoUpdater    the info manager update that folds events into the result snapshot
     * @param events         the event sink that gates and fans out the typed events
     * @param mediaPlane     the media plane bring up and teardown seam
     * @param eventBus       the shared host event bus the in call control units publish onto, or
     *                       {@code null} for a bare build
     * @param featureGate    the server calling feature gate the start entry points consult, or {@code null}
     *                       for a bare build
     * @param callLinkIqSender the request reply IQ sender the link and waiting room units dispatch through,
     *                       or {@code null} for a bare build
     * @param accountStore   the account sub-store the local self device JID and own device check are read
     *                       from, or {@code null} for a bare build
     * @param observer       the observer the end of call outcomes are reported to, never {@code null}
     * @throws NullPointerException if any unit argument or {@code observer} is {@code null}
     */
    private LifecycleController(OfferAckSender offerAckSender, CallKeyExchange crypto,
                                     VoipHostApi host, CallContextRegistry registry,
                                     CallStateTransition stateMachine, CallTimerScheduler timers,
                                     CallInfoUpdater infoUpdater, CallLifecycleEventSink events,
                                     MediaPlane mediaPlane, LiveCallEventBus eventBus,
                                     CallsFeatureGate featureGate, CallLinkIqSender callLinkIqSender,
                                     LinkedWhatsAppAccountStore accountStore,
                                     CallLifecycleObserver observer) {
        this.offerAckSender = Objects.requireNonNull(offerAckSender, "offerAckSender cannot be null");
        this.crypto = Objects.requireNonNull(crypto, "crypto cannot be null");
        this.host = Objects.requireNonNull(host, "host cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine cannot be null");
        this.timers = Objects.requireNonNull(timers, "timers cannot be null");
        this.infoUpdater = Objects.requireNonNull(infoUpdater, "infoUpdater cannot be null");
        this.events = Objects.requireNonNull(events, "events cannot be null");
        this.mediaPlane = Objects.requireNonNull(mediaPlane, "mediaPlane cannot be null");
        this.eventBus = eventBus;
        this.featureGate = featureGate;
        this.callLinkIqSender = callLinkIqSender;
        this.accountStore = accountStore;
        this.observer = Objects.requireNonNull(observer, "observer cannot be null");
    }

    /**
     * Assembles a fully wired live lifecycle controller from a client's call engine units.
     *
     * <p>This is the engine composition root: it builds the units a live client owns and threads them
     * into the controller. The offer ack send rides the client's request correlated send, the call key
     * crypto is the {@link LiveCallKeyExchange} facade over the reused Signal pipeline, signaling egress
     * and randomness come from {@link LiveVoipHostApi}, the at most two call contexts live in a single
     * {@link CallManager}, the transition guard is the {@link CallStateMachine} over that manager, the per
     * call timers run on the {@link LiveCallTimerScheduler}, the call info snapshots refresh through a
     * single {@link CallInfoManager}, and the {@link MediaPlane} is the real
     * {@link LiveMediaSession.LiveMediaPlane}. The client environment (event bus, feature gate, call link
     * sender, account store) is passed as four constructor arguments, and {@code observer} carries the
     * end of call outcomes back to the call service.
     *
     * <p>The whole engine is a directed acyclic graph with no post construction wiring: the scheduler holds
     * no controller (a fired timer runs the {@link Runnable} the controller arms it with) and the media
     * plane holds no controller (each brought up call reports its media connected and capture interrupted
     * events through the per call {@link MediaSessionListener} the controller passes to
     * {@link MediaPlane#bringUp bringUp}), so neither needs a holder or a setter to reach the controller.
     *
     * @apiNote The call service is the only caller; it passes itself as the {@code observer} so the engine
     * reports back into the service as a plain constructor dependency.
     * @implNote This implementation initializes the engine's single {@link VoipDriverManager} once here,
     * owning the audio capture and playback drivers and the camera and screen share source factories every
     * brought up media session routes through.
     *
     * @param whatsapp          the owning client, used for signaling egress and the offer ack round trip
     * @param messageEncryption the encryption service the call key crypto wraps the key with
     * @param messageService    the message service the call key crypto decrypts inbound key envelopes with
     * @param deviceService     the device service the call key crypto ensures sessions through
     * @param store             the store supplying the local ADV signed device identity and account identity
     * @param abPropsService    the AB props service the {@link CallsFeatureGate} reads its calling feature
     *                          flags from
     * @param eventBus          the shared host event bus the in call control units publish onto
     * @param observer          the observer the media connected, result, and ended outcomes are reported to
     * @return a fully wired lifecycle controller
     * @throws NullPointerException if any of the engine dependencies is {@code null}
     */
    public static LifecycleController create(LinkedWhatsAppClient whatsapp,
                                             MessageEncryption messageEncryption,
                                             MessageService messageService,
                                             DeviceService deviceService,
                                             LinkedWhatsAppStore store,
                                             ABPropsService abPropsService,
                                             LiveCallEventBus eventBus,
                                             CallLifecycleObserver observer) {
        Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        Objects.requireNonNull(messageEncryption, "messageEncryption cannot be null");
        Objects.requireNonNull(messageService, "messageService cannot be null");
        Objects.requireNonNull(deviceService, "deviceService cannot be null");
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        Objects.requireNonNull(eventBus, "eventBus cannot be null");
        Objects.requireNonNull(observer, "observer cannot be null");

        var secureRandom = new SecureRandom();
        CallKeyExchange crypto =
                new LiveCallKeyExchange(messageEncryption, messageService, deviceService, store, secureRandom);
        var manager = new CallManager();
        var stateMachine = new CallStateMachine(manager);
        var infoManager = new CallInfoManager();
        var events = new LiveCallLifecycleEventSink();
        VoipHostApi host = new LiveVoipHostApi(whatsapp, new LiveMediaDatagramSink(), frame -> {
        }, (eventType, payload) -> {
        });

        var voipDriverManager = newVoipDriverManager();
        voipDriverManager.initialize();
        // Built once and shared: this gate backs the controller's start, group start, and screen share
        // gating and supplies the group call initial BWE seed decision the media plane reads per bring up so
        // the AB props cache is warm when a call starts rather than read on a cold cache here at assembly.
        var featureGate = new CallsFeatureGate(abPropsService);
        var mediaPlane = new LiveMediaSession.LiveMediaPlane(host, store.accountStore(), voipDriverManager,
                featureGate);

        var timers = new LiveCallTimerScheduler(manager, events, featureGate);
        var controller = new LifecycleController(
                new LiveOfferAckSender(whatsapp),
                crypto,
                host,
                new LiveCallContextRegistry(manager),
                stateMachine,
                timers,
                new LiveCallInfoUpdater(manager, infoManager),
                events,
                mediaPlane,
                eventBus,
                featureGate,
                new LiveCallLinkIqSender(whatsapp),
                store.accountStore(),
                observer);
        if (Log.INFO) LOGGER.log(Level.INFO, "call engine assembled");
        return controller;
    }

    /**
     * Builds the engine's single {@link VoipDriverManager} over fresh platform capture and playback
     * drivers and the default camera and screen share source factories.
     *
     * <p>The manager owns the two audio capture drivers (microphone and system audio loopback), the audio
     * playback driver, and the camera and screen share video source factories for the lifetime of the
     * engine; each brought up media session routes its capture and playback bring up through this one
     * manager rather than opening a device directly. The returned manager is not yet
     * {@linkplain VoipDriverManager#initialize() initialized}; the caller initializes it once.
     *
     * @return a fresh, uninitialized driver manager owning the engine's capture and playback drivers
     */
    private static VoipDriverManager newVoipDriverManager() {
        return new VoipDriverManager(
                new LiveAudioCaptureDriver(),
                new LiveAudioCaptureDriver(),
                new LiveAudioPlaybackDriver(),
                deviceId -> VideoOutput.fromCamera(),
                surfaceId -> VideoOutput.fromScreen());
    }

    /**
     * Reports whether a device JID is one of the local account's own devices for the companion device
     * terminate guard.
     *
     * <p>A device JID belongs to the local account when it resolves to the same account as the account's
     * own phone number JID or LID, or when it appears in the account's linked device list; the account
     * equality test normalizes the device and agent suffixes away so a device JID
     * ({@code user:device@server}) matches the bare account JID. The account store is read live on each call
     * so a self JID, LID, or linked device set that became known after the engine was built is picked up. A
     * bare build with no account store never reports a device as own, so the companion guard never fires.
     *
     * @implNote This implementation runs {@link Jid#isSameAccount(Jid)} against both the phone number JID
     * and the LID, because a call's signaling sender may use either addressing mode, and falls back to the
     * linked device list as the explicit enumeration of the account's companions when the self identity is
     * not yet populated.
     *
     * @param deviceJid the device JID to test, never {@code null}
     * @return {@code true} when {@code deviceJid} is one of the local account's own devices
     */
    private boolean isOwnDevice(Jid deviceJid) {
        if (accountStore == null) {
            return false;
        }
        if (accountStore.jid().map(deviceJid::isSameAccount).orElse(false)
                || accountStore.lid().map(deviceJid::isSameAccount).orElse(false)) {
            return true;
        }
        for (var linkedDevice : accountStore.linkedDevices()) {
            if (deviceJid.isSameAccount(linkedDevice)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the local device JID an in call control context is stamped with, read live from the account
     * store, or {@code null} when the account is not yet known or the controller has no host boundary.
     *
     * <p>The account phone number JID, read live per call so a JID that became known after the engine was
     * built is picked up. A bare build with no account store returns {@code null}, and the control context
     * setup falls back to the call's creator, which already is the local device for an outbound call.
     *
     * @return the local device JID, or {@code null} when unavailable
     */
    private Jid selfDeviceJid() {
        return accountStore == null ? null : accountStore.jid().orElse(null);
    }

    /**
     * Reports the local user's own mute state for a call, announcing it to the peer.
     *
     * <p>Delegates to the call's {@link MuteController#setMuted(boolean)}, which sends the {@code mute_v2}
     * self state action and emits the mute change; a recent unmute opens the controller's thirty second
     * lockout against an inbound peer mute. A call that is not tracked, or one whose control units are not
     * built (the bus is unbound or the call is not yet answerable), is a no op.
     *
     * @param callId the identifier of the call whose mute state is reported
     * @param muted  {@code true} to report the local user muted, {@code false} to report unmuted
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void setMuted(String callId, boolean muted) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.mute().setMuted(muted));
    }

    /**
     * Turns the local camera on or off for a call, announcing the video state to the peer.
     *
     * <p>Delegates to the call's {@link VideoStateController#turnCamera(boolean)}, which sends the
     * {@code video_state} action and emits the video change. This is the announce half only; the actual
     * camera capture on the media plane is driven separately. A call that is not tracked, or one whose
     * control units are not built, is a no op.
     *
     * @param callId  the identifier of the call whose video state is reported
     * @param enabled {@code true} to turn the camera on, {@code false} to turn it off
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void setVideoEnabled(String callId, boolean enabled) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.video().turnCamera(enabled));
    }

    /**
     * Starts the local camera video track on a call, announcing the video state and driving the media plane.
     *
     * <p>This is the in call camera turn on used by an audio to video upgrade: it announces video on to the
     * peer through the call's {@link VideoStateController#turnCamera(boolean) turnCamera(true)} and starts the
     * outbound camera capture and encode path on the call's media plane through
     * {@link MediaPlane.Session#startLocalVideo()}. The announce and the media plane start are the two halves
     * of turning the local camera on: {@link #setVideoEnabled(String, boolean)} carries only the announce,
     * while this also raises the local video track. A call that is not tracked, one whose control units are
     * not built (the bus is unbound or the call is not yet answerable), or one whose media plane is not yet up
     * runs only the parts it can: the announce fires once the control units exist, and the media plane start
     * fires once the session is up.
     *
     * @implNote This implementation drives the {@code video_state} announce and the local camera track start
     * together, the announce through the per call {@link VideoStateController} and the camera track start
     * through the media session's {@link MediaPlane.Session#startLocalVideo()} seam. Both run under the call's
     * lock so the announce and the media plane start are not interleaved with a concurrent teardown.
     *
     * @param callId the identifier of the call whose local camera is turned on
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void startLocalVideo(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            ensureControls(orchestrated);
            orchestrated.controls().ifPresent(controls -> controls.video().turnCamera(true));
            orchestrated.mediaSession().ifPresent(MediaPlane.Session::startLocalVideo);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Issues a video upgrade request for a one to one call, asking the peer to turn an audio call to video.
     *
     * <p>Delegates to the call's {@link VideoStateController#requestUpgrade(boolean)} with the v1 request
     * shape; the peer answers with an accept or reject. A call that is not tracked, or one whose control
     * units are not built, is a no op.
     *
     * @param callId the identifier of the call to upgrade
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void requestVideoUpgrade(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.video().requestUpgrade(false));
    }

    /**
     * Rejects a pending video upgrade request on a call.
     *
     * <p>Delegates to the call's {@link VideoStateController#rejectUpgrade()}. A call that is not tracked, or
     * one whose control units are not built, is a no op.
     *
     * @param callId the identifier of the call whose upgrade request is rejected
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void rejectVideoUpgrade(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.video().rejectUpgrade());
    }

    /**
     * Starts the local screen share stream for a call, announcing it to the peer.
     *
     * <p>Delegates to the call's {@link ScreenShareController#start()}, which sends the {@code screen_share}
     * action at the negotiated protocol version and emits the screen share change. This is the V2 announce
     * only half; the V3 dual stream auxiliary media stream is not started here. A call that is not tracked,
     * or one whose control units are not built, is a no op. When the server feature gate is bound and reports
     * screen sharing disabled, the request is refused rather than silently dropped.
     *
     * @param callId the identifier of the call to start sharing on
     * @throws NullPointerException  if {@code callId} is {@code null}
     * @throws IllegalStateException if in call screen sharing is disabled for this account by the server
     *                               feature gate
     */
    public void startScreenShare(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var gate = featureGate;
        if (gate != null && !gate.isScreenShareEnabled()) {
            throw new IllegalStateException(
                    "Cannot start screen sharing: it is disabled for this account");
        }
        withControls(callId, controls -> controls.screenShare().start());
    }

    /**
     * Stops the local screen share stream for a call, announcing it to the peer.
     *
     * <p>Delegates to the call's {@link ScreenShareController#stop()}. A call that is not tracked, or one
     * whose control units are not built, is a no op.
     *
     * @param callId the identifier of the call to stop sharing on
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void stopScreenShare(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.screenShare().stop());
    }

    /**
     * Enables or disables a call's waiting room gate as the call host.
     *
     * <p>Delegates to the call's {@link WaitingRoomController#setEnabled(boolean)}, which dispatches the
     * waiting room toggle IQ and emits the applied gate ack event. A call that is not tracked, one whose
     * control units are not built, or one not joined through a call link (so it carries no waiting room
     * controller) is a no op.
     *
     * @param callId  the identifier of the call whose waiting room gate is toggled
     * @param enabled {@code true} to enable the waiting room, {@code false} to disable it
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void setWaitingRoomEnabled(String callId, boolean enabled) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> {
            if (controls.waitingRoom() != null) {
                controls.waitingRoom().setEnabled(enabled);
            }
        });
    }

    /**
     * Admits a single queued participant into a call from its waiting room lobby as the call host.
     *
     * <p>Delegates to the call's {@link WaitingRoomController#admit(Jid)}, which dispatches the admit IQ and
     * emits the admitted participants ack event. A call that is not tracked, one whose control units are not
     * built, or one not joined through a call link is a no op.
     *
     * @param callId  the identifier of the call the participant is admitted into
     * @param userJid the device JID of the participant to admit
     * @throws NullPointerException if {@code callId} or {@code userJid} is {@code null}
     */
    public void admitWaitingRoomParticipant(String callId, Jid userJid) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(userJid, "userJid cannot be null");
        withControls(callId, controls -> {
            if (controls.waitingRoom() != null) {
                controls.waitingRoom().admit(userJid);
            }
        });
    }

    /**
     * Admits every queued participant into a call from its waiting room lobby at once as the call host.
     *
     * <p>Delegates to the call's {@link WaitingRoomController#admitAll()}, which dispatches the admit all IQ
     * and emits the admitted participants ack event. A call that is not tracked, one whose control units are
     * not built, or one not joined through a call link is a no op.
     *
     * @param callId the identifier of the call whose lobby is drained
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void admitAllWaitingRoomParticipants(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> {
            if (controls.waitingRoom() != null) {
                controls.waitingRoom().admitAll();
            }
        });
    }

    /**
     * Denies a single queued participant admission to a call from its waiting room lobby as the call host.
     *
     * <p>Delegates to the call's {@link WaitingRoomController#deny(Jid)}, which dispatches the deny IQ and
     * emits the denied participants ack event. A call that is not tracked, one whose control units are not
     * built, or one not joined through a call link is a no op.
     *
     * @param callId  the identifier of the call the participant is denied from
     * @param userJid the device JID of the participant to deny
     * @throws NullPointerException if {@code callId} or {@code userJid} is {@code null}
     */
    public void denyWaitingRoomParticipant(String callId, Jid userJid) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(userJid, "userJid cannot be null");
        withControls(callId, controls -> {
            if (controls.waitingRoom() != null) {
                controls.waitingRoom().deny(userJid);
            }
        });
    }

    /**
     * Routes an outbound in call interaction for a call onto its engine control plane.
     *
     * <p>Switches the sealed {@link CallInteraction} onto the control unit that owns its plane: a
     * {@link CallInteraction.RaiseHand} or {@link CallInteraction.LowerHand} drives the call's
     * {@link RaiseHandController#setHandRaised(boolean)}, a {@link CallInteraction.PeerMuteRequest} drives the
     * call's {@link MuteController#requestPeerMute(Jid)} against the named target, and a
     * {@link CallInteraction.VideoUpgradeRequest} drives {@link #requestVideoUpgrade(String)}. A
     * {@link CallInteraction.Reaction} drives the call's {@link ReactionController} over the media plane's
     * application data side channel when that plane is up; it is accepted without effect when the call
     * carries no app data plane. A {@link CallInteraction.KeyFrameRequest} arms the outbound video encoder for
     * a fresh key frame through the call's {@link MediaPlane.Session#requestKeyFrame() media session}; it is
     * accepted without effect when the call's media plane is not up or carries no video. A call that is not
     * tracked, or one whose control units are not built, is a no op.
     *
     * @implNote This implementation handles the signaling plane interactions (raise hand, peer mute, video
     * upgrade) and routes a reaction onto the {@link ReactionController} bound to the call's
     * {@link AppDataController}, since reactions ride the app data side channel rather than signaling. The
     * outbound key frame request (the local application asking the encoder for an intra frame) is forwarded to
     * the media session's {@link MediaPlane.Session#requestKeyFrame()} passthrough. Acting on an inbound peer
     * picture loss indication (a relayed RTCP PLI or FIR) to trigger a local key frame is a separate, media
     * internal path kept below the call control layer, so it is owned by the media and transport units and
     * wired there: {@link com.github.auties00.cobalt.calls.transport.rtcp.RtcpFeedbackParser} decodes the PSFB
     * PLI or FIR onto {@code RtcpFeedback}, and the media session's inbound RTCP listener arms the video
     * encoder on it. It is not routed through this controller.
     * @param callId      the identifier of the call the interaction targets
     * @param interaction the interaction to route
     * @throws NullPointerException if {@code callId} or {@code interaction} is {@code null}
     */
    public void sendInteraction(String callId, CallInteraction interaction) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(interaction, "interaction cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            ensureControls(orchestrated);
            // The key frame arm is the only interaction that reaches the media plane rather than a control
            // unit, so it is dispatched whenever the session is up even before the control units exist; the
            // remaining arms run only once the control units are built.
            if (interaction instanceof CallInteraction.KeyFrameRequest) {
                orchestrated.mediaSession().ifPresent(MediaPlane.Session::requestKeyFrame);
                return;
            }
            var controls = orchestrated.controls().orElse(null);
            if (controls == null) {
                return;
            }
            switch (interaction) {
                case CallInteraction.RaiseHand ignored -> controls.raiseHand().setHandRaised(true);
                case CallInteraction.LowerHand ignored -> controls.raiseHand().setHandRaised(false);
                case CallInteraction.PeerMuteRequest request ->
                        controls.mute().requestPeerMute(Jid.of(request.target()));
                case CallInteraction.VideoUpgradeRequest ignored -> controls.video().requestUpgrade(false);
                case CallInteraction.Reaction reaction ->
                        controls.reactionControl().ifPresent(unit -> unit.sendReaction(reaction.emoji()));
                case CallInteraction.KeyFrameRequest _ -> {
                    // Handled above against the media session before the control unit gate.
                }
            }
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Places an outbound one to one call to a peer's devices and rings them.
     *
     * <p>Allocates the call, generates its identifier, mints the call key and fans it out per peer device
     * inside the offer, builds and sends the offer, and on a positive ack records the caller's relay block.
     * The call enters {@link CallLifecycleState#CALLING} with the initial result
     * {@link CallResult#CALL_OFFER_ACK_NOT_RECEIVED} and the caller lonely timer armed; a positive offer ack
     * clears that initial result, and a NACK ends the call. The caller's media plane is brought up only once
     * the peer answers (the offer ack relay credentials are rejected by the relay until the accept arrives),
     * so this method returns with the call ringing rather than connected. The returned {@link Call} is the
     * live view the application observes as the call progresses.
     *
     * @implNote This implementation requires the starting precondition (no call already live for the local
     * side beyond the dual call ceiling), allocates the primary orchestration handle, generates the 16 byte
     * call identifier, mints the raw key through {@link CallKeyExchange#mintCallKey()}, and ships the offer.
     * The caller lonely timer is the ringing watchdog; the {@link CallResult#CALL_OFFER_ACK_NOT_RECEIVED}
     * initial result is set on a freshly placed call and cleared on the offer ack.
     *
     * @param self       the local account's device JID, stamped as the call creator
     * @param peer        the peer user JID being called
     * @param deviceJids  the peer device JIDs to fan the offer and call key out to
     * @param video       whether the call is placed with video enabled
     * @param streams     the application capture sources and playback sinks the media plane drives once the
     *                    peer answers, or {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code self}, {@code peer}, {@code deviceJids}, or {@code streams}
     *                                  is {@code null}, or {@code deviceJids} contains a {@code null} element
     * @throws IllegalArgumentException if {@code peer} is a group or community JID, or {@code deviceJids}
     *                                  is empty
     * @throws IllegalStateException    if the dual call ceiling is already reached, or one to one web calling
     *                                  is disabled for this account by the server feature gate
     * @throws WhatsAppCallException    if the offer could not be sent or no offer ack arrived
     */
    public Call startCall(Jid self, Jid peer, List<Jid> deviceJids, boolean video, MediaStreams streams) {
        Objects.requireNonNull(self, "self cannot be null");
        Objects.requireNonNull(peer, "peer cannot be null");
        Objects.requireNonNull(streams, "streams cannot be null");
        var devices = List.copyOf(deviceJids);
        if (peer.hasGroupOrCommunityServer()) {
            throw new IllegalArgumentException("startCall is for one-to-one calls; got group/community JID " + peer);
        }
        if (devices.isEmpty()) {
            throw new IllegalArgumentException("deviceJids cannot be empty for an outbound offer");
        }
        var gate = featureGate;
        // enable_web_calling master gate (AB prop id 15461): WA gates the 1:1 outbound offer on
        // WAWebVoipGatingUtils.isCallingEnabled() across its deep-link, bundle-load, and voip-init layers; a
        // headless library collapses those into this single startCall entry, mirroring the inline guard WA
        // keeps on the group send site (startGroupCall -> isGroupCallingEnabled()).
        if (gate != null && !gate.isWebCallingEnabled()) {
            throw new IllegalStateException("Cannot start a call: web calling is disabled for this account");
        }

        var callId = generateCallId();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting one-to-one call {0} to {1}, video={2}", callId, peer, video);
        var call = new Call(callId, peer, peer, self, true, false, video, CallState.RINGING);
        var orchestrated = new OrchestratedCall(call, true, peer, CallLifecycleState.NONE);
        orchestrated.peerDevices(devices);
        orchestrated.mediaStreams(streams);
        calls.put(callId, orchestrated);
        orchestrated.engineContext(registry.allocate(call, CallLifecycleState.NONE));

        orchestrated.lock().lock();
        try {
            var callKey = crypto.mintCallKey();
            orchestrated.callKey(callKey);

            var offer = buildOffer(callId, self, devices, callKey, video, offerIdentity());
            transition(orchestrated, CallLifecycleState.CALLING, CallEventType.CALL_OFFER_SENT);
            armTimer(callId, CallTimerKind.CALLER_LONELY);
            armTimer(callId, CallTimerKind.PERIODIC);
            armTimer(callId, CallTimerKind.HEARTBEAT);

            var ack = sendOffer(callId, self, peer, offer);
            applyOfferAck(orchestrated, ack);
            return call;
        } catch (WhatsAppCallException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "offer send failed for call " + callId, e);
            tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.CALL_OFFER_SEND_FAILED);
            throw e;
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Places an outbound group call into a group, fanning the offer out over the selective forwarding unit
     * and sharing the call key per participant.
     *
     * <p>Allocates the call and its {@link CallMembership} roster from {@code peers}, generates the call
     * identifier, and sends a group {@code <offer>} carrying the {@code <group_info>} roster of the self and
     * participant devices (a group offer ships NO call key in the offer itself). The offer rides the
     * synchronous ack seam, and the positive ack carries the selective forwarding unit's shared relay block,
     * which the controller records and reconciles its membership against and uses to bring up the media plane.
     * The controller then mints the 32 byte call key and fans it out as a per participant {@code <enc_rekey>},
     * one unicast stanza per connected participant device. The call enters {@link CallLifecycleState#CALLING}
     * for the offer leg, then {@link CallLifecycleState#ACCEPT_SENT} once the unit relay is known and the
     * media plane is brought up, and once the transport connects {@link #onMediaConnected(String)} settles it
     * in {@link CallLifecycleState#CONNECTED_LONELY}, the group lonely state, advancing to
     * {@link CallLifecycleState#CALL_ACTIVE} once a peer connects, driven by a later {@code <group_update>}
     * reconcile. This method returns with the offer acked and the media bring up started. The returned
     * {@link Call}'s {@link Call#chatJid()} is the group.
     *
     * @implNote This implementation ships a group offer carrying the device enumerated {@code <group_info>}
     * roster but no per device key fanout (a group offer ships no call key; the per participant key arrives
     * post join as {@code <enc_rekey>}); the ack relay is the shared SFU relay block with its {@code uuid},
     * {@code participant_uuid}, {@code key}, and {@code hbh_key}; the key share is the unicast
     * {@code <enc_rekey>} fanout of one {@code <enc>} per recipient stanza carrying a single 32 byte key; and
     * the {@link CallLifecycleState#CONNECTED_LONELY} versus {@link CallLifecycleState#CALL_ACTIVE} settle on
     * the media connected step is the active versus lonely decision by connected peer count. The selective
     * forwarding subscription publish (the sender and receive subscriptions embedded in STUN binding requests)
     * rides the media plane's transport rather than this signaling path, so it is reached through
     * {@link MediaPlane#bringUp(String, Stanza, List, byte[], boolean, boolean, int, CallMembership, MediaStreams, Jid, Optional)}
     * alongside the SRTP and SFrame key bring up.
     *
     * @param self      the local account's device JID, stamped as the call creator
     * @param peers     the user JIDs of every other group participant the offer rosters and the key is
     *                  fanned out to
     * @param groupJid  the group JID the call belongs to
     * @param video     whether the call is placed with video enabled
     * @param streams   the application capture sources and playback sinks the media plane drives, or
     *                  {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code self}, {@code peers}, {@code groupJid}, or {@code streams}
     *                                  is {@code null}, or {@code peers} contains a {@code null} element
     * @throws IllegalArgumentException if {@code peers} is empty, or the call's participant count (the peers
     *                                  plus the local user) exceeds the server group call participant ceiling
     * @throws IllegalStateException    if the dual call ceiling is already reached, or group calling is
     *                                  disabled for this account by the server feature gate
     * @throws WhatsAppCallException    if the offer could not be sent or no offer ack arrived
     */
    public Call startGroupCall(Jid self, Collection<Jid> peers, Jid groupJid, boolean video,
                               MediaStreams streams) {
        Objects.requireNonNull(peers, "peers cannot be null");
        var participantDevices = new LinkedHashMap<Jid, List<Jid>>();
        for (var peer : peers) {
            Objects.requireNonNull(peer, "peers cannot contain a null element");
            participantDevices.put(peer, List.of(peer));
        }
        return startGroupCall(self, participantDevices, groupJid, video, streams);
    }

    /**
     * Places an outbound group call whose participants are already resolved to their per user device JIDs.
     *
     * <p>This is the device resolved placement the call service drives after resolving each invited
     * participant to its LID and device set. The offer's {@code <group_info>} roster enumerates the local
     * (self) device and every participant device, so the server can fan the offer to the participants'
     * devices; the rest of the placement (call key mint, per participant {@code <enc_rekey>} fanout, media
     * bring up, timers) is identical to {@link #startGroupCall(Jid, Collection, Jid, boolean, MediaStreams)}.
     *
     * @param self               the local account's device JID, stamped as the call creator
     * @param participantDevices the invited participants keyed by user JID to their resolved device JIDs, in
     *                           invite order; must be non empty
     * @param groupJid           the group JID the call belongs to
     * @param video              whether the call is placed with video enabled
     * @param streams            the application capture sources and playback sinks the media plane drives, or
     *                           {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code self}, {@code participantDevices}, {@code groupJid}, or
     *                                  {@code streams} is {@code null}, or a device list is {@code null}
     * @throws IllegalArgumentException if {@code participantDevices} is empty, or the call's participant
     *                                  count exceeds the server group call participant ceiling
     * @throws IllegalStateException    if the dual call ceiling is already reached, or group calling is
     *                                  disabled for this account by the server feature gate
     * @throws WhatsAppCallException    if the offer could not be sent or no offer ack arrived
     */
    public Call startGroupCall(Jid self, Map<Jid, List<Jid>> participantDevices, Jid groupJid, boolean video,
                               MediaStreams streams) {
        Objects.requireNonNull(self, "self cannot be null");
        return placeGroupCallOffer(self, generateCallId(), self, participantDevices, groupJid, video, streams);
    }

    /**
     * Places a group {@code <offer>} for a group call under a supplied call identifier and creator, the body
     * of an outbound group placement.
     *
     * <p>This is the placement path {@link #startGroupCall(Jid, Map, Jid, boolean, MediaStreams)} drives
     * with a freshly generated {@code callId} and {@code creator} the local device: it validates the roster
     * against the server group calling gate and participant ceiling, allocates the orchestration handle and
     * its {@link CallMembership} roster, mints the end to end call key, ships the device enumerated group
     * offer to the call's {@code <callId>@call} target, records the caller relay from the offer ack, brings
     * the media plane up, and fans the call key out to the connected roster. The offer's {@code call-creator}
     * carries {@code creator} while the roster's self member and the key fanout carry {@code self}; for a
     * placement the two coincide. A call already tracked under {@code callId} is refused before any
     * allocation.
     *
     * <p>Joining an ongoing group call does NOT reach this path: a joiner has already received the call's
     * offer and answers it through {@link #joinGroupCall(String, boolean, MediaStreams)} (an empty
     * {@code <lobby>} then an {@code <accept>}), never sending a fresh offer.
     *
     * @implNote This implementation keeps the {@code creator} and {@code callId} parameters general so the
     * body reads as the reusable offer core; {@code startGroupCall} is its only caller.
     *
     * @param self               the local account's device JID, rostered as the self member and stamped on the
     *                           key fanout
     * @param callId             the call identifier, freshly generated for the placement
     * @param creator            the call creator's device JID stamped as the offer {@code call-creator} (the
     *                           local device for a placement)
     * @param participantDevices the participants keyed by user JID to their resolved device JIDs, in roster
     *                           order; must be non empty
     * @param groupJid           the group JID the call belongs to
     * @param video              whether the local side offers video
     * @param streams            the application capture sources and playback sinks the media plane drives, or
     *                           {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code self}, {@code callId}, {@code creator},
     *                                  {@code participantDevices}, {@code groupJid}, or {@code streams} is
     *                                  {@code null}, or a device list is {@code null}
     * @throws IllegalArgumentException if {@code participantDevices} is empty, or the call's participant count
     *                                  exceeds the server group call participant ceiling
     * @throws IllegalStateException    if group calling is disabled for this account by the server feature
     *                                  gate, or a call is already tracked under {@code callId}
     * @throws WhatsAppCallException    if the offer could not be sent or no offer ack arrived
     */
    private Call placeGroupCallOffer(Jid self, String callId, Jid creator, Map<Jid, List<Jid>> participantDevices,
                                     Jid groupJid, boolean video, MediaStreams streams) {
        Objects.requireNonNull(self, "self cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(creator, "creator cannot be null");
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(streams, "streams cannot be null");
        Objects.requireNonNull(participantDevices, "participantDevices cannot be null");
        var participantDeviceMap = new LinkedHashMap<Jid, List<Jid>>();
        participantDevices.forEach((user, devices) -> participantDeviceMap.put(
                Objects.requireNonNull(user, "participant user cannot be null"),
                List.copyOf(Objects.requireNonNull(devices, "participant device list cannot be null"))));
        var participants = List.copyOf(participantDeviceMap.keySet());
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("peers cannot be empty for an outbound group offer");
        }
        var gate = featureGate;
        if (gate != null) {
            // isGroupCallsEnabled is the conjunction isWebCallingEnabled() && isWebGroupCallingEnabled(), so
            // this single check enforces both the one to one calling master gate and the web group calling
            // master gate that must hold for a group call to start on web; the web group calling flag is
            // honoured here transitively rather than re tested separately.
            if (!gate.isGroupCallsEnabled()) {
                throw new IllegalStateException(
                        "Cannot start a group call: group calling is disabled for this account");
            }
            // The participant count is the peers plus the local user; the gate's ceiling caps total
            // membership, so reject before allocating when the requested roster would exceed it.
            var requested = participants.size() + 1;
            var max = gate.groupCallMaxParticipants();
            if (requested > max) {
                throw new IllegalArgumentException("Cannot start a group call with " + requested
                        + " participants: the server ceiling is " + max);
            }
        }
        if (calls.containsKey(callId)) {
            throw new IllegalStateException("Cannot place or join group call " + callId + ": it is already live");
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting group call {0} into {1}, participants={2}, video={3}",
                callId, groupJid, participants.size(), video);
        var call = new Call(callId, groupJid, groupJid, creator, true, true, video, CallState.CONNECTING);
        var orchestrated = new OrchestratedCall(call, true, null, CallLifecycleState.NONE);
        var membership = new CallMembership(callId);
        membership.selfUserJid(self);
        orchestrated.membership(membership);
        orchestrated.groupOutbound(new GroupCallOutbound(callId, self, membership, host));
        var roster = rosterOf(self, participantDeviceMap);
        membership.reconcile(roster);
        orchestrated.peerDevices(participants);
        orchestrated.mediaStreams(streams);
        calls.put(callId, orchestrated);
        orchestrated.engineContext(registry.allocate(call, CallLifecycleState.NONE));

        var groupTarget = Jid.of(callId + "@call");
        orchestrated.lock().lock();
        try {
            var callKey = crypto.mintCallKey();
            orchestrated.callKey(callKey);

            var offer = buildGroupOffer(callId, creator, roster, video, offerIdentity());
            transition(orchestrated, CallLifecycleState.CALLING, CallEventType.CALL_OFFER_SENT);
            armTimer(callId, CallTimerKind.CALLER_LONELY);
            armTimer(callId, CallTimerKind.PERIODIC);
            armTimer(callId, CallTimerKind.HEARTBEAT);

            var ack = sendOffer(callId, self, groupTarget, offer);
            applyOfferAck(orchestrated, ack);
            if (orchestrated.state() == CallLifecycleState.ENDING || orchestrated.state() == CallLifecycleState.NONE) {
                return call;
            }

            reconcileFromAck(orchestrated, ack);
            transition(orchestrated, CallLifecycleState.ACCEPT_SENT, CallEventType.CALL_ACCEPT_SENT);
            orchestrated.relay().ifPresent(relay ->
                    bringUpMediaPlane(orchestrated, relay, orchestrated.offerAckVoipSettings(), callKey, true, video));
            fanOutGroupRekey(orchestrated, self, callKey);
            armTimer(callId, CallTimerKind.UPDATE_ENCRYPTION_KEY);
            ensureControls(orchestrated);
            return call;
        } catch (WhatsAppCallException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "group offer send failed for call " + callId, e);
            tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.CALL_OFFER_SEND_FAILED);
            throw e;
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Accepts an inbound offer the local user answered, bringing up the call's media plane.
     *
     * <p>Validates that the call exists and is in an answerable state, brings up the media plane from the
     * relay block the offer or a later group update carried (deriving the per direction SRTP and the per
     * participant SFrame keys from the call key the offer delivered), sends the accept, transitions to
     * {@link CallLifecycleState#ACCEPT_SENT} with the result {@link CallResult#ACCEPTED}, and arms the
     * watchdog. A group accept addresses the call's MUC target rather than the creator device.
     *
     * @implNote This implementation enforces the state precondition (the call must not be ending or active
     * elsewhere), brings up media and transport, transitions to {@link CallLifecycleState#ACCEPT_SENT} with
     * the {@link CallResult#ACCEPTED} result, and sends the accept. The audio device, media, transport, and
     * BWE start is reached through
     * {@link MediaPlane#bringUp(String, Stanza, List, byte[], boolean, boolean, int, CallMembership, MediaStreams, Jid, Optional)}.
     *
     * @param callId  the identifier of the call being accepted
     * @param video   whether the local side answers with video enabled
     * @param streams the application capture sources and playback sinks the media plane drives, or
     *                {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code callId} or {@code streams} is {@code null}
     * @throws IllegalArgumentException if no call exists for {@code callId}
     * @throws IllegalStateException    if the call is not in an answerable state
     * @throws WhatsAppCallException    if the media plane cannot be brought up
     */
    public Call acceptCall(String callId, boolean video, MediaStreams streams) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(streams, "streams cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "accepting call {0}, video={1}", callId, video);
        var orchestrated = require(callId);
        orchestrated.lock().lock();
        try {
            var state = orchestrated.state();
            if (state == CallLifecycleState.ENDING || state == CallLifecycleState.CALL_ACTIVE_ELSEWHERE) {
                throw new IllegalStateException("Cannot accept call " + callId + " in state " + state);
            }
            orchestrated.mediaStreams(streams);
            var call = orchestrated.call();
            var relay = orchestrated.relay().orElse(null);
            // A group call carries no key in its offer (the shared call key travels through the enc_rekey
            // exchange), so the answering or joining device mints its own shared key here; every participant's
            // SFrame keys derive from it and the device JID. A one to one call already recovered its key from
            // the offer, so this is a no op for it.
            if (call.isGroup() && orchestrated.callKey().isEmpty()) {
                orchestrated.callKey(crypto.mintCallKey());
            }
            var callKey = orchestrated.callKey().orElse(null);
            // A one to one call brings its media plane up here from the offer relay and key. A group call
            // defers the bring up to the <group_update> that delivers the group relay (handleGroupUpdate);
            // group media comes up on the group_update relay, not the offer's.
            if (!call.isGroup() && relay != null && callKey != null) {
                var voipSettings = orchestrated.incomingOffer()
                        .map(OfferStanza::voipSettings)
                        .orElseGet(List::of);
                bringUpMediaPlane(orchestrated, relay, voipSettings, callKey, false, video);
            }

            var creator = call.creator();
            var to = call.isGroup() ? Jid.of(callId + "@call") : orchestrated.peerDeviceJid().orElse(call.peer());
            var offerVideo = orchestrated.incomingOffer().map(offer -> !offer.videoCodecs().isEmpty()).orElse(false);
            var accept = buildAccept(callId, creator, relay, offerVideo);
            // The accept is shipped fire and forget: unlike the offer's synchronous relay bearing ack, the
            // <ack class="call" type="accept"> is asynchronous and is consumed by handleIncomingAck, which
            // ends the call on an accept NACK (404 -> CallDoesNotExistForRejoin, 434 -> CallIsFull).
            host.sendSignaling(CallStanza.toCall(accept, to, callId));

            transition(orchestrated, CallLifecycleState.ACCEPT_SENT, CallEventType.CALL_ACCEPT_SENT);
            armTimer(callId, CallTimerKind.PERIODIC);
            ensureControls(orchestrated);
            return call;
        } catch (WhatsAppCallException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "accept send failed for call " + callId, e);
            tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.CALL_ACCEPT_SEND_FAILED);
            throw e;
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Sends an early ring acknowledgement for an inbound offer before the user answers.
     *
     * <p>A callee device emits a preaccept after the offer and before the user answers, so the caller
     * learns the device is alerting and can begin early media preparation. The preaccept echoes the call's
     * audio profile and capability; it carries no key fanout or transport block, which arrive with the
     * accept. A group preaccept addresses the call's MUC target. This method does not change the call
     * state; the call stays ringing until the user accepts or rejects.
     *
     * @implNote This implementation builds the {@code <preaccept>} with the callee audio capabilities and the
     * voip capability advertisement and ships it fire and forget. The group end to end key and video
     * capability a group preaccept additionally embeds are owned by the key distribution and capability units
     * rather than emitted here.
     *
     * @param callId the identifier of the inbound call to acknowledge
     * @throws NullPointerException     if {@code callId} is {@code null}
     * @throws IllegalArgumentException if no call exists for {@code callId}
     */
    public void preaccept(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var orchestrated = require(callId);
        orchestrated.lock().lock();
        try {
            var call = orchestrated.call();
            var creator = call.creator();
            var to = call.isGroup() ? Jid.of(callId + "@call") : orchestrated.peerDeviceJid().orElse(call.peer());
            var offerVideo = orchestrated.incomingOffer().map(offer -> !offer.videoCodecs().isEmpty()).orElse(false);
            var preaccept = buildPreaccept(callId, creator, offerVideo);
            host.sendSignaling(CallStanza.toCall(preaccept, to, callId));
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Declines an inbound offer before answering it.
     *
     * <p>Sends a reject for the call (or, when no call object exists for the identifier, a standalone
     * reject to the offer's creator), then tears the call down: the timers are cancelled, any media plane
     * is closed, the call leaves through {@link CallLifecycleState#ENDING}, and the reject and ending events
     * are emitted. The reject reason is the wire literal vocabulary shared with terminate.
     *
     * @implNote This implementation, when a context exists, sends a reject for it and transitions toward
     * {@link CallLifecycleState#ENDING}; when no orchestration handle exists it builds the peer JID from the
     * supplied creator and sends a standalone reject.
     *
     * @param callId      the identifier of the call to decline
     * @param callCreator the call creator's device JID, the reject recipient when no call object exists
     * @param reason      the decline reason
     * @throws NullPointerException if {@code callId}, {@code callCreator}, or {@code reason} is
     *                              {@code null}
     */
    public void rejectCall(String callId, Jid callCreator, CallEndReason reason) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            var reject = RejectStanza.of(callId, callCreator, reason);
            host.sendSignaling(CallStanza.toCall(reject, callCreator.toUserJid(), callId));
            return;
        }
        orchestrated.lock().lock();
        try {
            // Mark the call rejected so any inbound signaling that races the teardown is classified
            // IGNORE_REJECTED by the message router rather than acted on; once tearDown removes the handle
            // the router instead returns DROP for the now unknown call, so this guards only the race while
            // the rejected handle is still tracked.
            orchestrated.dedupState(orchestrated.dedupState().markRejected());
            var to = orchestrated.peerDeviceJid().orElse(callCreator.toUserJid());
            var reject = RejectStanza.of(callId, callCreator, reason);
            host.sendSignaling(CallStanza.toCall(reject, to, callId));
            tearDown(orchestrated, reason, CallEventType.CALL_REJECT_RECEIVED);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Joins an ongoing group call the local device was offered, by entering its lobby and answering.
     *
     * <p>Late joining an in progress group call is not a fresh offer. The local device has already received
     * the call's {@code <offer>} (which rang it and is tracked here), so joining it from the ongoing call
     * banner sends an empty {@code <lobby>} to the call's {@code <callId>@call} target and then the same
     * {@code <accept>} an answer sends, both stamped with the ongoing call's original {@code call-creator}. The
     * relay and per participant keys the media plane needs are delivered by the subsequent inbound
     * {@code <group_update>} (the relay) and {@code enc_rekey} exchange (the keys), not by this device's own
     * offer, so this method delegates the answer to {@link #acceptCall(String, boolean, MediaStreams)}
     * after the lobby and returns the live {@link Call} view.
     *
     * <p>This is distinct from both {@link #acceptCall(String, boolean, MediaStreams)} (which answers a
     * ringing offer directly, with no {@code <lobby>}) and the call link token join
     * ({@link #joinCallLink(Jid, String, CallLinkMedia, boolean, MediaStreams)}). The call must already
     * be tracked from its inbound offer; a call the local device was never offered cannot be joined this way.
     *
     * @implNote This implementation sends the banner join as an empty {@code <lobby call-id call-creator/>} to
     * {@code <callId>@call} followed by an {@code <accept>}; there is no self originated {@code <offer>} and no
     * {@code <link_join>}. The {@code <lobby>} is sent whether the ring was ignored (which itself sends a
     * {@code <reject>}) or timed out. The media relay arrives on the inbound {@code <group_update>} and the
     * media plane comes up from it (see {@link #handleGroupUpdate(GroupUpdateStanza)}), so the {@code <accept>}
     * the join sends does not itself connect media.
     *
     * @param callId  the identifier of the ongoing group call to join; must be a call this device was offered
     * @param video   whether the local user joins with video enabled
     * @param streams the application capture sources and playback sinks the media plane drives, or
     *                {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code callId} or {@code streams} is {@code null}
     * @throws IllegalArgumentException if no call is tracked for {@code callId} (the device was not offered it),
     *                                  or the tracked call is not a group call
     * @throws IllegalStateException    if the call is not in an answerable state
     * @throws WhatsAppCallException    if the media plane cannot be brought up
     */
    public Call joinGroupCall(String callId, boolean video, MediaStreams streams) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(streams, "streams cannot be null");
        var orchestrated = require(callId);
        if (!orchestrated.call().isGroup()) {
            throw new IllegalArgumentException("joinGroupCall is for group calls; " + callId + " is one-to-one");
        }
        orchestrated.lock().lock();
        try {
            var creator = orchestrated.call().creator();
            var lobby = new StanzaBuilder()
                    .description("lobby")
                    .attribute("call-id", callId)
                    .attribute("call-creator", creator)
                    .build();
            host.sendSignaling(wrapInCall(lobby, Jid.of(callId + "@call"), callId));
        } finally {
            orchestrated.lock().unlock();
        }
        return acceptCall(callId, video, streams);
    }

    /**
     * Joins a call through a call link token, running the query and join handshake and bringing up media.
     *
     * <p>Resolves the link token, requests admission, and answers the joined call. The call is allocated and
     * parked in {@link CallLifecycleState#LINK}; the per call {@link CallLinkController} runs the
     * {@code link_query} preview then the {@code link_join} request over the bound {@link CallLinkIqSender};
     * the join ack's lobby participant list, when present, is surfaced through the per call
     * {@link WaitingRoomController}; the join ack's {@code <relay>} subtree, when present, is recorded as the
     * call's relay; and the call is then answered through {@link #acceptCall(String, boolean, MediaStreams)},
     * which sends the accept and brings the media plane up once the relay and the call key are both known. A
     * group join carries no call key in the join ack (the per participant key arrives post join as an
     * {@code <enc_rekey>}), so the accept ships and the call sits in {@link CallLifecycleState#ACCEPT_SENT}
     * with the media plane brought up on the later rekey, mirroring an outbound group placement.
     *
     * <p>The whole path is gated on {@link CallsFeatureGate#isCallLinkEnabled()} when the feature gate is
     * bound: a build whose server flags disable call links refuses the join rather than running the
     * handshake, the same deny by throw the start, group start, and screen share entry points apply. The
     * call link join requires the {@link CallLinkIqSender} bound (the query and join are blocking round trips
     * with no fire and forget fallback), so a controller built without it refuses the join.
     *
     * @implNote This implementation parks the call in {@link CallLifecycleState#LINK}, runs the link query
     * and join handshake through the {@link CallLinkController}, then answers through
     * {@link #acceptCall(String, boolean, MediaStreams)}. The query is the {@link CallLinkQueryAction#PREVIEW}
     * lookup a joiner issues, and the join is a single {@code link_join} stanza carrying one
     * {@code join-state} attribute (default {@link #LINK_JOIN_STATE_DEFAULT}), never a two leg sequence. The
     * joiner registers under the all zeros placeholder call id, runs the {@code link_join} handshake, then
     * adopts the relay assigned identity the ack carries through {@link #adoptJoinAckIdentity}: the ack
     * supplies a required {@code call-id}, a required {@code call-creator}, and a required {@code <group_info>}
     * roster, and the resolved id is adopted only while the current id is still the placeholder. This method
     * re keys the {@code calls} registration and the engine context to the resolved id, swaps in a rebuilt
     * {@link Call} under the resolved id and creator, and builds the {@link CallMembership} (whose per device
     * SSRCs derive from the call id) under the resolved id, seeded from the ack's {@code <group_info>}.
     *
     * @param self    the local account's device JID, stamped as the joined call's creator placeholder (the
     *                join ack's real creator is not applied to the immutable {@link Call})
     * @param token   the call link token to join
     * @param media   the media kind the caller intends to use, carried on the link query and join
     * @param video   whether the local user joins with video enabled
     * @param streams the application capture sources and playback sinks the media plane drives, or
     *                {@link MediaStreams#none()} to fall back to platform devices
     * @return the live call view
     * @throws NullPointerException     if {@code self}, {@code token}, {@code media}, or {@code streams} is
     *                                  {@code null}
     * @throws IllegalStateException    if the dual call ceiling is already reached, if call links are
     *                                  disabled for this account by the server feature gate, or if the
     *                                  call link IQ sender is not wired
     * @throws WhatsAppCallException    if the media plane cannot be brought up
     */
    public Call joinCallLink(Jid self, String token, CallLinkMedia media, boolean video,
                             MediaStreams streams) {
        Objects.requireNonNull(self, "self cannot be null");
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(media, "media cannot be null");
        Objects.requireNonNull(streams, "streams cannot be null");
        var gate = featureGate;
        if (gate != null && !gate.isCallLinkEnabled()) {
            throw new IllegalStateException(
                    "Cannot join a call link: call links are disabled for this account");
        }
        var iqSender = callLinkIqSender;
        if (iqSender == null) {
            throw new IllegalStateException(
                    "Cannot join a call link: the call-link IQ sender is not wired");
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "joining call link token={0}, video={1}", new LogRedactable.Token(token), video);
        // The joiner mints its call object at preview under the all zeros sentinel call id, registers it,
        // runs the link_join handshake, then adopts the relay assigned id and creator from the join ack while
        // the id is still the placeholder. Register under the placeholder, transition to LINK, run the
        // handshake, then adopt the resolved identity in adoptJoinAckIdentity. The membership and outbound
        // group unit are built there, under the resolved id, because the membership's per device SSRCs derive
        // from the call id.
        var callId = PLACEHOLDER_CALL_ID;
        var call = new Call(callId, self, self, self, true, true, video, CallState.CONNECTING);
        var orchestrated = new OrchestratedCall(call, true, null, CallLifecycleState.NONE);
        orchestrated.mediaStreams(streams);
        calls.put(callId, orchestrated);
        orchestrated.engineContext(registry.allocate(call, CallLifecycleState.NONE));

        orchestrated.lock().lock();
        try {
            transition(orchestrated, CallLifecycleState.LINK, CallEventType.CALL_LINK_STATE_CHANGED);

            // The link sink reads the call's id at emit (orchestrated::callId), so the LINK phase events fire
            // under the placeholder and post adoption events under the relay assigned id, following the rekey.
            CallEventSink linkSink = eventBus == null ? event -> { } : new ControlEventBridge(orchestrated::callId, eventBus);
            var linkController = new CallLinkController(iqSender, linkSink);

            linkController.preview(token, media);
            // The join is a single link_join stanza carrying one join state attribute, not a two leg
            //  sequence. The join state value is 2 by default and 1 under a call state condition not modeled
            //  here.
            // TODO: confirm which previewCallLink boolean selects join state 1 versus the default 2. The
            //  selecting flag originates from a previewCallLink parameter byte with no symbol or string name,
            //  is distinct from the rejoin flag, and no captured link_join stanza exists to observe it, so the
            //  1 versus 2 selection is not yet recoverable. This is NOT a missing second leg.
            var joinAck = linkController.join(token, LINK_JOIN_STATE_DEFAULT);
            // Adopt the relay assigned call id and creator from the ack, re keying the placeholder
            // registration, and seed the membership (its per device SSRCs derive from the call id) from the
            // ack's <group_info>. The ack carries no relay; the joiner's relay arrives via a later
            // <group_update>.
            callId = adoptJoinAckIdentity(orchestrated, callId, joinAck, video, self);

            var context = CallControlContext.group(callId, orchestrated.call().creator(), self);
            var waitingRoomController = new WaitingRoomController(context, iqSender, linkSink);
            if (!joinAck.waitingRoomUsers().isEmpty()) {
                waitingRoomController.onWaitingRoomUpdate(joinAck.waitingRoomUsers());
            }

            acceptCall(callId, video, streams);
            installLinkControls(orchestrated, linkController, waitingRoomController);
            return orchestrated.call();
        } catch (RuntimeException e) {
            // A handshake failure (the link query or join IQ throwing) leaves the call allocated but not yet
            // answered, so tear it down here. A media bring up failure inside acceptCall has already torn the
            // call down and removed it from the registry, so the still tracked guard avoids a double teardown.
            if (calls.containsKey(callId)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "call link join failed for call " + callId, e);
                tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.CALL_IS_ENDING);
            }
            throw e;
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Mints a fresh shareable call link through the call link control plane.
     *
     * <p>Builds a transient {@link CallLinkController} over the bound call link IQ sender and runs its
     * {@link CallLinkController#create(CallLinkMedia, boolean)} to dispatch the {@code link_create} request
     * and parse the relay's reply, returning the minted link. This is a standalone control plane operation
     * that mints a link the host can share: it allocates no call and advances no call link join substate,
     * and it is gated on the same server call link feature flag and the same bound call link IQ sender as
     * {@link #joinCallLink(Jid, String, CallLinkMedia, boolean, MediaStreams) joining a link}.
     *
     * @implNote This implementation constructs the {@link CallLinkController} with a no op event sink because
     * the create is a blocking request reply whose result is delivered through the return value, and the call
     * link join sub state carries no create state, so the controller emits nothing. The feature gate and IQ
     * sender guards mirror {@link #joinCallLink}.
     *
     * @param media              the media kind the link is created with; never {@code null}
     * @param waitingRoomEnabled {@code true} to request the link's waiting room gate at creation time
     * @return the minted call link
     * @throws NullPointerException  if {@code media} is {@code null}
     * @throws IllegalStateException if call links are disabled for this account by the server feature gate,
     *                               or if the call link IQ sender is not wired
     */
    public CallLink createCallLink(CallLinkMedia media, boolean waitingRoomEnabled) {
        Objects.requireNonNull(media, "media cannot be null");
        var gate = featureGate;
        if (gate != null && !gate.isCallLinkEnabled()) {
            throw new IllegalStateException(
                    "Cannot create a call link: call links are disabled for this account");
        }
        var iqSender = callLinkIqSender;
        if (iqSender == null) {
            throw new IllegalStateException(
                    "Cannot create a call link: the call-link IQ sender is not wired");
        }
        var controller = new CallLinkController(iqSender, event -> { });
        return controller.create(media, waitingRoomEnabled).toCallLink();
    }

    /**
     * Adopts the relay assigned identity a call link join ack carries, re keying the call from its all zeros
     * placeholder registration to the resolved id.
     *
     * <p>Rebuilds the public {@link Call} under the resolved call id and the ack's creator (carrying the
     * live phase and mute flags forward), swaps it into the orchestration handle, moves the {@code calls}
     * registration from the placeholder id to the resolved id, and re homes the engine context under the
     * resolved id. It then builds the call's {@link CallMembership} under the resolved id, seeded from the
     * ack's {@code <group_info>} roster, and the outbound group unit against it. The membership is built
     * here rather than at the placeholder registration because its per device secure SSRCs derive from the
     * call id, so it must key on the resolved id.
     *
     * @implNote This implementation adopts the ack's identity only while the current id is still the all
     * zeros placeholder. Because the call state is composed rather than flat, it rebuilds the {@link Call},
     * {@link CallMembership}, and {@link GroupCallOutbound} under the resolved id and re homes the engine
     * context through the {@link CallContextRegistry} (release then allocate, since the registry keys by call
     * id and the LINK phase arms no timer state to carry over).
     *
     * @param orchestrated  the joined call's orchestration handle, registered under {@code placeholderId}
     * @param placeholderId the all zeros placeholder id the call is currently registered under
     * @param joinAck       the decoded join acknowledgement carrying the relay assigned identity
     * @param video         whether the local user joined with video enabled
     * @param self          the local account's device JID, the membership's self and the outbound unit's
     *                      creator
     * @return the resolved call id the call is now registered under
     */
    private String adoptJoinAckIdentity(OrchestratedCall orchestrated, String placeholderId,
                                        LinkJoinAck joinAck, boolean video, Jid self) {
        var resolvedId = joinAck.callId();
        var creator = joinAck.callCreator();
        var target = Jid.of(resolvedId + "@call");
        var placeholderCall = orchestrated.call();
        var resolvedCall = new Call(resolvedId, target, target, creator, true, true, video, placeholderCall.state());
        resolvedCall.setAudioMuted(placeholderCall.isAudioMuted());
        resolvedCall.setVideoMuted(placeholderCall.isVideoMuted());
        orchestrated.call(resolvedCall);
        calls.remove(placeholderId);
        calls.put(resolvedId, orchestrated);
        registry.release(placeholderId);
        orchestrated.engineContext(registry.allocate(resolvedCall, orchestrated.state()));
        var membership = new CallMembership(resolvedId);
        membership.selfUserJid(self);
        membership.reconcile(joinAck.groupInfo());
        orchestrated.membership(membership);
        orchestrated.groupOutbound(new GroupCallOutbound(resolvedId, self, membership, host));
        return resolvedId;
    }

    /**
     * Folds a call's call link and waiting room control units into its stored in call control bundle.
     *
     * <p>The call link join builds the {@link CallLinkController} and {@link WaitingRoomController} to run the
     * query and join handshake before the call is answered, then answers the call, which builds the base
     * {@link CallControls} bundle (the four signaling plane units, plus the app data units when the media
     * plane is up). This merges the two link units into that bundle so they are held for the call's lifetime,
     * reachable by the host side waiting room admit and deny, and dropped on teardown with the rest of the
     * bundle. When no base bundle was built (the event bus is unbound, so the call carries no in call control
     * units), the link units are dropped with the rest, since they have no holder; the link handshake they
     * performed has already completed by then. Must be called under the call's lock.
     *
     * @param orchestrated  the call whose link controls are folded in
     * @param linkController the call link control unit built for the join handshake
     * @param waitingRoom   the waiting room control unit built for the join handshake
     */
    private static void installLinkControls(OrchestratedCall orchestrated, CallLinkController linkController,
                                            WaitingRoomController waitingRoom) {
        orchestrated.controls()
                .map(base -> base.withCallLink(linkController, waitingRoom))
                .ifPresent(orchestrated::controls);
    }

    /**
     * Ends a live call, sending a terminate and tearing it down.
     *
     * <p>Sends a terminate carrying the end reason to the peer (fanned out to the call's peer devices for
     * an outbound call, or addressed to the group call target for a group call), then cancels the call's
     * timers, closes its media plane, leaves through {@link CallLifecycleState#ENDING} to
     * {@link CallLifecycleState#NONE}, and emits the ending event. Ending a call that does not exist is a no op.
     *
     * @implNote This implementation sends the terminate, then stops every per call timer, frees the
     * orchestration handle, and clears the slot. When both a primary and a secondary call are live the
     * secondary is ended before the primary; this method ends one call by identifier.
     *
     * @param callId the identifier of the call to end
     * @param reason the end reason carried on the terminate
     * @throws NullPointerException if {@code callId} or {@code reason} is {@code null}
     */
    public void endCall(String callId, CallEndReason reason) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            var call = orchestrated.call();
            var creator = call.creator();
            var destination = orchestrated.peerDevices();
            var to = call.isGroup()
                    ? Jid.of(callId + "@call")
                    : orchestrated.peerDeviceJid().orElse(call.peer());
            var terminate = TerminateStanza.of(callId, creator, reason, destination);
            host.sendSignaling(CallStanza.toCall(terminate, to, callId));
            tearDown(orchestrated, reason, CallEventType.CALL_IS_ENDING);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Sends a mid call group membership update adding or removing participants on an in progress group call.
     *
     * <p>Routes the update through the call's outbound group call unit, which builds the
     * {@code <group_update>} carrying a {@code <group_info>} roster of the affected participants, ships it
     * fire and forget to {@code target}, and reconciles the call's {@link CallMembership} against the add or
     * remove. A call that is not tracked, or one that is not a group call (and so has no outbound group call
     * unit), is a no op.
     *
     * @implNote This implementation drives the unit that owns the outbound group membership path
     * ({@link GroupCallOutbound#sendGroupParticipants(Jid, Jid, List, boolean)}), so the membership update is
     * built and the call's roster reconciled in one place rather than inline in the call service. The unit
     * also owns the per peer offer send timestamps and the unanswered offer sweep, so a participant added
     * here becomes a sweep candidate once the controller's rekey path fans the key to it.
     *
     * @param callId       the group call identifier
     * @param target       the group call target JID the update is addressed to
     * @param creator      the call-creator JID stamped on the update
     * @param participants the participants to add or remove; must be non empty
     * @param added        {@code true} to add the participants, {@code false} to remove them
     * @throws NullPointerException     if {@code callId}, {@code target}, {@code creator}, or
     *                                  {@code participants} is {@code null}
     * @throws IllegalArgumentException if {@code participants} is empty
     */
    public void sendGroupParticipants(String callId, Jid target, Jid creator, List<Jid> participants,
                                      boolean added) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(creator, "creator cannot be null");
        Objects.requireNonNull(participants, "participants cannot be null");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants cannot be empty");
        }
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            orchestrated.groupOutbound()
                    .ifPresent(unit -> unit.sendGroupParticipants(target, creator, participants, added));
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Returns the outbound group call unit for a tracked group call, for the watchdog's unanswered offer
     * sweep.
     *
     * <p>The per call timer scheduler resolves the unit by call id on each watchdog tick and drives its
     * {@link GroupCallOutbound#sweepUnansweredOffers() sweep}; a call the controller does not track, or a
     * one to one call with no outbound group call unit, yields an empty result so the watchdog sweeps
     * nothing. The unit is resolved fresh per tick rather than captured at arm time, so a call torn down
     * between ticks is no longer swept.
     *
     * @param callId the call identifier
     * @return an {@link Optional} holding the call's outbound group call unit, or empty when none exists
     */
    Optional<GroupCallOutbound> groupOutbound(String callId) {
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return Optional.empty();
        }
        return orchestrated.groupOutbound();
    }

    /**
     * Processes an inbound offer, ringing a new call and recovering its call key.
     *
     * <p>Allocates the call from the offer, recovers the end to end call key from the offer's per device
     * {@code <enc>} fanout (a one to one offer) or leaves it for a later group rekey, records the offer's
     * relay block, and rings: the call enters {@link CallLifecycleState#RECEIVED_CALL}, or
     * {@link CallLifecycleState#RECEIVED_CALL_WITHOUT_OFFER} when the offer carries no media descriptor. A group
     * offer additionally allocates the call's {@link CallMembership} and reconciles it against the offer's
     * {@code <group_info>} roster, so a subsequent inbound {@code <group_update>} reconciles against an
     * already populated membership rather than being dropped. The returned {@link IncomingCall} is the
     * listener facing offer the application accepts or rejects. An offer for a call that already exists is
     * dropped and reported as an empty result; an offer arriving while the dual call ceiling is reached is
     * buffered into the busy or lobby pending call holder when its {@code <voip_settings>} enable the
     * pending call path, and dropped otherwise, both reported as an empty result.
     *
     * @implNote This implementation allocates the orchestration handle from the offer, Signal decrypts the
     * call key through {@link CallKeyExchange#decryptCallKey(CallKeyDistribution, Jid)} (the sender of the
     * envelope is the offer's authoring device, supplied as {@code senderJid}), records the relay block,
     * rings, and sets the state to {@link CallLifecycleState#RECEIVED_CALL} or
     * {@link CallLifecycleState#RECEIVED_CALL_WITHOUT_OFFER} by whether the offer carried a media descriptor. A
     * group offer's {@code <group_info>} establishes the call's membership up front through
     * {@link CallMembership#reconcile(GroupInfoStanza)}, seeding the participant set from the offer roster
     * before the first {@code <group_update>}. When the ceiling is reached this method takes the busy offer
     * branch: gated on the offer's {@code enable_pending_call} {@code <voip_settings>} leaf, it stores the
     * offer in the {@link PendingCall} holder instead of dropping it, so the router can queue the call's later
     * signaling.
     *
     * @param offer     the decoded inbound offer
     * @param senderJid the device JID that authored the offer envelope, used as the call key decryption
     *                  sender
     * @return an {@link Optional} holding the listener facing incoming call, or empty when the offer was
     *         dropped
     * @throws NullPointerException if {@code offer} or {@code senderJid} is {@code null}
     */
    public Optional<IncomingCall> handleIncomingOffer(OfferStanza offer, Jid senderJid) {
        Objects.requireNonNull(offer, "offer cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        var callId = offer.callId().orElseThrow();
        if (calls.containsKey(callId)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping duplicate offer for already-tracked call {0}", callId);
            return Optional.empty();
        }

        var creator = offer.callCreator().orElseThrow();
        var group = offer.isGroup();
        var chatJid = offer.groupJidValue().orElse(creator.toUserJid());
        var call = new Call(callId, creator.toUserJid(), chatJid, creator, false, group, offer.isVideo(),
                CallState.RINGING);
        var orchestrated = new OrchestratedCall(call, false, senderJid, CallLifecycleState.NONE);
        orchestrated.incomingOffer(offer);
        offer.relayNode().ifPresent(orchestrated::relay);
        if (group) {
            attachOfferMembership(orchestrated, offer);
        }
        calls.put(callId, orchestrated);
        orchestrated.engineContext(registry.allocate(call, CallLifecycleState.NONE));

        orchestrated.lock().lock();
        try {
            recoverOfferCallKey(orchestrated, offer, senderJid);
            var ringingState = offer.mediaDescriptor().isPresent()
                    ? CallLifecycleState.RECEIVED_CALL
                    : CallLifecycleState.RECEIVED_CALL_WITHOUT_OFFER;
            transition(orchestrated, ringingState, CallEventType.CALL_OFFER_RECEIVED);

            // Begin the relay latency exchange as soon as the offer's relay block is known so the peer's
            // report has arrived before this side answers and brings up its media plane, letting the election
            // converge both ends onto a shared relay rather than each picking its locally fastest one.
            startRelayLatencyExchange(orchestrated, orchestrated.relay().orElse(null));

            // The listener facing call carries the caller's phone number (the server stamps caller_pn on
            // the delivered offer), not the LID the protocol addresses, so it matches the JID applications
            // hold for contacts; the internal Call keeps the LID creator for reply addressing.
            var callerForApp = offer.callerPnValue().map(Jid::toUserJid).orElse(creator.toUserJid());
            var chatForApp = group ? chatJid : callerForApp;
            var incoming = new IncomingCall(callId, callerForApp, chatForApp, Instant.now(),
                    offer.isVideo(), group, offer.groupJidValue().orElse(null), false);
            return Optional.of(incoming);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Returns whether an inbound offer's {@code <voip_settings>} enable the busy or lobby pending call path.
     *
     * <p>Parses the offer's {@code <voip_settings>} bundles and reports whether any carries
     * {@link VoipParamKey#OPTIONS_ENABLE_PENDING_CALL options.enable_pending_call} set. The flag is a
     * per call server pushed parameter defaulting to {@code 0} (disabled), so an offer that carries no such
     * leaf yields {@code false} and the busy offer is dropped rather than buffered.
     *
     * @implNote This implementation reads the {@code options.enable_pending_call} leaf of the per call
     * {@code <voip_settings>} document, parsed through
     * {@link VoipSettings#of(Stanza, VoipParamJsonDeserializer)} the same way the media plane materialises
     * the negotiated bundle, and gates the pending call path on it.
     *
     * @param offer the decoded inbound offer
     * @return {@code true} when the pending call path is enabled for the offer, {@code false} otherwise
     */
    private boolean isPendingCallEnabled(OfferStanza offer) {
        var deserializer = new VoipParamJsonDeserializer();
        for (var node : offer.voipSettings()) {
            var enabled = VoipSettings.of(node, deserializer).params()
                    .getBoolean(VoipParamKey.OPTIONS_ENABLE_PENDING_CALL);
            if (enabled.isPresent() && enabled.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Buffers a busy or lobby offer into the pending call holder for join time replay.
     *
     * <p>Retains the offer as a new {@link PendingCall} unless the holder already buffers a still
     * {@link PendingCall.State#PENDING pending} call with the same identifier, in which case the offer
     * is a re ring of the already buffered call and the existing buffer is kept so its queued signaling is
     * not discarded. The holder is a single reference because one pending call context is buffered at a time.
     *
     * @implNote This implementation fills the pending context with the offer once the
     * {@code enable_pending_call} gate has passed. A re ring is detected by call identifier so the queued
     * messages survive the re ring rather than being discarded by reallocating the buffer.
     *
     * @param offer the decoded inbound offer to buffer
     */
    private void bufferPendingOffer(OfferStanza offer) {
        var callId = offer.callId().orElseThrow();
        var existing = pendingCall;
        if (existing != null && existing.state() == PendingCall.State.PENDING
                && existing.callId().equals(callId)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "re-ring of buffered pending-call offer {0}: keeping existing buffer", callId);
            return;
        }
        pendingCall = new PendingCall(offer);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                "buffering pending-call offer for {0}: dual-call ceiling reached, enable_pending_call set",
                callId);
    }

    /**
     * Returns whether a call identifier names the buffered busy or lobby pending call awaiting join.
     *
     * <p>Reports {@code true} only while the holder buffers a {@link PendingCall.State#PENDING pending}
     * call whose identifier matches, so the inbound router consults it before dropping a message that is not
     * an offer for a call not in the active calls map. A rejected or absent pending call yields {@code false}.
     *
     * @param callId the call identifier from an inbound message
     * @return {@code true} when the identifier names the buffered pending call, {@code false} otherwise
     */
    private boolean isBufferedPendingCall(String callId) {
        var pending = pendingCall;
        return pending != null && pending.state() == PendingCall.State.PENDING
                && pending.callId().equals(callId);
    }

    /**
     * Appends a router classified pending call signaling message to the buffered pending call's queue.
     *
     * <p>Buffers the message onto the holder for join time replay; a message that arrives after the pending
     * call has been rejected, or after the holder has been cleared by a race, is dropped, mirroring the
     * queue's own terminal state guard.
     *
     * @param message the router classified inbound message to buffer
     * @param callId  the message's call identifier, for the diagnostic log
     */
    private void bufferPendingMessage(CallMessage message, String callId) {
        var pending = pendingCall;
        if (pending != null && pending.buffer(message)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "buffered pending-call signaling {0} for {1}", message.type(), callId);
        }
    }

    /**
     * Classifies one decoded inbound signaling action through the message router, then dispatches it to its
     * lifecycle handler on a routable verdict.
     *
     * <p>This is the inbound seam the signaling receiver forwards every routable {@link CallMessage} to,
     * after the receiver has validated the envelope header, classified the raw envelope, and emitted its
     * acknowledgement. The controller first runs the finer per message {@link IncomingMessageRouter}
     * classification against live call state, threading the call's per call deduplication state in and out,
     * then acts on the {@link IncomingMessageRouter.RoutingClass} verdict:
     * <ul>
     *   <li>{@link IncomingMessageRouter.RoutingClass#PROCESS PROCESS} dispatches on the message type
     *       to the matching phase transition (an offer rings a new call, a preaccept marks the peer
     *       alerting, an accept starts the local bring up, a reject or terminate ends the call, a transport
     *       message advances the media plane);</li>
     *   <li>{@link IncomingMessageRouter.RoutingClass#OFFER_RERING OFFER_RERING} routes the re ringing
     *       offer back through {@link #handleIncomingOffer(OfferStanza, Jid)}, which detects the already
     *       tracked call and drops the duplicate offer body (the offer acknowledgement the re ring needs was
     *       already re sent by the receiver before this dispatch);</li>
     *   <li>{@link IncomingMessageRouter.RoutingClass#ACCEPT_HANDLE ACCEPT_HANDLE} routes the accept onto
     *       the accept received bring up path;</li>
     *   <li>{@link IncomingMessageRouter.RoutingClass#BUFFER_PENDING BUFFER_PENDING} appends the
     *       message to the buffered busy or lobby pending call's queue for join time replay rather than
     *       dropping it as unknown;</li>
     *   <li>{@link IncomingMessageRouter.RoutingClass#IGNORE IGNORE},
     *       {@link IncomingMessageRouter.RoutingClass#IGNORE_REJECTED IGNORE_REJECTED}, and
     *       {@link IncomingMessageRouter.RoutingClass#DROP DROP} drop the message without dispatch
     *       (a duplicate or stale transaction id, signaling for a call the local user already rejected, and a
     *       message naming no resolvable call respectively).</li>
     * </ul>
     * A dispatched message advances the call's recorded transaction id once the call exists. A message type
     * the controller does not act on at this layer is logged and ignored.
     *
     * <p>The boolean return reports only whether an inbound {@link TerminateStanza} was effected, that is,
     * whether {@link #handlePeerTerminate(TerminateStanza, Jid)} tore the call down rather than suppressing
     * the teardown behind a guard. It is {@code true} only for a terminate the dispatch acted on, and
     * {@code false} for every other message type, for a terminate the router dropped as a duplicate or stale
     * transaction, and for a terminate a guard ignored. The call service reads it to fire the host
     * {@code onCallEnded} fan out only for an effected terminate, since the terminate handler is the point the
     * end of call host event fires from, and only after the ignore guards pass.
     *
     * @implNote This implementation runs the router through {@link IncomingMessageRouter} before the per type
     * dispatch: the router validates LID addressing, deduplicates by
     * {@code (type, call-id, transaction-id)}, detects an offer re ring, and suppresses signaling for a
     * rejected call, returning one of the six routing classes the dispatch branches on. The authoring device
     * {@code senderJid} (the {@code <call>} envelope {@code from}) is passed as the router's LID addressing
     * signal, matching the controller's use of it as the peer signaling device, so a message whose authoring
     * device or call creator is LID addressed clears the router's LID gate. The PROCESS branch dispatches
     * {@link OfferStanza} to {@link #handleIncomingOffer(OfferStanza, Jid)}, {@link PreacceptStanza} to the
     * {@link CallLifecycleState#PREACCEPT_RECEIVED} transition, {@link AcceptStanza} to the accept received
     * bring up, {@link RejectStanza} and {@link TerminateStanza} to the terminate handling and teardown,
     * {@link TransportStanza} to the transport advance, and {@link GroupUpdateStanza} to the membership
     * reconcile and active versus lonely re decision. The remaining in call action types (mute, video state,
     * screen share, reactions, standalone rekey) are dispatched to the control units the controller delegates
     * to and are not all handled inline here. Dispatch is gated on the router verdict, the dedup state is
     * threaded around it, and for a message that resolves an existing call the router's resolved context is
     * threaded into the direct per type handlers under the call's held lock so they no longer re resolve it.
     *
     * @param message   the decoded inbound action
     * @param senderJid the device JID that authored the message envelope, used as the decryption sender,
     *                  the peer signaling device, and the router's LID addressing signal
     * @return {@code true} when the message was an inbound terminate the dispatch tore the call down for;
     *         {@code false} for every other message type, a router dropped terminate, or a terminate a guard
     *         ignored
     * @throws NullPointerException if {@code message} or {@code senderJid} is {@code null}
     */
    public boolean handleIncomingMessage(CallMessage message, Jid senderJid) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        var callId = message.callId().orElse(null);
        var resolved = callId == null ? null : calls.get(callId);
        var dedup = resolved == null
                ? IncomingMessageRouter.DedupState.INITIAL
                : resolved.dedupState();
        var verdict = incomingRouter.route(message, senderJid, dedup,
                id -> Objects.equals(id, callId) ? resolved : calls.get(id), this::isBufferedPendingCall);
        switch (verdict.routingClass()) {
            case PROCESS, OFFER_RERING, ACCEPT_HANDLE -> {
                var orchestrated = verdict.context().orElse(null);
                if (orchestrated == null) {
                    // A fresh offer that resolved no context: handleIncomingOffer creates and locks the call
                    // itself, so there is no existing call to hold a lock on across this dispatch.
                    var terminateEffected = dispatchInbound(message, senderJid, null);
                    advanceDedup(message, callId);
                    return terminateEffected;
                }
                orchestrated.lock().lock();
                try {
                    // tearDown() removes the call from the map under this same per-call lock, so a call no
                    // longer mapped once the lock is held was torn down while this thread waited for it: drop
                    // the message rather than dispatch it against a dead call.
                    if (calls.get(callId) != orchestrated) {
                        return false;
                    }
                    var terminateEffected = dispatchInbound(message, senderJid, orchestrated);
                    advanceDedup(message, callId);
                    return terminateEffected;
                } finally {
                    orchestrated.lock().unlock();
                }
            }
            case BUFFER_PENDING -> bufferPendingMessage(message, callId);
            case IGNORE, IGNORE_REJECTED, DROP -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "router verdict {0} drops inbound call message {1} for call {2}",
                        verdict.routingClass(), message.type(), callId);
            }
        }
        return false;
    }

    /**
     * Dispatches a router admitted inbound message to its per type lifecycle handler.
     *
     * <p>Switches the decoded {@link CallMessage} onto the handler that owns its phase transition. A message
     * type the controller does not act on at this layer is logged and ignored.
     *
     * <p>The boolean return is meaningful only for a {@link TerminateStanza}: it forwards whether
     * {@link #handlePeerTerminate(TerminateStanza, Jid)} tore the call down rather than suppressing the
     * teardown behind a guard. Every other message type returns {@code false}, since only a terminate teardown
     * drives the host end of call notification.
     *
     * @param message      the router admitted inbound action
     * @param senderJid    the device JID that authored the message envelope
     * @param orchestrated the resolved call context the message is dispatched against, whose per-call lock
     *                     the caller already holds, or {@code null} for a fresh offer that creates its own
     *                     call context
     * @return {@code true} when the message was a terminate that tore the call down; {@code false} otherwise
     */
    private boolean dispatchInbound(CallMessage message, Jid senderJid, OrchestratedCall orchestrated) {
        switch (message) {
            case OfferStanza offer -> handleIncomingOffer(offer, senderJid);
            case PreacceptStanza preaccept -> handlePeerPreaccept(preaccept, orchestrated);
            case AcceptStanza accept -> handlePeerAccept(accept, senderJid, orchestrated);
            case RejectStanza reject -> handlePeerReject(reject, senderJid, orchestrated);
            case TerminateStanza terminate -> {
                return handlePeerTerminate(terminate, senderJid, orchestrated);
            }
            case TransportStanza transport -> handlePeerTransport(transport, orchestrated);
            case RelayLatencyStanza relayLatency -> handlePeerRelayLatency(relayLatency, senderJid, orchestrated);
            case GroupUpdateStanza groupUpdate -> handleGroupUpdate(groupUpdate, orchestrated);
            case MuteV2Stanza mute -> handlePeerMute(mute, senderJid);
            case VideoStateStanza videoState -> handlePeerVideoState(videoState, senderJid);
            case ScreenShareStanza screenShare -> handlePeerScreenShare(screenShare, senderJid);
            case RaiseHandStanza raiseHand -> handlePeerRaiseHand(raiseHand, senderJid);
            case RekeyStanza rekey -> {
                // A peer's <enc_rekey> carries that participant's own 32 byte E2E key: each participant fans
                // its own distinct key, not one shared key. These per participant keys are derived and rotated,
                // but on the SFU relayed media path they are NOT consumed: the per frame SFrame transform is
                // never engaged in SFU group mode (the media rides the relay hop by hop SRTP, and
                // LiveMediaSession.openSframe passes the body through). So an inbound peer key is acknowledged
                // but not installed into per participant crypto here, because no relayed frame reads it; the
                // relay SRTP the group_update brought up carries the media. Engaging the SFrame transform (a
                // mesh, non SFU topology) would additionally install each peer key through a per participant
                // CallMembership key seam, which the media plane does not consume today.
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "inbound enc_rekey for call {0} acknowledged (per-participant E2E key is not consumed on "
                                + "the SFU relayed media path)", rekey.callId().orElseThrow());
            }
            default -> {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "no inline lifecycle handling for call message {0}", message.type());
            }
        }
        return false;
    }

    /**
     * Handles an inbound asynchronous call ack: the server confirmed or rejected a fire and forget leg.
     *
     * <p>The offer ack is the offer send's synchronous return value, so the only ack that reaches here is
     * the accept's: shipped fire and forget, it arrives later as a top level {@code <ack class="call"
     * type="accept">}. A positive ack needs no action, because the answered call is already brought up and
     * advancing. A NACK ends the answered call: the server {@code error} is mapped onto a
     * {@link CallResult}, that result's {@link CallResult#toEndReason() end reason} ends the
     * call, and {@link CallEventType#HANDLE_ACCEPT_ACK_FAILED} is fired. An ack for an untracked call is
     * ignored.
     *
     * @implNote This implementation maps server error {@code 404} to
     * {@link CallResult#CALL_DOES_NOT_EXIST_FOR_REJOIN} and {@code 434} to {@link CallResult#CALL_IS_FULL}
     * (any other error sets no result code but still ends the call), then ends the call through
     * {@link #tearDown(OrchestratedCall, CallEndReason, CallEventType)} firing
     * {@link CallEventType#HANDLE_ACCEPT_ACK_FAILED}. The mapped result is not written onto the engine context
     * here, since the controller never mutates the context (the state units own it) and the info manager
     * derives the snapshot result from the dispatched event; the result instead drives the public end reason
     * and the diagnostic log.
     *
     * @param outcome the decoded inbound call ack
     * @throws NullPointerException if {@code outcome} is {@code null}
     */
    public void handleIncomingAck(CallAckOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome cannot be null");
        if (outcome.isAck()) {
            return;
        }
        var orchestrated = calls.get(outcome.id());
        if (orchestrated == null) {
            return;
        }
        var error = outcome.error().getAsInt();
        orchestrated.lock().lock();
        try {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                    "accept ack error code {0}, treating as accept nack for call {1}", error, outcome.id());
            var reason = CallResult.fromAcceptAckError(error)
                    .map(CallResult::toEndReason)
                    .orElse(CallEndReason.SETUP_FAILED);
            tearDown(orchestrated, reason, CallEventType.HANDLE_ACCEPT_ACK_FAILED);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Sends one call liveness heartbeat for the call.
     *
     * <p>The {@link CallTimerKind#HEARTBEAT} timer invokes this on its cadence. A heartbeat is emitted while
     * the call is active ({@link CallLifecycleState#CALL_ACTIVE} or
     * {@link CallLifecycleState#CONNECTED_LONELY}), for both one to one and group calls, so a tick that fires
     * during setup or after teardown sends nothing. The action is a content free
     * {@code <heartbeat call-id call-creator/>} addressed to the call's {@code <callId>@call} target and
     * written fire and forget, like the in call control actions; it takes no call lock, since the timer
     * driver runs the tick off the lock.
     *
     * @implNote A one to one call heartbeats too, not only a group call: a connected one to one peer tears
     * the call down with {@code reason=timeout} when the periodic heartbeat stops, so a heartbeat is sent for
     * every call, not only groups.
     *
     * @param callId the identifier of the call to heartbeat; never {@code null}
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void sendHeartbeat(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (host == null) {
            return;
        }
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        var call = orchestrated.call();
        var state = orchestrated.state();
        if (state != CallLifecycleState.CALL_ACTIVE && state != CallLifecycleState.CONNECTED_LONELY) {
            return;
        }
        host.sendSignaling(CallStanza.toCall(
                new HeartbeatStanza(callId, call.creator()), Jid.of(callId + "@call"), callId));
    }

    /**
     * Advances the recorded inbound transaction id for a call after a routed message is dispatched.
     *
     * <p>Reads the dispatched message's {@code transaction-id} attribute and folds it into the call's
     * {@linkplain OrchestratedCall#dedupState() deduplication state} through
     * {@link IncomingMessageRouter.DedupState#withTransactionId(int)}, so a later replay of the same
     * or an older transaction id is classified as a duplicate. A message that carries no transaction id, or
     * one whose call the dispatch did not create or that has since been torn down, advances nothing.
     *
     * @implNote This implementation advances the dedup state only after the per type handler has run, so an
     * offer that created the call (the router returns its verdict with no resolved context for a fresh offer)
     * is found here by its now tracked handle; the {@link IncomingMessageRouter.DedupState#withTransactionId(int)}
     * step keeps the newest transaction id and leaves a stale or absent one unchanged, so the call's seen id
     * only advances.
     *
     * @param message the dispatched inbound action
     * @param callId  the message's call id, or {@code null} when it carried none
     */
    private void advanceDedup(CallMessage message, String callId) {
        if (callId == null) {
            return;
        }
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        var transactionId = message.toStanza().getAttributeAsInt("transaction-id", -1);
        if (transactionId < 0) {
            return;
        }
        orchestrated.lock().lock();
        try {
            orchestrated.dedupState(orchestrated.dedupState().withTransactionId(transactionId));
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Marks a call active once its media plane carries traffic in both directions.
     *
     * <p>The media plane and transport unit calls this when the call's transport reaches the bidirectional
     * traffic state: the controller moves the call from its accept leg
     * ({@link CallLifecycleState#ACCEPT_SENT} or {@link CallLifecycleState#ACCEPT_RECEIVED}) to
     * {@link CallLifecycleState#CALL_ACTIVE}, and the transition guard captures the active duration start and
     * cancels the connected lonely timer. A one to one call goes active when peer media flows; a group call
     * goes active when at least one peer connects, otherwise it sits in
     * {@link CallLifecycleState#CONNECTED_LONELY}. A signal for an untracked call is ignored.
     *
     * @implNote This implementation transitions to {@link CallLifecycleState#CALL_ACTIVE} once transport and
     * media connect with peer media present, and once a group peer connects. The transport state source is
     * the traffic started notification, surfaced here as a call from the media plane unit.
     *
     * @param callId the identifier of the call whose media plane became bidirectional
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void onMediaConnected(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            var state = orchestrated.state();
            if (state != CallLifecycleState.ACCEPT_SENT && state != CallLifecycleState.ACCEPT_RECEIVED
                    && state != CallLifecycleState.CONNECTED_LONELY && state != CallLifecycleState.REJOINING) {
                return;
            }
            var target = mediaConnectedTarget(orchestrated);
            transition(orchestrated, target, CallEventType.CALL_STATE_CHANGED);
            ensureControls(orchestrated);
            announceInitialVideoState(orchestrated);
            if (target == CallLifecycleState.CALL_ACTIVE) {
                notifyConnected(callId);
            }
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Reacts to a local platform camera capture being revoked by the operating system mid call by pausing
     * the local video.
     *
     * <p>Bound by the assembler to the media plane's capture interrupted sink and invoked with the call's
     * identifier when that call's manager camera capture driver enters
     * {@link com.github.auties00.cobalt.calls.platform.video.VideoCaptureDriver.State#INTERRUPTED}. Drives
     * the call's {@link VideoStateController#pause()} so the local video stream transitions to
     * {@link VideoStreamState#PAUSED}, which broadcasts the paused state to the peer and emits the local
     * change; a paused stream can be resumed without a fresh upgrade negotiation once the device returns. A
     * signal for an untracked call, or one whose control units are not built, is a no op.
     *
     * @param callId the identifier of the call whose local camera capture was revoked
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void onLocalCaptureInterrupted(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        withControls(callId, controls -> controls.video().pause());
    }

    /**
     * Reports a call's identifier to the bound service connected sink once its media plane goes active.
     *
     * <p>Fires the {@linkplain #bindConnectedSink(Consumer) bound} sink so the call service stamps the call's
     * telemetry accumulator with its connected instant. A controller whose sink is unbound runs the no op
     * default sink, so an unbound build stamps no connected instant. The stamp the sink applies is
     * idempotent, so a reconnect that re enters {@link CallLifecycleState#CALL_ACTIVE} through
     * {@link #onMediaConnected(String)} does not move the connected timestamp. Any failure the sink raises is
     * caught and logged so the media connected transition that called this always completes.
     *
     * @param callId the identifier of the call whose media plane went active
     */
    private void notifyConnected(String callId) {
        try {
            observer.onConnected(callId);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "connected sink failed for call " + callId, e);
        }
    }

    /**
     * Returns the in call state a call enters when its media plane becomes bidirectional.
     *
     * <p>A one to one call goes straight to {@link CallLifecycleState#CALL_ACTIVE} once peer media flows. A
     * group call goes to {@link CallLifecycleState#CALL_ACTIVE} only when at least one other participant is
     * connected; with no connected peer it settles in {@link CallLifecycleState#CONNECTED_LONELY}, the group
     * lonely state, until a peer joins.
     *
     * @implNote This implementation applies the active versus lonely decision for the media connected step: a
     * group call's connected peer presence selects the target, while a one to one call has no roster and is
     * always active on connect. The connected peer test reads the call's
     * {@link CallMembership#participantProvider() participant provider}
     * ({@link com.github.auties00.cobalt.calls.engine.participant.ParticipantProvider#firstConnectedPeer()}),
     * whose per slot {@link CallParticipant} aggregates
     * carry the membership state projected from each roster entry's {@code "connected"} literal.
     *
     * @param orchestrated the call whose media plane connected
     * @return {@link CallLifecycleState#CALL_ACTIVE}, or {@link CallLifecycleState#CONNECTED_LONELY} for a group
     *         call with no connected peer
     */
    private static CallLifecycleState mediaConnectedTarget(OrchestratedCall orchestrated) {
        if (!orchestrated.call().isGroup()) {
            return CallLifecycleState.CALL_ACTIVE;
        }
        var anyPeerConnected = orchestrated.membership()
                .map(membership -> membership.participantProvider().firstConnectedPeer().isPresent())
                .orElse(false);
        return anyPeerConnected ? CallLifecycleState.CALL_ACTIVE : CallLifecycleState.CONNECTED_LONELY;
    }

    /**
     * Announces the local camera on state once a video call's media plane connects.
     *
     * <p>A call placed or answered with video carries the camera on from the start, yet the peer renders
     * the local video only after it receives the camera on {@code <video>} state announcement. This
     * broadcasts {@link VideoStreamState#ENABLED} through the call's {@link VideoStateController} the first
     * time the media plane connects, so the peer learns the camera is live and renders the inbound video;
     * an audio only call announces nothing, and a reconnect that finds the camera already enabled
     * re announces nothing.
     *
     * @implNote This implementation raises the camera track and broadcasts the {@code <video>} state together
     * at connect, so a peer that receives the video RTP also learns the camera is on. A mid call camera
     * toggle still flows through {@link #startLocalVideo(String)}/{@link #setVideoEnabled(String, boolean)}.
     *
     * @param orchestrated the call whose media plane connected
     */
    private static void announceInitialVideoState(OrchestratedCall orchestrated) {
        if (!orchestrated.call().isVideo()) {
            return;
        }
        orchestrated.controls().ifPresent(controls -> {
            if (controls.video().state() != VideoStreamState.ENABLED) {
                controls.video().turnCamera(true);
            }
        });
    }

    /**
     * Marks a call reconnecting when its media plane loses its network path.
     *
     * <p>The media plane and transport unit calls this when the call's transport loses connectivity: the
     * controller moves an active call to {@link CallLifecycleState#REJOINING} so the transport can renegotiate
     * its relay or DTLS path; a successful renegotiation returns the call to
     * {@link CallLifecycleState#CALL_ACTIVE} through {@link #onMediaConnected(String)}, while a failed one
     * ends it. A signal for an untracked call is ignored.
     *
     * @implNote This implementation transitions to {@link CallLifecycleState#REJOINING} on a network path
     * loss. The transport state source is the traffic stopped notification, surfaced here as a call from the
     * media plane unit.
     *
     * @param callId the identifier of the call whose media plane lost its path
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public void onMediaLost(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            if (orchestrated.state() == CallLifecycleState.CALL_ACTIVE) {
                transition(orchestrated, CallLifecycleState.REJOINING, CallEventType.CALL_STATE_CHANGED);
            }
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound preaccept: the peer device is alerting.
     *
     * <p>Marks an outbound call's state {@link CallLifecycleState#PREACCEPT_RECEIVED} so the caller can begin
     * early media preparation, and emits the preaccept received event. A preaccept for an untracked call is
     * ignored.
     *
     * @implNote This implementation transitions a call in {@link CallLifecycleState#CALLING} to
     * {@link CallLifecycleState#PREACCEPT_RECEIVED}.
     *
     * @param preaccept    the decoded inbound preaccept
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handlePeerPreaccept(PreacceptStanza preaccept, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            transition(orchestrated, CallLifecycleState.PREACCEPT_RECEIVED, CallEventType.CALL_PREACCEPT_RECEIVED);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound accept: the peer answered an outbound call.
     *
     * <p>Records the answering device, moves an outbound call to {@link CallLifecycleState#ACCEPT_RECEIVED},
     * and brings up the media plane from the relay block the accept (or the earlier offer ack) carried,
     * keying it with the call key the caller minted. A nested {@code <transport>} block in the accept
     * supplies the relay block when the offer ack did not. An accept for an untracked call is ignored.
     *
     * @implNote This implementation transitions to {@link CallLifecycleState#ACCEPT_RECEIVED} and brings up
     * transport and media. The answering device JID is recorded as the peer signaling device so subsequent
     * point to point signaling and the per participant key derivation address the device that answered,
     * keying media from the answering device JID.
     *
     * @param accept       the decoded inbound accept
     * @param senderJid    the device JID that authored the accept, recorded as the answering device
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handlePeerAccept(AcceptStanza accept, Jid senderJid, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            orchestrated.peerDeviceJid(senderJid);
            accept.transportNode()
                    .flatMap(transport -> transport.getChild("relay"))
                    .ifPresent(orchestrated::relay);
            transition(orchestrated, CallLifecycleState.ACCEPT_RECEIVED, CallEventType.CALL_ACCEPT_RECEIVED);
            // The callee answered: cancel the caller lonely no answer timer armed at offer send, otherwise it
            // later fires callerLonelyTimeout and tears the now answered call down with reason=timeout.
            timers.cancel(accept.callId().orElseThrow(), CallTimerKind.CALLER_LONELY);

            var relay = orchestrated.relay().orElse(null);
            var callKey = orchestrated.callKey().orElse(null);
            // Fallback for a caller whose synchronous offer ack carried no relay block: the accept's relay is
            // the first one learned, so start the exchange here. A no op once applyOfferAck already started it.
            startRelayLatencyExchange(orchestrated, relay);
            if (relay != null && callKey != null) {
                bringUpMediaPlane(orchestrated, relay, orchestrated.offerAckVoipSettings(), callKey, true,
                        orchestrated.call().isVideo());
            }
            ensureControls(orchestrated);
        } catch (WhatsAppCallException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "media plane bring-up failed after peer accept for call " + accept.callId().orElseThrow(), e);
            tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.MEDIA_STREAM_ERROR);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound reject: a callee device declined the outbound call.
     *
     * <p>Ends the call with the reject's reason and emits the reject received event, unless the reject comes
     * from a device that is not the active peer. On a multi device callee every device rings, so once one
     * device has answered (its JID pinned as the {@linkplain OrchestratedCall#peerDeviceJid() active
     * peer} by {@link #handlePeerAccept(AcceptStanza, Jid)}), another of the callee's devices declining its
     * own ringing leg must not tear down the now answered call; that reject is dropped. A reject from the
     * active peer itself, or a reject before any device has answered (no active peer pinned), ends the call.
     * A reject for an untracked call is ignored.
     *
     * @implNote This implementation applies the active device guard: a per device reject only ends the call
     * when it concerns the device the call is established with, so a sibling device dismissing its ringing
     * notification does not end a call another device is already carrying.
     *
     * @param reject       the decoded inbound reject
     * @param senderJid    the device JID that authored the reject
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handlePeerReject(RejectStanza reject, Jid senderJid, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            var activePeer = orchestrated.peerDeviceJid().orElse(null);
            if (activePeer != null && senderJid != null && !activePeer.equals(senderJid)) {
                return;
            }
            tearDown(orchestrated, reject.reason(), CallEventType.CALL_REJECT_RECEIVED);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound terminate: either side ended the call, unless an ignore guard suppresses it.
     *
     * <p>Ends the call with the terminate's reason and emits the terminate received event. A terminate for
     * an untracked call is ignored. The reason maps onto the result axis through the info manager update
     * fired by the teardown. Two guards can suppress the teardown so a stale or misdirected terminate does
     * not end an otherwise valid call:
     * <ul>
     *   <li>the companion device guard drops a one to one shaped terminate that arrives during a group
     *       call when its authoring device is one of the local account's own devices, so another of the
     *       user's devices ending its own one to one leg does not tear down the user's group call; and</li>
     *   <li>the joinable on expired offer guard drops a terminate for a joinable (call link) call whose
     *       offer has already expired, so a late terminate does not abort an otherwise valid join.</li>
     * </ul>
     * Each guard is gated on its server feature flag and on the data the guard needs; when the flag is
     * clear, the gate is unbound, or the guard's data is absent, the terminate ends the call as usual.
     *
     * @implNote This implementation maps the reason to a result, tears the media down, and applies the two
     * ignore guards. The companion device guard drops a terminate whose sender is another device of the local
     * account, resolved through the {@linkplain #bindOwnDeviceResolver(Predicate) bound own device predicate}
     * over the authoring {@code senderJid}, scoped (per
     * {@link CallsFeatureGate#isIgnoreOneToOneTerminateInGroupCall()}) to a one to one terminate arriving
     * during a group call. The joinable guard drops a joinable terminate when the call is
     * {@link CallLifecycleState#REJOINING} or {@link CallLifecycleState#ACCEPT_SENT}, or is outside
     * {@link CallLifecycleState#CALL_ACTIVE} and {@link CallLifecycleState#CONNECTED_LONELY} with a reason
     * other than the reasons handled elsewhere; see
     * {@link #isIgnoredJoinableTerminate(OrchestratedCall, TerminateStanza)}.
     *
     * <p>The boolean return reports whether the terminate was effected: {@code true} when the call was torn
     * down, and {@code false} when the terminate was for an untracked call or when either ignore guard
     * suppressed the teardown. The call service reads it so the host {@code onCallEnded} fan out fires only
     * for an effected terminate, matching where the end of call host event fires only after the ignore guards
     * pass.
     *
     * @param terminate    the decoded inbound terminate
     * @param senderJid    the device JID that authored the terminate envelope, the companion device
     *                     discriminator, or {@code null} when the envelope carried no sender
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     * @return {@code true} when the call was torn down; {@code false} when it was untracked or a guard
     *         suppressed the teardown
     */
    private boolean handlePeerTerminate(TerminateStanza terminate, Jid senderJid, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            if (isIgnoredCompanionTerminate(orchestrated, senderJid)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "ignoring terminate from companion device {0} during group call {1}",
                        senderJid, terminate.callId().orElseThrow());
                return false;
            }
            if (isIgnoredJoinableTerminate(orchestrated, terminate)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG,
                        "ignoring joinable terminate in {0} state for call {1}",
                        orchestrated.state(), terminate.callId().orElseThrow());
                return false;
            }
            tearDown(orchestrated, terminate.reason(), CallEventType.CALL_TERMINATE_RECEIVED);
            return true;
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Reports whether an inbound terminate is an ignored companion device terminate during a group call.
     *
     * <p>Returns {@code true} only when every condition of the companion device guard holds: the
     * {@link CallsFeatureGate#isIgnoreOneToOneTerminateInGroupCall()} flag is set, the tracked call is a
     * group call, and the terminate's authoring {@code senderJid} is one of the local account's own
     * devices per the {@linkplain #bindOwnDeviceResolver(Predicate) bound own device predicate}. When the
     * feature gate is unbound, the own device resolver is unbound, or {@code senderJid} is {@code null},
     * the guard does not fire and the caller tears the call down as usual.
     *
     * @implNote This implementation drops a one to one terminate authored by another device of the local
     * account while a group call is active. The own device test reads the store backed predicate; the group
     * call active condition is the call's {@link Call#isGroup()} status. A bare {@link TerminateStanza}
     * carries no group marker to discriminate a one to one terminate from a group terminate on the wire, so
     * the own device author during a group call test is the recoverable form of the one to one companion
     * check the {@code IGNORE_ONE_TO_ONE_TERMINATE_IN_GROUP_CALL} prop names.
     *
     * @param orchestrated the tracked call the terminate targets, accessed under its lock
     * @param senderJid    the device JID that authored the terminate, or {@code null} when absent
     * @return {@code true} when the companion device guard suppresses the teardown
     */
    private boolean isIgnoredCompanionTerminate(OrchestratedCall orchestrated, Jid senderJid) {
        var gate = featureGate;
        if (gate == null || senderJid == null) {
            return false;
        }
        return gate.isIgnoreOneToOneTerminateInGroupCall()
                && orchestrated.call().isGroup()
                && isOwnDevice(senderJid);
    }

    /**
     * Reports whether an inbound joinable terminate is dropped by the state and reason guard.
     *
     * <p>Returns {@code true} only when the call is joinable and its state is one a joinable terminate is
     * ignored in: either {@link CallLifecycleState#REJOINING} or {@link CallLifecycleState#ACCEPT_SENT},
     * or a state outside {@link CallLifecycleState#CALL_ACTIVE} and {@link CallLifecycleState#CONNECTED_LONELY} whose
     * terminate reason is none of the reasons handled elsewhere ({@link CallEndReason#ACCEPTED_ELSEWHERE},
     * {@link CallEndReason#REJECTED_ELSEWHERE}, {@link CallEndReason#DEVICE_SWITCH}). The call is joinable when
     * its inbound {@link OfferStanza#joinable()} offer says so; an outbound call (which tracks no inbound
     * offer) is never treated as joinable here.
     *
     * @implNote This implementation ignores a joinable terminate when the call is
     * {@link CallLifecycleState#REJOINING} or {@link CallLifecycleState#ACCEPT_SENT}, or when it is outside
     * {@link CallLifecycleState#CALL_ACTIVE} and {@link CallLifecycleState#CONNECTED_LONELY} and its reason is
     * none of {@code accepted_elsewhere}, {@code rejected_elsewhere}, or {@code device_switch}. It reads no
     * offer expiry predicate: the call's joinable state stands in for the terminate's joinable flag through
     * the inbound offer's {@code joinable} flag.
     *
     * @param orchestrated the tracked call the terminate targets, accessed under its lock
     * @param terminate    the decoded inbound terminate, supplying the reason for the not established branch
     * @return {@code true} when the joinable terminate guard suppresses the teardown
     */
    private boolean isIgnoredJoinableTerminate(OrchestratedCall orchestrated, TerminateStanza terminate) {
        var offer = orchestrated.incomingOffer().orElse(null);
        if (offer == null || !offer.joinable()) {
            return false;
        }
        var state = orchestrated.state();
        if (state == CallLifecycleState.REJOINING || state == CallLifecycleState.ACCEPT_SENT) {
            return true;
        }
        if (state != CallLifecycleState.CALL_ACTIVE && state != CallLifecycleState.CONNECTED_LONELY) {
            var reason = terminate.reason();
            return reason != CallEndReason.ACCEPTED_ELSEWHERE
                    && reason != CallEndReason.REJECTED_ELSEWHERE
                    && reason != CallEndReason.DEVICE_SWITCH;
        }
        return false;
    }

    /**
     * Handles an inbound transport message, recording an updated relay block.
     *
     * <p>A transport message carries the peer's transport and relay candidates; the controller records a
     * relay block it carries so a deferred media plane bring up can use it. The finer transport subtype
     * routing (remote candidates, relay latency, peer health, ICE and DTLS) is owned by the transport unit. A
     * transport message for an untracked call is ignored.
     *
     * @implNote This implementation captures the relay block; the transport unit handles the candidate, relay
     * latency, and ICE and DTLS subtypes, which are not reproduced here.
     *
     * @param transport    the decoded inbound transport message
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handlePeerTransport(TransportStanza transport, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            transport.toStanza().getChild("relay").ifPresent(orchestrated::relay);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound relay latency report: folds the peer's per relay round trip latencies into the
     * call's relay election state.
     *
     * <p>A {@code <relaylatency>} report carries the peer's measured latency to each relay it probed; recording
     * it marks the relays the peer can reach, so the next {@linkplain RelayLatencyState#electBestRelayName(RelayElection.Mode)
     * election} the media plane bring up runs can converge both ends onto a relay they share. A report for an
     * untracked call, or one that arrives before the local end has built its own
     * {@linkplain OrchestratedCall#relayLatencyState() relay latency state} from its relay block, is
     * dropped.
     *
     * <p>Receiving a report also pins the peer device the relay exchange runs with: it replies with the local
     * end's own per relay latencies addressed to the exact {@code senderJid} that sent this report, so the
     * peer's own election folds in the relays the local end reaches and converges onto a shared relay. This
     * device to device reply is required because the proactive offer ack report is addressed to the callee's
     * primary device, which is not the device that answers and runs the election; the answering device only
     * learns the local end's relays through this reply to its own report.
     *
     * @implNote This implementation overwrites the per relay remote latency table the election reads, keyed by
     * relay name. The election itself runs at bring up; the reply mirrors the bidirectional exchange both ends
     * drive, each end reporting its latencies to the specific peer device it is exchanging with.
     *
     * @param message      the decoded inbound relay latency report
     * @param senderJid    the peer device that sent the report, the reply's recipient
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handlePeerRelayLatency(RelayLatencyStanza message, Jid senderJid, OrchestratedCall orchestrated) {
        orchestrated.lock().lock();
        try {
            var state = orchestrated.relayLatencyState().orElse(null);
            if (state == null) {
                return;
            }
            state.recordPeerLatencies(message.entries());
            // Reply with the local end's own relay latencies to the exact device that sent this report, so the
            //  answering device (not the primary the proactive offer ack report is addressed to) learns the
            //  relays the local end reaches and re elects onto the shared relay before it binds.
            if (host != null && senderJid != null) {
                var callId = orchestrated.callId();
                var reply = new RelayLatencyStanza(callId, orchestrated.call().creator(), false, -1,
                        state.toLatencyEntries());
                host.sendSignaling(CallStanza.toCall(reply, senderJid, callId));
            }
            // The primary flow needs no mid call switch: the peer sends its <relaylatency> reports before its
            //  <accept>, so they are folded in before the media plane bring up runs the election, which then
            //  converges both ends at bind time. A report that arrives after bring up (a relay rekey, or a
            //  late peer) cannot move the bound relay in this pass: the relay address is selected once at
            //  bring up and LiveRelayTransport binds it as a one shot.
            // TODO: handle the mid call relay switch for a report that arrives after bring up: re run the
            //  election here and, when it elects a different relay, rebind the transport.
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Builds the call's relay election state from a learned relay block and ships the local end's
     * {@code <relaylatency>} report to the peer, once per call.
     *
     * <p>Parses the relay block into its {@link RelayInfo}, seeds a {@link RelayLatencyState} from the offered
     * relay {@linkplain RelayInfo#endpoints() endpoints} (each relay's {@code c2r_rtt} measured by the server),
     * records it on the orchestration handle, and sends a {@code <relaylatency>} carrying the seeded per relay
     * latencies to the peer so the two ends can exchange their views and converge their relay choice before the
     * media plane comes up. The send is idempotent per call: a second invocation (a later relay block on the
     * same call) is a no op once the state exists, so the report is sent exactly once. A call with no host
     * transport, no relay block, or a relay block that does not parse sends nothing.
     *
     * @implNote This implementation seeds the per relay table from the offered {@code c2r_rtt} and reports it
     * in a {@code <relaylatency>}, then folds the peer's report into the table the election reads. The report
     * is addressed with the same {@link #controlRecipient(OrchestratedCall) recipient rule} the in call
     * control actions use (the MUC call target for a group call, the peer signaling device otherwise).
     *
     * @param orchestrated the call whose relay latency exchange is started
     * @param relayNode    the call's learned {@code <relay>} block subtree, or {@code null} when none is known
     */
    private void startRelayLatencyExchange(OrchestratedCall orchestrated, Stanza relayNode) {
        if (host == null || relayNode == null || orchestrated.relayLatencyState().isPresent()) {
            return;
        }
        var relayInfo = RelayInfo.of(relayNode).orElse(null);
        if (relayInfo == null) {
            return;
        }
        // The local per relay latencies are seeded from the offer ack c2r_rtt the server measured (the
        //  RelayLatencyState constructor). This seed converges both ends: the election picks by the sum of
        //  each relay's per party latencies, so as long as each end reports the same value it elects with
        //  (this state reports and elects from the same localLatencies), both ends compute an identical per
        //  relay sum and elect the same relay regardless of the latency scale. A live per relay probe ping
        //  round that would replace the c2r_rtt seed with a locally measured RTT
        //  (RelayLatencyState.recordProbeLatency) is an optional accuracy refinement; it does not affect
        //  convergence, only which of several shared relays wins a close call.
        // TODO: run the pre bind probe ping round to seed measured RTTs; blocked on the relay's probe
        //  response wire shape, untested without a probe capture.
        var state = new RelayLatencyState(relayInfo.endpoints());
        orchestrated.relayLatencyState(state);
        var callId = orchestrated.callId();
        var report = new RelayLatencyStanza(callId, orchestrated.call().creator(), false, -1,
                state.toLatencyEntries());
        host.sendSignaling(CallStanza.toCall(report, controlRecipient(orchestrated), callId));
    }

    /**
     * Handles an inbound group update: reconciles the membership roster and re decides the active state.
     *
     * <p>A {@code <group_update>} carries the refreshed {@code <group_info>} roster of an in progress group
     * call. The controller reconciles the call's {@link CallMembership} against it, then re runs the
     * active versus lonely decision: a group call with at least one other connected participant is
     * {@link CallLifecycleState#CALL_ACTIVE}, otherwise it stays or returns to
     * {@link CallLifecycleState#CONNECTED_LONELY}. The update additionally carries the call's {@code <relay>} and
     * {@code <voip_settings>} as {@code extraChildren}; for a call answered or joined against a group offer
     * (which carries no relay for the media plane), this is where the media plane is brought up, on the relay
     * the update delivers and the shared call key minted at accept. A membership change then triggers a fresh
     * shared key share, so the local participant re fans its current call key to the connected roster, stamped
     * with the local device JID (from the self JID supplier, since the call creator is the local device only
     * for a placement). A group update for an untracked call, or one carrying no roster, is ignored.
     *
     * @implNote This implementation reconciles then decides and brings the media plane up once the relay is
     * known: the membership diff drives the active versus lonely transition, the relay from the update brings
     * the deferred group media plane up, and the call key is re shared on a membership change. The re fan is
     * not gated to the caller, since every participant (a placer, an answering callee, or a late joiner) fans
     * its own per participant E2E key. A peer's inbound {@code enc_rekey} key is acknowledged but not
     * installed into per participant crypto, because the SFU relayed media rides the relay hop by hop SRTP the
     * relay block carries and does not engage the per frame SFrame transform ({@code openSframe} passes the
     * body through); the inbound routing is in {@link #dispatchInbound}.
     *
     * @param groupUpdate  the decoded inbound group update
     * @param orchestrated the resolved call context, whose per-call lock the caller holds
     */
    private void handleGroupUpdate(GroupUpdateStanza groupUpdate, OrchestratedCall orchestrated) {
        var roster = groupUpdate.groupInfoValue().orElse(null);
        if (roster == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            var membership = orchestrated.membership().orElse(null);
            if (membership == null) {
                return;
            }
            // A group call answered or joined against a group offer brings its media plane up on the relay the
            // group_update delivers, not the offer's; record it and seed the relay latency exchange over it.
            var relayNode = groupUpdate.extraChildren().stream()
                    .filter(child -> child.hasDescription("relay"))
                    .findFirst()
                    .orElse(null);
            if (relayNode != null) {
                orchestrated.relay(relayNode);
                startRelayLatencyExchange(orchestrated, relayNode);
            }

            var diff = membership.reconcile(roster);
            if (!diff.isEmpty()) {
                infoUpdater.updateForEvent(orchestrated.callId(), CallEventType.GROUP_INFO_CHANGED);
                events.emit(CallEventType.GROUP_INFO_CHANGED, DataUtils.EMPTY_BYTE_ARRAY);
            }

            // Bring up the media plane for a group call answered or joined but not yet connected, now that the
            // group relay is known and the shared call key has been minted at accept. A group placement whose
            // media is already up is a no op (bringUpMediaPlane returns early on an existing session).
            if (orchestrated.mediaSession().isEmpty()) {
                var relay = orchestrated.relay().orElse(null);
                var callKey = orchestrated.callKey().orElse(null);
                if (relay != null && callKey != null) {
                    try {
                        var voipSettings = groupUpdate.extraChildren().stream()
                                .filter(child -> child.hasDescription(VOIP_SETTINGS_ELEMENT))
                                .toList();
                        bringUpMediaPlane(orchestrated, relay, voipSettings, callKey,
                                orchestrated.isCaller(), orchestrated.call().isVideo());
                    } catch (WhatsAppCallException e) {
                        if (Log.WARNING) LOGGER.log(Level.WARNING,
                                "group media bring-up failed for call " + orchestrated.callId(), e);
                        tearDown(orchestrated, CallEndReason.SETUP_FAILED, CallEventType.MEDIA_STREAM_ERROR);
                        return;
                    }
                }
            }

            // Re share the local shared call key to the connected roster on a membership change: a placement
            // re fans on every join or leave, and a device that has just joined fans on its first group_update
            // so every peer can derive its SFrame keys from it. The rekey is stamped with the local device JID
            // from the self JID supplier (the call creator for a placement, the joining device for a join),
            // rather than the call creator, which is the local device only for a placement.
            if (!diff.isEmpty()) {
                var self = selfDeviceJid();
                var sender = self != null ? self : orchestrated.call().creator();
                orchestrated.callKey().ifPresent(callKey -> fanOutGroupRekey(orchestrated, sender, callKey));
            }

            // After any re fan re stamps the roster's devices, clear the offer marker of every peer the
            // roster now reports connected, so a connected peer's key rotation is never mistaken for an
            // unanswered offer and only a peer that never connects remains a sweep candidate.
            orchestrated.groupOutbound().ifPresent(GroupCallOutbound::clearConnectedOffers);
            decideGroupActiveState(orchestrated, membership);
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound {@code mute_v2}: a peer reported its own mute state or asked the local user to mute.
     *
     * <p>Routes a peer mute request to the call's {@link MuteController#onPeerMuteRequest(Jid)}, which
     * surfaces a {@link MuteByAnotherParticipant} unless the
     * recent unmute lockout is active, and a peer self state report to
     * {@link MuteController#onPeerMuted(Jid)}, which satisfies a pending outbound peer mute request without
     * re emitting. A {@code mute_v2} for an untracked call, or one whose control units are not built, is
     * ignored.
     *
     * @param mute      the decoded inbound mute_v2 action
     * @param senderJid the device JID that authored the action, treated as the reporting peer
     */
    private void handlePeerMute(MuteV2Stanza mute, Jid senderJid) {
        withControls(mute.callId().orElseThrow(), controls -> {
            if (mute.peerRequest()) {
                controls.mute().onPeerMuteRequest(senderJid);
            } else {
                controls.mute().onPeerMuted(senderJid);
            }
        });
    }

    /**
     * Handles an inbound {@code video_state}: a peer reported a change in its video stream state.
     *
     * <p>Routes the report to the call's {@link VideoStateController#onPeerVideoState(Jid, VideoStreamState)},
     * which emits the peer's video change, and projects the reported state onto the owning
     * {@link CallParticipant} so a {@link com.github.auties00.cobalt.calls.engine.participant.ParticipantView}
     * snapshot reflects the live peer video state: the participant's {@linkplain CallParticipant#videoState(int)
     * video state code} is set to the reported state's {@linkplain VideoStreamState#wireOrdinal() engine
     * ordinal} and the reporting device's {@linkplain
     * com.github.auties00.cobalt.calls.engine.participant.CallDeviceInfo#videoEnabled(boolean) per device
     * video enabled flag} is set to whether the reported state is {@link VideoStreamState#ENABLED}. A
     * {@code video_state} for an untracked call, or one whose control units are not built, still projects
     * the state when the call's membership knows the reporting device.
     *
     * @implNote This implementation mirrors the participant plane writes made alongside the host event: it
     * sets the participant's video state code (the value
     * {@link CallParticipant#videoState(int)} reads back)
     * and toggles the reporting device's per device video enabled flag. The subscribed encoded stream id the
     * view also carries is driven by the video subscription manager in the media plane, not by this signaling
     * path, so it is not set here.
     *
     * @param videoState the decoded inbound video state action
     * @param senderJid  the device JID that authored the action, treated as the reporting peer
     */
    private void handlePeerVideoState(VideoStateStanza videoState, Jid senderJid) {
        var state = videoState.state();
        projectPeerMediaState(videoState.callId().orElseThrow(), senderJid, participant -> {
            participant.videoState(state.wireOrdinal());
            participant.device(senderJid)
                    .ifPresent(device -> device.videoEnabled(state == VideoStreamState.ENABLED));
        });
        withControls(videoState.callId().orElseThrow(), controls -> controls.video().onPeerVideoState(senderJid, state));
    }

    /**
     * Handles an inbound {@code screen_share}: a peer reported a change in its screen share stream.
     *
     * <p>Routes the report to the call's
     * {@link ScreenShareController#onPeerScreenShare(Jid, ScreenShareState, int)
     * onPeerScreenShare}, resolving the numeric wire state through
     * {@link ScreenShareState#ofCode(int)}; an unrecognized
     * state code is dropped. A {@code screen_share} for an untracked call, or one whose control units are not
     * built, is ignored.
     *
     * @implNote This implementation mirrors the participant plane write made alongside the host event: it sets
     * the participant's screen sharing flag read back to find the sharing peer, modeling it as
     * {@link CallParticipant#screenSharing(boolean)}, set when the reported state is
     * {@link ScreenShareState#STARTED} and cleared otherwise.
     *
     * @param screenShare the decoded inbound screen share action
     * @param senderJid   the device JID that authored the action, treated as the reporting peer
     */
    private void handlePeerScreenShare(ScreenShareStanza screenShare, Jid senderJid) {
        ScreenShareState.ofCode(screenShare.state()).ifPresent(state -> {
            projectPeerMediaState(screenShare.callId().orElseThrow(), senderJid,
                    participant -> participant.screenSharing(state == ScreenShareState.STARTED));
            withControls(screenShare.callId().orElseThrow(),
                    controls -> controls.screenShare().onPeerScreenShare(senderJid, state, screenShare.version()));
        });
    }

    /**
     * Projects a peer media state change onto the owning participant aggregate under the call's lock.
     *
     * <p>Resolves the tracked call, takes its lock, finds the call membership's participant owning the
     * reporting device JID, and applies the mutation to that live {@link CallParticipant} so a later
     * {@link com.github.auties00.cobalt.calls.engine.participant.ParticipantView} snapshot reflects the
     * change. A call that is not tracked, that has no membership (a one to one call keeps no roster), or
     * whose membership does not know the reporting device runs no mutation, reading the peer state off the
     * participant only when the participant set holds that device.
     *
     * @param callId    the identifier of the call the report belongs to
     * @param senderJid the reporting device JID whose owning participant is mutated
     * @param mutation  the mutation to apply to the resolved participant aggregate
     */
    private void projectPeerMediaState(String callId, Jid senderJid, Consumer<CallParticipant> mutation) {
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            orchestrated.membership()
                    .flatMap(membership -> membership.findByDeviceJid(senderJid))
                    .ifPresent(slot -> mutation.accept(slot.participant()));
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Handles an inbound {@code raise_hand}: a peer raised or lowered its hand.
     *
     * <p>Routes the report to the call's {@link RaiseHandController#onPeerHandRaised(Jid, boolean)}, which
     * emits the peer's hand state change and feeds the grid ranking comparator. A {@code raise_hand} for an
     * untracked call, or one whose control units are not built, is ignored.
     *
     * @param raiseHand the decoded inbound raise hand action
     * @param senderJid the device JID that authored the action, treated as the reporting peer
     */
    private void handlePeerRaiseHand(RaiseHandStanza raiseHand, Jid senderJid) {
        withControls(raiseHand.callId().orElseThrow(), controls -> controls.raiseHand().onPeerHandRaised(senderJid, raiseHand.raised()));
    }

    /**
     * Decides whether a group call is active or lonely from its connected participant count.
     *
     * <p>A group call with at least one other connected participant is moved to
     * {@link CallLifecycleState#CALL_ACTIVE}; one with none returns to {@link CallLifecycleState#CONNECTED_LONELY}.
     * Only the two in call states are transitioned, since the active versus lonely decision applies once the
     * call has connected to the unit; a call still in its offer or accept leg is left for the media connected
     * path to advance.
     *
     * @implNote This implementation selects {@link CallLifecycleState#CALL_ACTIVE} when a peer is connected
     * and {@link CallLifecycleState#CONNECTED_LONELY} when none is (the local participant is alone). The
     * connected peer test reads the reconciled membership's
     * {@link CallMembership#participantProvider() participant provider}
     * ({@link com.github.auties00.cobalt.calls.engine.participant.ParticipantProvider#firstConnectedPeer()}),
     * whose per slot aggregates carry the membership state the reconcile projected from each roster entry's
     * {@code "connected"} literal of the server user state table.
     *
     * @param orchestrated the group call being decided
     * @param membership   the call's reconciled membership
     */
    private void decideGroupActiveState(OrchestratedCall orchestrated, CallMembership membership) {
        var state = orchestrated.state();
        if (state != CallLifecycleState.CALL_ACTIVE && state != CallLifecycleState.CONNECTED_LONELY) {
            return;
        }
        var anyPeerConnected = membership.participantProvider().firstConnectedPeer().isPresent();
        var target = anyPeerConnected ? CallLifecycleState.CALL_ACTIVE : CallLifecycleState.CONNECTED_LONELY;
        transition(orchestrated, target, CallEventType.CALL_STATE_CHANGED);
    }

    /**
     * Runs an action against a tracked call's in call control units under the call's lock.
     *
     * <p>Resolves the orchestration handle, takes the call's lock, builds the call's control units if they
     * are not yet built and the bus is bound, and runs the action against them. A call that is not tracked,
     * or one whose control units could not be built (the bus is unbound), runs no action; this is the
     * uniform no op the public in call control methods and the inbound action handlers fall back to so an
     * action on an absent or not yet answerable call is silently dropped, matching the best effort control
     * plane.
     *
     * @param callId the identifier of the call whose control units the action targets
     * @param action the action to run against the call's control units
     */
    private void withControls(String callId, Consumer<CallControls> action) {
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            return;
        }
        orchestrated.lock().lock();
        try {
            ensureControls(orchestrated);
            var controls = orchestrated.controls().orElse(null);
            if (controls != null) {
                action.accept(controls);
            }
        } finally {
            orchestrated.lock().unlock();
        }
    }

    /**
     * Builds and stores a call's in call control units the first time the call becomes answerable.
     *
     * <p>Constructs the {@link CallControlContext} for the call, a {@link ControlEventBridge} over the shared
     * bus that stamps the call id onto the host events the units emit, a {@link CallSignalingSender} that
     * wraps each action in the {@code <call>} envelope addressed to the call's current control recipient, and
     * the four signaling plane control units (mute, video, screen share, raise hand) plus the
     * {@link SpeakerRankingService} the raise hand unit feeds. When the call's media plane carries an
     * application data plane, the three app data backed control units (reactions, live transcription, IMU)
     * are also constructed over the media session's {@link AppDataController} and wired onto the same
     * {@link ControlEventBridge}: the {@link ReactionController} attaches its outbound send seam to
     * {@link AppDataController#sendReaction(String)} and registers itself as the controller's inbound
     * reaction observer ({@link AppDataController#attachReactionObserver(BiConsumer)} plus
     * the reaction clear sweep), the {@link TranscriptionController} registers as the inbound transcription
     * observer ({@link AppDataController#attachTranscriptionObserver(BiConsumer)}), and the
     * {@link ImuDataController} is built over the call's outbound IMU app data send. A call whose transport
     * carries no app data plane skips the three app data units, which are then left unset on the holder. The
     * units are stored on the orchestration handle and closed on teardown. This is idempotent: a call whose
     * units are already built, or one for which the bus has not been bound, is left unchanged. Must be called
     * under the call's lock.
     *
     * @implNote This implementation builds the control units lazily, once a call is answerable (accept sent,
     * accept received, or a group call gone active). The screen share version defaults to
     * {@link ScreenShareController#VERSION_V2} (the single stream port swap path); the V3 dual stream
     * negotiation is a separate piece. The control context's self JID is read from the bound supplier and
     * falls back to the call creator, which is the local device for an outbound call. The app data control
     * units exist only once the media plane is up and exposes an {@link AppDataController}, so they are built
     * here rather than at signaling plane construction time: a call answered before its relay block is known
     * builds the signaling plane units first and gains the app data units on the {@code ensureControls} pass
     * that follows the media plane bring up.
     *
     * @param orchestrated the call whose control units are built
     */
    private void ensureControls(OrchestratedCall orchestrated) {
        if (eventBus == null || orchestrated.controls().isPresent()) {
            return;
        }
        var call = orchestrated.call();
        var callId = orchestrated.callId();
        var creator = call.creator();
        var resolvedSelf = selfDeviceJid();
        var self = resolvedSelf != null ? resolvedSelf : creator;
        var context = call.isGroup()
                ? CallControlContext.group(callId, creator, self)
                : CallControlContext.oneToOne(callId, creator, self);
        CallSignalingSender sender =
                message -> host.sendSignaling(CallStanza.toCall(message, controlRecipient(orchestrated), callId));
        var bridge = new ControlEventBridge(callId, eventBus);
        var ranking = new SpeakerRankingService(bridge);
        var mute = new MuteController(context, sender, bridge);
        var video = new VideoStateController(context, sender, bridge);
        var screenShare = new ScreenShareController(context, sender, bridge, screenShareVersion());
        var raiseHand = new RaiseHandController(context, sender, bridge, ranking);
        // TODO: wire TonePlaybackManager. Construct new TonePlaybackManager(bridge) next to these per call
        //  controllers and drive activate and deactivate from the CallStateMachine transitions (connecting,
        //  ringback, busy, incoming pending call), calling clear() at teardown, for tone priority playback.

        ReactionController reaction = null;
        TranscriptionController transcription = null;
        ImuDataController imu = null;
        var appData = orchestrated.appDataController().orElse(null);
        if (appData != null) {
            reaction = new ReactionController(self, bridge);
            reaction.attach(appData);
            appData.attachReactionObserver(reaction::onReaction);

            transcription = new TranscriptionController(bridge);
            var transcriptionUnit = transcription;
            appData.attachTranscriptionObserver(transcriptionUnit::onTranscript);

            // The IMU sender and observer are intentional no ops for the web target: the web VoIP stack has no
            //  sensor, accelerometer, or DeviceMotion source, so no outbound IMU samples exist to serialize,
            //  and IMU does not ride the app data stream this controller demuxes (reaction, transcription,
            //  rekey, subscription, feedback), so no inbound IMU flows on this path to parse. The ImuSample
            //  frame layout is modeled (ImuDataController), but both directions are moot for the web target.
            imu = new ImuDataController(sample -> { }, (participant, sample) -> { });
        }

        orchestrated.controls(new CallControls(
                mute, video, screenShare, raiseHand, reaction, transcription, imu, null, null));
    }

    /**
     * Resolves the screen share protocol version a call's {@link ScreenShareController} advertises.
     *
     * <p>The server expresses the screen share capability as a milestone version rather than a boolean:
     * {@link CallsFeatureGate#screenShareMilestoneVersion()} returns the negotiated milestone (its prop
     * default is {@code 2}), which is the protocol version the {@code <screen_share>} action carries, so it
     * is fed straight into the controller. When the feature gate is unbound (the test harnesses build the
     * controller with no AB props service) this falls back to {@link ScreenShareController#VERSION_V2}, the
     * single stream port swap version the prop default selects, so a build with no gate behaves the same.
     *
     * @implNote This implementation feeds the screen share milestone prop into the
     * {@code <screen_share version>} the controller stamps. The milestone is the screen share protocol
     * version directly ({@code 2} selects {@link ScreenShareController#VERSION_V2}, {@code 3} selects
     * {@link ScreenShareController#VERSION_V3}); the gate already folds the milestone versus master gate
     * decision into {@link CallsFeatureGate#isScreenShareEnabled()}, which {@link #startScreenShare(String)}
     * checks before a share starts, so this read supplies only the version the action advertises.
     *
     * @return the negotiated screen share protocol version, or {@link ScreenShareController#VERSION_V2} when
     *         the feature gate is unbound
     */
    private int screenShareVersion() {
        var gate = featureGate;
        return gate == null ? ScreenShareController.VERSION_V2 : gate.screenShareMilestoneVersion();
    }

    /**
     * Resolves the recipient an in call control action for a call is addressed to.
     *
     * <p>A group call addresses its MUC call target {@code <callId>@call}; a one to one call addresses the
     * peer's signaling device when it is known, falling back to the peer user JID. This is the same recipient
     * rule the accept and preaccept legs use, evaluated per send so a peer device JID that becomes known
     * after the control units are built (the caller learns the answering device) is picked up.
     *
     * @param orchestrated the call whose control recipient is resolved
     * @return the recipient JID for an in call control action
     */
    private static Jid controlRecipient(OrchestratedCall orchestrated) {
        var call = orchestrated.call();
        return call.isGroup()
                ? Jid.of(call.callId() + "@call")
                : orchestrated.peerDeviceJid().orElse(call.peer());
    }

    /**
     * Brings up the media plane for a call and records the resulting session.
     *
     * <p>Delegates to
     * {@link MediaPlane#bringUp(String, Stanza, List, byte[], boolean, boolean, int, CallMembership, MediaStreams, Jid, Optional)}
     * with the negotiated {@code <voip_settings>} bundles, the call's current membership size, and the
     * call's recorded application capture sources and playback sinks, and stores the returned session on the
     * orchestration handle so the teardown can close it. The bring up exception is propagated to the caller,
     * which decides whether to tear the call down.
     *
     * <p>The participant count is the call's membership size for a group call and zero for a one to one call
     * (which tracks no membership roster); the media plane treats a missing roster as the default call size.
     *
     * <p>Once the session is up, the session's application data controller (present only when the brought up
     * transport carries an app data plane) is recorded on the orchestration handle so the in call control
     * units that observe the app data side channel can attach themselves to it when they are built, which
     * happens later in the call lifecycle in {@link #ensureControls(OrchestratedCall)}. A transport
     * with no app data plane records {@code null}, and the app data backed control units are then simply not
     * built.
     *
     * @param orchestrated the call whose media plane is being brought up
     * @param relay        the relay block subtree
     * @param voipSettings the {@code <voip_settings>} bundle nodes the offer (callee) or offer
     *                     acknowledgement (caller) carried, in wire order
     * @param callKey      the raw call key the SRTP and SFrame keys derive from
     * @param isCaller     whether the local side placed the call
     * @param video        whether the local side participates with video
     * @throws WhatsAppCallException if the media plane cannot be brought up
     */
    private void bringUpMediaPlane(OrchestratedCall orchestrated, Stanza relay, List<Stanza> voipSettings,
                                   byte[] callKey, boolean isCaller, boolean video) {
        if (orchestrated.mediaSession().isPresent()) {
            return;
        }
        var membership = orchestrated.membership().orElse(null);
        var participantCount = membership == null ? 0 : membership.size();
        // Elect the relay both ends reach from the exchanged latencies before the transport binds; an empty
        // election (no peer report yet, or no relay both ends reported) leaves the bring up on its local pick.
        var electedRelayName = orchestrated.relayLatencyState()
                .flatMap(state -> state.electBestRelayName(RelayElection.Mode.DEFAULT));
        var session = mediaPlane.bringUp(orchestrated.callId(), relay, voipSettings, callKey, isCaller, video,
                participantCount, membership, orchestrated.mediaStreams(), orchestrated.peerDeviceJid().orElse(null),
                electedRelayName, mediaSessionListener);
        orchestrated.mediaSession(session);
        orchestrated.appDataController(session.appDataController().orElse(null));
    }

    /**
     * Returns the {@code <voip_settings>} bundle subtrees carried directly under a call ack stanza.
     *
     * <p>The server denormalises the engine parameter bundles into the synchronous offer ack as direct
     * {@code <voip_settings>} children alongside the relay, user, and rte blocks; this reads them in wire
     * order for the caller side media plane bring up.
     *
     * @param ack the server's call ack stanza
     * @return the ack's {@code <voip_settings>} children, in wire order; empty when the ack carries none
     */
    private static List<Stanza> voipSettingsOf(Stanza ack) {
        return ack.getChildren(VOIP_SETTINGS_ELEMENT)
                .stream()
                .toList();
    }

    /**
     * Builds the {@code <offer>} action for an outbound one to one call.
     *
     * <p>The offer carries the call creator, the caller identity hints from {@code identity} (the
     * {@code caller_pn} and {@code username} attributes, both absent today per {@link #offerIdentity()}), the
     * caller's standard capability advertisement, the offered audio codecs, the standard encryption options,
     * and the per device call key fanout the crypto facade produced. A video offer additionally advertises the
     * video codecs.
     *
     * @param callId    the call identifier
     * @param self      the local device JID, the call creator
     * @param devices   the peer device JIDs the key is fanned out to
     * @param callKey   the minted call key
     * @param video     whether video is offered
     * @param identity  the caller identity hints (caller phone number JID and username) the offer advertises,
     *                  each carried only when present
     * @return the offer action
     */
    private OfferStanza buildOffer(String callId, Jid self, List<Jid> devices, byte[] callKey,
                                   boolean video, OfferIdentity identity) {
        var plaintext = crypto.wrapCallKey(callKey);
        var keyDistribution = crypto.encryptOfferFanout(devices, plaintext);
        var capabilities = List.of(standardCapability());
        var audioCodecs = standardAudioCodecs();
        var videoCodecs = video ? standardVideoCodecs() : List.<CallCodecDescriptor>of();
        var deviceIdentity = anyPreKeyMessage(keyDistribution) ? crypto.signedDeviceIdentity() : null;
        return new OfferStanza(callId, self, identity.callerPn(), null, identity.username(), null, null, null,
                false, false, null, -1, NET_MEDIUM_OFFER, capabilities, audioCodecs, videoCodecs, keyDistribution, null,
                CallEncOptions.standard(), null, deviceIdentity, null, null, null, List.of(), null);
    }

    /**
     * Builds the {@code <offer>} action for an outbound group call.
     *
     * <p>The group offer carries the offered audio codecs, the caller network medium, and the
     * {@code <group_info>} roster of the rostered participants, with the caller's capability advertised
     * inline on the self device inside that roster. Unlike the one to one offer it carries NO
     * {@code group-jid} or {@code joinable} attribute, NO offer level {@code <capability>}, NO
     * {@code <encopt>}, and NO per device key fanout (the per participant key arrives post join as
     * {@code <enc_rekey>}), so no Signal session is established here and no device identity is attached. A
     * video group call additionally advertises the video codecs.
     *
     * @implNote This implementation builds an {@code <offer call-id call-creator>} with children
     * {@code <audio>}, {@code <net>}, then {@code <group_info>}, carrying neither {@code group-jid} nor
     * {@code joinable} nor an offer level {@code <capability>} or {@code <encopt>}. The group identity travels
     * in the {@code <group_info>} roster and the {@code <call to="<callId>@call">} envelope, not in a
     * {@code group-jid} attribute; the {@code joinable} and {@code group-jid} attributes the
     * {@link OfferStanza} can still serialize belong to the call link or scheduled call offer, not a member
     * placement.
     *
     * @param callId   the call identifier
     * @param self     the local device JID, the call creator
     * @param roster   the participant roster carried as the offer's {@code <group_info>}
     * @param video    whether video is offered
     * @param identity the caller identity hints (caller phone number JID and username) the offer advertises,
     *                 each carried only when present (both absent today per {@link #offerIdentity()})
     * @return the group offer action
     */
    private OfferStanza buildGroupOffer(String callId, Jid self, GroupInfoStanza roster,
                                        boolean video, OfferIdentity identity) {
        var audioCodecs = standardAudioCodecs();
        var videoCodecs = video ? standardVideoCodecs() : List.<CallCodecDescriptor>of();
        return new OfferStanza(callId, self, identity.callerPn(), null, identity.username(), null, null, null,
                false, false, null, -1, NET_MEDIUM_OFFER, List.of(), audioCodecs, videoCodecs, List.of(), null,
                null, roster.toStanza(), null, null, null, null, List.of(), null);
    }

    /**
     * Resolves the caller identity hints an outbound {@code <offer>} advertises, which are none.
     *
     * <p>Which identity hints an offer carries are gated on server AB props the {@link CallsFeatureGate}
     * exposes:
     * <ul>
     *   <li>{@link CallsFeatureGate#callingLidVersion()} is the LID versus PN calling decision. The
     *       call creator JID form is fixed upstream (the call service stamps the account JID, a LID device
     *       JID once LID calling is active).</li>
     *   <li>{@link CallsFeatureGate#isPhoneNumberPrivacyEnabled()} governs whether the caller's phone number
     *       may accompany a LID call as {@code caller_pn}.</li>
     *   <li>{@link CallsFeatureGate#isCallingUsernameEnabled()} governs whether a {@code username} hint
     *       accompanies the call.</li>
     * </ul>
     *
     * <p>The client does NOT itself stamp {@code caller_pn} or {@code username} on the offer it sends. On the
     * sender egress a one to one offer carries only {@code call-creator} (a {@code @lid} device JID);
     * {@code caller_pn} and {@code caller_country_code} appear only on the copy the server relays to the
     * callee, server injected like {@code <relay>} and {@code <voip_settings>}. So this resolver supplies
     * neither hint, and an offer carries neither attribute regardless of the gate.
     *
     * @implNote This implementation returns no hints. The {@link OfferStanza} keeps its {@code caller_pn} and
     * {@code username} components (the inbound parse populates them on a received offer), but the outbound
     * builders pass {@code null} for both.
     *
     * @return the no hint identity; the outbound offer carries neither {@code caller_pn} nor {@code username}
     */
    private OfferIdentity offerIdentity() {
        // The client does not stamp caller_pn or username on the offer it sends (the server injects caller_pn
        // on the relayed copy). No outbound hint is selected.
        return OfferIdentity.NONE;
    }

    /**
     * The caller identity hints an outbound {@code <offer>} could carry.
     *
     * <p>Each hint is present only when an outbound value is available; an absent hint is {@code null}, which
     * the {@link OfferStanza} builder omits from the wire element. The {@link #NONE} singleton is the no hint
     * shape {@link #offerIdentity()} always returns today, since the client does not stamp {@code caller_pn}
     * or {@code username} on the offer it sends.
     *
     * @param callerPn the caller's phone number JID to advertise as {@code caller_pn}, or {@code null} to
     *                 omit it
     * @param username the caller's username to advertise as {@code username}, or {@code null} to omit it
     */
    private record OfferIdentity(Jid callerPn, String username) {
        /**
         * The no hint identity carrying neither {@code caller_pn} nor {@code username}.
         */
        private static final OfferIdentity NONE = new OfferIdentity(null, null);
    }

    /**
     * Builds the outbound group {@code <offer>} {@code <group_info>} roster: the local (self) member first,
     * then each invited participant, every member enumerating its device JIDs.
     *
     * <p>The self member is one {@code <user>} carrying the single call creator {@code <device>} with the
     * inline {@code <capability>} advertisement; each participant member is one {@code <user>} enumerating
     * its known device JIDs as bare {@code <device>} children. The roster carries no {@code connected-limit}
     * (that attribute appears on the {@code <group_update>} broadcast roster, not the offer's), and no
     * per member state, since the relay decorates the members with their state on the roster it echoes back.
     *
     * @implNote This implementation lists, per {@code <user jid>}, every device the caller knows for that
     * member as a {@code <device jid>} child, and the local device additionally carries the inline
     * {@code <capability ver="1">} the caller advertises on the one to one offer. The server NACKs a stale
     * device set with {@code error="427"} and echoes the corrected roster, which the caller re offers against;
     * providing the caller's best known device set up front is what lets the server fan the offer to the
     * participants' devices at all.
     *
     * @param self               the local device JID, the call creator, whose user is rostered first
     * @param participantDevices the invited participants keyed by user JID to their known device JIDs, in
     *                           roster order
     * @return the placement group info roster
     */
    private GroupInfoStanza rosterOf(Jid self, Map<Jid, List<Jid>> participantDevices) {
        var entries = new ArrayList<Stanza>(participantDevices.size() + 1);
        var selfDevice = new CallParticipantUserNode.Device(self, Optional.empty(), -1,
                Optional.of(standardCapability()));
        entries.add(CallParticipantUserNode.ofUserDevices(self.toUserJid(), List.of(selfDevice)).toNode());
        for (var entry : participantDevices.entrySet()) {
            var devices = entry.getValue().stream()
                    .map(deviceJid -> new CallParticipantUserNode.Device(deviceJid, Optional.empty(), -1,
                            Optional.<CallCapability>empty()))
                    .toList();
            entries.add(CallParticipantUserNode.ofUserDevices(entry.getKey().toUserJid(), devices).toNode());
        }
        return GroupInfoStanza.ofUsers(null, -1, entries);
    }

    /**
     * Reconciles a group call's membership against the roster a positive offer ack echoed, if any.
     *
     * <p>The selective forwarding unit's positive ack carries a {@code <group_info>} roster decorated with
     * the per participant devices, capabilities, and state the relay assigned; the controller reconciles the
     * call's {@link CallMembership} against it so the membership reflects the unit's view. An ack carrying no
     * roster leaves the placement membership unchanged.
     *
     * @param orchestrated the group call whose membership is reconciled
     * @param ack          the server's offer ack stanza
     */
    private static void reconcileFromAck(OrchestratedCall orchestrated, Stanza ack) {
        var membership = orchestrated.membership().orElse(null);
        if (membership == null) {
            return;
        }
        ack.streamChildren()
                .map(GroupInfoStanza::of)
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(membership::reconcile);
    }

    /**
     * Fans the call key out to every connected participant device as a per participant {@code <enc_rekey>}.
     *
     * <p>The local participant re shares its current call key by sending one unicast {@code <enc_rekey>}
     * stanza per connected participant device, each addressed to that device and carrying a single
     * {@code <enc>} envelope of the wrapped key. The key is wrapped once and encrypted per device through the
     * crypto facade's rekey fanout, which skips a device whose encryption fails rather than aborting the
     * whole rotation. A rekey carries the local device JID as its call creator and an omitted transaction id,
     * since the placement and membership change rounds do not stamp the self rekey transaction counter bounded
     * below {@link RekeyStanza#MAX_TRANSACTION_ID}.
     *
     * @implNote This implementation sends each connected participant device its own
     * {@code <call to="<deviceLid>"><enc_rekey>} carrying one {@code <enc>} (a single 32 byte key, the same
     * plaintext shape as the offer key), with a {@code <device-identity>} attached only on a {@code pkmsg}
     * envelope. The three per domain audio, video, and appdata keys are derived locally after decrypt, not
     * transmitted. The transaction id is omitted here because the self rekey counter origin is not threaded
     * through.
     *
     * @param orchestrated the group call whose key is shared
     * @param self         the local device JID, stamped as the rekey call creator
     * @param callKey      the call key to re share
     */
    private void fanOutGroupRekey(OrchestratedCall orchestrated, Jid self, byte[] callKey) {
        var recipients = connectedParticipantDevices(orchestrated, self);
        if (recipients.isEmpty()) {
            return;
        }
        var plaintext = crypto.wrapCallKey(callKey);
        var envelopes = crypto.encryptRekeyFanout(recipients, plaintext);
        var callId = orchestrated.callId();
        for (var envelope : envelopes) {
            var rekey = envelope.toNode(callId, self, -1);
            host.sendSignaling(wrapInCall(rekey, envelope.recipientDevice(), callId));
        }
        orchestrated.groupOutbound().ifPresent(unit -> unit.fanOfferSent(recipients));
    }

    /**
     * Collects every participant device the local participant fans a rekey out to.
     *
     * <p>Walks the call's membership roster and gathers each device JID of each participant other than the
     * local account, so the local participant's own devices are never sent the rekey. A rekey is addressed
     * unicast to a device JID and Signal encrypted for that device, so a participant the roster lists with
     * no device yet (a placement roster the relay has not yet decorated with its device list) contributes no
     * recipient: the key reaches it on the next reconcile once the relay's {@code <group_info>} broadcast
     * carries the participant's {@code <device>} list, so the rekey fanout follows the connected roster
     * rather than the bare placement roster.
     *
     * @param orchestrated the group call whose roster is walked
     * @param self         the local device JID, whose account is excluded
     * @return the recipient device JIDs, possibly empty
     */
    private static List<Jid> connectedParticipantDevices(OrchestratedCall orchestrated, Jid self) {
        var membership = orchestrated.membership().orElse(null);
        if (membership == null) {
            return List.of();
        }
        var selfUser = self.toUserJid();
        var recipients = new ArrayList<Jid>();
        for (var identity : membership.identities()) {
            if (identity.jid().toUserJid().equals(selfUser)) {
                continue;
            }
            for (var device : identity.devices()) {
                recipients.add(device.jid());
            }
        }
        return List.copyOf(recipients);
    }

    /**
     * Wraps a built action stanza in a {@code <call>} envelope addressed to a recipient device.
     *
     * <p>This is the envelope shim for an action already built as a {@link Stanza} (a per participant rekey),
     * distinct from {@link CallStanza#toCall(CallMessage, Jid, String)} which wraps a typed
     * {@link CallMessage}.
     *
     * @param action the built action stanza to wrap
     * @param to     the recipient device JID
     * @param callId the call identifier, used as the envelope stanza id
     * @return the {@code <call to id>} envelope nesting the action
     */
    private static Stanza wrapInCall(Stanza action, Jid to, String callId) {
        return new StanzaBuilder()
                .description(CallStanza.ELEMENT)
                .attribute("to", to)
                .attribute("id", callId)
                .content(action)
                .build();
    }

    /**
     * Builds the {@code <accept>} action for answering a call.
     *
     * <p>The accept echoes the server allocated {@code <relay>} block as its first child (with each
     * endpoint's client to relay round trip hints stripped) so the server can complete relay allocation,
     * and advertises the callee's standard capability and the offered audio codecs; when the offer carried
     * video it also echoes the video codec descriptor ({@code <video dec="H264" device_orientation="0"/>},
     * no {@code enc}) so the caller learns the callee will decode the video stream. The camera on intent
     * flows to the media plane bring up and the {@code <video>} state announcement rather than to this
     * element.
     *
     * @param callId  the call identifier
     * @param creator the call creator's device JID
     * @param relay   the offered {@code <relay>} block to echo, or {@code null} when none was offered
     * @param video   whether the offered call carried video, so the accept echoes the video codec
     * @return the accept action
     */
    private AcceptStanza buildAccept(String callId, Jid creator, Stanza relay, boolean video) {
        var capabilities = List.of(standardCapability());
        var audioCodecs = standardAudioCodecs();
        var videoCodecs = video ? acceptVideoCodecs() : List.<CallCodecDescriptor>of();
        var acceptRelay = relay == null ? null
                : RelayInfo.of(relay).map(RelayInfo::withoutEndpointRoundTripHints).orElse(null);
        return new AcceptStanza(callId, creator, NET_MEDIUM_ACCEPT, capabilities, audioCodecs, videoCodecs,
                List.of(), null, CallEncOptions.standard(), null, acceptRelay);
    }

    /**
     * Builds the {@code <preaccept>} action acknowledging an inbound offer.
     *
     * <p>When the offer carried video the preaccept echoes the video codec descriptor
     * ({@code <video dec="H264" device_orientation="0" screen_width="0" screen_height="0"/>}, no
     * {@code enc}) alongside the audio so the caller learns the alerting device will decode the video.
     *
     * @param callId  the call identifier
     * @param creator the call creator's device JID
     * @param video   whether the offered call carried video, so the preaccept echoes the video codec
     * @return the preaccept action
     */
    private PreacceptStanza buildPreaccept(String callId, Jid creator, boolean video) {
        var capabilities = List.of(standardCapability());
        var audioCodecs = standardAudioCodecs();
        var videoCodecs = video ? preacceptVideoCodecs() : List.<CallCodecDescriptor>of();
        return new PreacceptStanza(callId, creator, capabilities, audioCodecs, videoCodecs, null,
                CallEncOptions.standard());
    }

    /**
     * Sends an offer and returns the server's synchronous ack.
     *
     * <p>The offer rides {@link OfferAckSender} so the calling virtual thread blocks for the
     * relay bearing ack; a failure to send is surfaced as a nonfatal call exception.
     *
     * @param callId the call identifier, for diagnostics
     * @param self   the local device JID, the envelope sender context
     * @param peer   the peer user JID, the envelope recipient
     * @param offer  the offer action to wrap and send
     * @return the server's ack stanza
     * @throws WhatsAppCallException if the offer could not be sent or no ack arrived
     */
    private Stanza sendOffer(String callId, Jid self, Jid peer, OfferStanza offer) {
        var envelope = CallStanza.toCall(offer, peer, callId);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending offer for call {0} from {1} to {2}", callId, self, peer);
        return offerAckSender.sendOfferAndAwaitAck(envelope);
    }

    /**
     * Applies an offer's synchronous ack to a call, clearing the initial result or ending on a NACK.
     *
     * <p>A positive ack carries the caller's relay block and the denormalised {@code <voip_settings>}
     * engine parameter bundles; the controller records both and clears the
     * {@link CallResult#CALL_OFFER_ACK_NOT_RECEIVED} initial result by folding the offer ack received event
     * into the info manager. The caller does NOT bring up its media plane here: the offer ack relay block
     * carries placeholder credentials the relay rejects before the callee answers, and the finalized
     * credentials arrive in the peer's accept, so the caller defers the bring up to
     * {@link #handlePeerAccept(AcceptStanza, Jid)}; the recorded bundles are fed to the voip param manager at
     * that deferred bring up, since the accept itself carries no {@code <voip_settings>}. A NACK (an ack with
     * an {@code error}) ends the call as unavailable.
     *
     * @implNote This implementation defers media to the accept: the offer ack relay block is allocated before
     * the callee answers and the relay drops an Allocate fired against it, so the relay path comes up only
     * from the accept relay; the caller records the offer ack relay as a reference but brings media up on the
     * accept.
     *
     * @param orchestrated the outbound call the ack belongs to
     * @param ack          the server's ack stanza
     */
    private void applyOfferAck(OrchestratedCall orchestrated, Stanza ack) {
        var callId = orchestrated.callId();
        if (ack.hasAttribute("error")) {
            // Every offer ack error collapses to ServerNack (no 404 or 434 submap as the accept ack has);
            // the call ends as a setup failure carrying that reason rather than UNKNOWN.
            if (Log.INFO) LOGGER.log(Level.INFO, "offer nack for call {0}: error={1}", callId,
                    ack.getAttributeAsString("error", "?"));
            observer.onResult(callId, CallResult.SERVER_NACK);
            tearDown(orchestrated, CallResult.SERVER_NACK.toEndReason(),
                    CallEventType.CALL_OFFER_NACK_RECEIVED);
            return;
        }
        infoUpdater.updateForEvent(callId, CallEventType.CALL_OFFER_ACK_RECEIVED);
        events.emit(CallEventType.CALL_OFFER_ACK_RECEIVED, DataUtils.EMPTY_BYTE_ARRAY);
        ack.getChild("relay").ifPresent(orchestrated::relay);
        orchestrated.offerAckVoipSettings(voipSettingsOf(ack));
        // The offer ack relay block names the caller's relay set (placeholder credentials notwithstanding):
        // begin the relay latency exchange now so the peer learns this side's per relay latencies and the
        // accept time bring up can elect a relay both ends reach rather than the locally fastest one.
        startRelayLatencyExchange(orchestrated, orchestrated.relay().orElse(null));
    }

    /**
     * Recovers the end to end call key from an inbound one to one offer's per device fanout.
     *
     * <p>Walks the offer's key distribution slots and Signal decrypts the first slot that yields the
     * 32 byte key, recording it on the orchestration handle. A group offer carries no key in the
     * offer (the key arrives post join through a rekey), so no key is recovered and none is recorded.
     *
     * @param orchestrated the inbound call the key is recovered for
     * @param offer        the decoded inbound offer
     * @param senderJid    the device JID that authored the offer, the decryption sender
     */
    private void recoverOfferCallKey(OrchestratedCall orchestrated, OfferStanza offer, Jid senderJid) {
        for (var slot : offer.keyDistribution()) {
            var recovered = crypto.decryptCallKey(slot, senderJid);
            if (recovered.isPresent()) {
                orchestrated.callKey(recovered.get());
                return;
            }
        }
    }

    /**
     * Allocates and seeds the membership of a group call rung from an inbound offer.
     *
     * <p>Attaches a fresh {@link CallMembership} to the orchestration handle and, when the offer carries a
     * parseable {@code <group_info>} roster, reconciles the membership against it so the call's participant
     * set is populated from the offer before any {@code <group_update>} arrives. An inbound offer group call
     * that skipped this would carry no membership, and {@link #handleGroupUpdate(GroupUpdateStanza)} would
     * drop every later roster reconcile against its {@code membership == null} guard; seeding here keeps the
     * inbound offer group path symmetric with the placement
     * ({@link #startGroupCall(Jid, Collection, Jid, boolean, MediaStreams)}) path that allocates the
     * membership up front. An offer whose {@code <group_info>} is absent or does not parse leaves an empty
     * membership the first {@code <group_update>} populates.
     *
     * @implNote This implementation seeds the participant set from the offer roster before the first group
     * update: the offer's {@code <group_info>} is the same roster shape the {@code <group_update>} carries, so
     * it is reconciled through the same {@link CallMembership#reconcile(GroupInfoStanza)} the update uses.
     *
     * @param orchestrated the inbound group call whose membership is allocated
     * @param offer        the decoded inbound group offer
     */
    private static void attachOfferMembership(OrchestratedCall orchestrated, OfferStanza offer) {
        var membership = new CallMembership(orchestrated.callId());
        orchestrated.membership(membership);
        offer.groupInfoNode()
                .flatMap(GroupInfoStanza::of)
                .ifPresent(membership::reconcile);
    }

    /**
     * Transitions a call to a new internal state through the guard and fires the state event on success.
     *
     * <p>The guard runs first through
     * {@link CallStateTransition#transition(String, CallLifecycleState)}, and only when it accepts a
     * real change does the controller mirror the new state on the orchestration handle, publish the public
     * projection onto the {@link Call} view, fold the lifecycle event into the result snapshot, and emit
     * the lifecycle event. The state changed event ({@link CallEventType#CALL_STATE_CHANGED}) is fired
     * for every accepted change except the silent {@link CallLifecycleState#LINK} and
     * {@link CallLifecycleState#ENDING} transitions, which the guard treats as event free, and except when
     * the {@code lifecycleEvent} already is {@link CallEventType#CALL_STATE_CHANGED} (a state change with no
     * distinct lifecycle event of its own), so it is emitted once rather than twice. The
     * {@code lifecycleEvent} (for the link transition, the call link state event) is always emitted. A
     * guard rejection or a no op transition to the current state fires no event.
     *
     * @param orchestrated   the call being transitioned
     * @param newState       the target internal state
     * @param lifecycleEvent the lifecycle event this transition raises
     */
    private void transition(OrchestratedCall orchestrated, CallLifecycleState newState,
                            CallEventType lifecycleEvent) {
        var callId = orchestrated.callId();
        var prior = stateMachine.transition(callId, newState).orElse(null);
        if (prior == null || prior == newState) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state {1} -> {2}", callId, prior, newState);
        manageConnectedLonelyTimer(callId, prior, newState);
        orchestrated.state(newState);
        orchestrated.call().setState(newState.toPublic());
        if (newState == CallLifecycleState.CALLING || newState == CallLifecycleState.ACCEPT_SENT) {
            orchestrated.engineContext().ifPresent(CallContext::markStarted);
        }
        infoUpdater.updateForEvent(callId, lifecycleEvent);
        events.emit(lifecycleEvent, DataUtils.EMPTY_BYTE_ARRAY);
        var silent = newState == CallLifecycleState.LINK || newState == CallLifecycleState.ENDING;
        if (!silent && lifecycleEvent != CallEventType.CALL_STATE_CHANGED) {
            events.emit(CallEventType.CALL_STATE_CHANGED, DataUtils.EMPTY_BYTE_ARRAY);
        }
    }

    /**
     * Arms or cancels the connected lonely timer for the state change a transition just made.
     *
     * <p>The scheduler holds no reference to this controller, so the connected lonely timer, which the
     * state guard used to schedule and cancel itself, is now driven from here where the controller drives
     * the transition: entering {@link CallLifecycleState#CONNECTED_LONELY} arms it, and leaving it or
     * entering {@link CallLifecycleState#CALL_ACTIVE} cancels it. This runs only for accepted, state
     * changing transitions (the caller has already filtered a rejected or no op transition), which are
     * never the silent {@link CallLifecycleState#LINK} or {@link CallLifecycleState#ENDING} transitions,
     * matching the exact condition under which the guard formerly fired the seam. The teardown path
     * reaches {@link CallLifecycleState#NONE} through {@link #timers} {@link CallTimerScheduler#cancelAll
     * cancelAll}, so a {@link CallLifecycleState#CONNECTED_LONELY} to {@link CallLifecycleState#NONE}
     * teardown cancels the timer there.
     *
     * @param callId   the identifier of the transitioning call
     * @param prior    the state left
     * @param newState the state entered
     */
    private void manageConnectedLonelyTimer(String callId, CallLifecycleState prior, CallLifecycleState newState) {
        if (newState == CallLifecycleState.CONNECTED_LONELY) {
            armTimer(callId, CallTimerKind.CONNECTED_LONELY);
        } else if (prior == CallLifecycleState.CONNECTED_LONELY || newState == CallLifecycleState.CALL_ACTIVE) {
            timers.cancel(callId, CallTimerKind.CONNECTED_LONELY);
        }
    }

    /**
     * Arms the given timer for a call, supplying the fire action the scheduler runs.
     *
     * <p>The scheduler holds no reference to this controller; it is a pure timer mechanism the controller
     * drives, so this maps each {@link CallTimerKind} to the controller behavior a fired timer runs: the
     * lonely timeouts end the call with {@link CallEndReason#TIMEOUT}, the heartbeat sends a heartbeat, and
     * the periodic watchdog sweeps a group call's unanswered offers. Kinds whose callbacks are owned by
     * other units are armed with an inert action.
     *
     * @param callId the identifier of the call whose timer is armed
     * @param kind   the timer to arm
     */
    private void armTimer(String callId, CallTimerKind kind) {
        Runnable action = switch (kind) {
            case CALLER_LONELY, CONNECTED_LONELY -> () -> endCall(callId, CallEndReason.TIMEOUT);
            case HEARTBEAT -> () -> sendHeartbeat(callId);
            case PERIODIC -> () -> groupOutbound(callId).ifPresent(GroupCallOutbound::sweepUnansweredOffers);
            default -> () -> {
            };
        };
        timers.arm(callId, kind, action);
    }

    /**
     * Tears a call down: cancels its timers, closes its media plane, ends its state, and frees its handle.
     *
     * <p>Sets the public end reason, drives the internal state to {@link CallLifecycleState#NONE}, emits the
     * supplied end event, hands the finished engine {@link CallContext} to the
     * {@linkplain #bindCallLogSink(BiConsumer) bound} outbound call log sink, reports the call identifier to
     * the {@linkplain #bindTeardownSink(Consumer) bound} service teardown sink so the call's service level
     * state is freed and its end of call WAM telemetry drains, cancels every per call timer, closes the
     * media plane session if one is up, and removes the orchestration handle. The path to
     * {@link CallLifecycleState#NONE} depends on the current state: from a setup or link state the call first
     * passes through the silent {@link CallLifecycleState#ENDING} transition and then the silent
     * {@link CallLifecycleState#ENDING} to {@link CallLifecycleState#NONE} teardown, both event free under the
     * guard; from an in call state ({@link CallLifecycleState#CALL_ACTIVE} or
     * {@link CallLifecycleState#CONNECTED_LONELY}) the guard forbids the {@link CallLifecycleState#ENDING} hop, so
     * the teardown takes the legal direct {@link CallLifecycleState#NONE} edge those states permit. The call log
     * sink and the service teardown sink each fire once per call after the {@link CallLifecycleState#NONE}
     * transition has closed out the context's durations; they are independent end of call outputs, and a
     * call log, teardown sink, or media plane failure is swallowed so a teardown always completes.
     *
     * @implNote This implementation transitions to {@link CallLifecycleState#NONE}, cancels every timer, and
     * frees the context. The {@link CallLifecycleState#ENDING} hop is attempted only from the states the guard
     * accepts it from; the two closed in call states reach {@link CallLifecycleState#NONE} directly, because
     * the guard's closed set check rejects an in call to {@link CallLifecycleState#ENDING} transition before
     * its silent ending shortcut. The end event the controller emits is the lifecycle event for the cause of
     * the teardown; the public {@link Call} view is moved to {@link CallState#ENDED} through the
     * {@link CallLifecycleState#NONE} projection.
     *
     * @param orchestrated the call being torn down
     * @param reason       the end reason published on the public call view
     * @param endEvent     the lifecycle event for the cause of the teardown
     */
    private void tearDown(OrchestratedCall orchestrated, CallEndReason reason, CallEventType endEvent) {
        var callId = orchestrated.callId();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "tearing down call {0}, reason={1}, event={2}", callId, reason, endEvent);
        orchestrated.call().setEndReason(reason);
        infoUpdater.updateForEvent(callId, endEvent);
        events.emit(endEvent, DataUtils.EMPTY_BYTE_ARRAY);

        var current = orchestrated.state();
        if (current != CallLifecycleState.CALL_ACTIVE && current != CallLifecycleState.CONNECTED_LONELY) {
            stateMachine.transition(callId, CallLifecycleState.ENDING);
        }
        stateMachine.transition(callId, CallLifecycleState.NONE);
        orchestrated.state(CallLifecycleState.NONE);
        orchestrated.call().setState(CallState.ENDED);
        events.emit(CallEventType.CALL_STATE_CHANGED, DataUtils.EMPTY_BYTE_ARRAY);

        notifyEnded(orchestrated, reason);

        timers.cancelAll(callId);
        orchestrated.controls().ifPresent(controls -> {
            try {
                controls.close();
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "in-call control close failed for call " + callId, e);
            }
        });
        orchestrated.mediaSession().ifPresent(session -> {
            try {
                session.close();
            } catch (RuntimeException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "media plane close failed for call " + callId, e);
            }
        });
        registry.release(callId);
        calls.remove(callId);
    }

    /**
     * Reports a torn down call's end to the {@link CallLifecycleObserver} once, at the single ending
     * transition.
     *
     * <p>Resolves the call's finished engine {@link CallContext} from its orchestration handle (possibly
     * {@code null} for a test registry that allocated none) and hands it, the call identifier, and the
     * terminal {@link CallEndReason} to the observer, so the call service frees the call's service level
     * state, drains its end of call WAM telemetry, and pushes the call log in one place; a {@code null}
     * context lets the service skip the call log while still freeing the call. Any failure the observer
     * raises is caught and logged so the teardown that called this always completes; the service level drain
     * is best effort.
     *
     * @implNote This implementation reports after the {@link CallLifecycleState#NONE} transition and the
     * public {@link CallState#ENDED} projection, so the context's duration accumulators are closed out and
     * the call view the service reads carries the terminal end reason when its WAM Call event is built.
     *
     * @param orchestrated the call being torn down, accessed under its lock
     * @param reason       the terminal end reason reported to the observer
     */
    private void notifyEnded(OrchestratedCall orchestrated, CallEndReason reason) {
        var callId = orchestrated.callId();
        try {
            observer.onEnded(callId, orchestrated.engineContext().orElse(null), reason);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "lifecycle observer onEnded failed for call " + callId, e);
        }
    }

    /**
     * Returns the orchestration handle for a tracked call.
     *
     * @param callId the call identifier
     * @return the orchestration handle
     * @throws IllegalArgumentException if no call exists for {@code callId}
     */
    private OrchestratedCall require(String callId) {
        var orchestrated = calls.get(callId);
        if (orchestrated == null) {
            throw new IllegalArgumentException("No active call for id " + callId);
        }
        return orchestrated;
    }

    /**
     * Generates a fresh 32 character call identifier.
     *
     * <p>Draws sixteen cryptographically strong random bytes from the host and hex encodes each byte's
     * high nibble then its low nibble through the mixed case translation table, so the identifier's case
     * varies across its characters.
     *
     * @implNote This implementation hex encodes sixteen random bytes through the table
     * {@code "0123456789ABCDEF0123456789abcdef"} into a 32 character identifier. The bytes are drawn from
     * {@link VoipHostApi#randomBytes(int)}, which is backed by a cryptographically strong source.
     *
     * @return the generated call identifier
     */
    private String generateCallId() {
        var random = host.randomBytes(CALL_ID_RANDOM_BYTES);
        var chars = new char[CALL_ID_RANDOM_BYTES * 2];
        for (var i = 0; i < random.length; i++) {
            var unsigned = random[i] & 0xff;
            chars[i * 2] = CALL_ID_HEX[unsigned >>> 4];
            chars[i * 2 + 1] = CALL_ID_HEX[unsigned & 0x0f];
        }
        return new String(chars);
    }

    /**
     * Returns the caller's standard capability advertisement.
     *
     * <p>Wraps the client's self advertisement bytes in a {@code <capability ver="1">} element, the
     * capability set every device advertises.
     *
     * @return the standard capability
     */
    private static CallCapability standardCapability() {
        return new CallCapability(CAPABILITY_VERSION, VoipCapabilities.standard().serialize());
    }

    /**
     * Returns the offered audio formats: Opus at the narrowband and wideband sampling rates.
     *
     * <p>The live captures show the offer advertising {@code <audio enc="opus" rate="8000"/>} and
     * {@code <audio enc="opus" rate="16000"/>} as flat elements, so the offer advertises the Opus codec
     * at each rate.
     *
     * @return the offered audio format descriptors
     */
    private static List<CallCodecDescriptor> standardAudioCodecs() {
        return List.of(
                CallCodecDescriptor.audio(AUDIO_CODEC_OPUS, AUDIO_RATE_NARROWBAND),
                CallCodecDescriptor.audio(AUDIO_CODEC_OPUS, AUDIO_RATE_WIDEBAND));
    }

    /**
     * Returns the offered video format for a video call: a single flat {@code <video>} element
     * advertising H.264.
     *
     * <p>Every video offer carries one
     * {@code <video dec="H264" enc="h.264" device_orientation="0" screen_width="0" screen_height="0"/>}
     * element, with the device orientation and screen geometry zero until the camera starts, so the
     * offer advertises that single element rather than a per codec list.
     *
     * @return the offered video format descriptor
     */
    private static List<CallCodecDescriptor> standardVideoCodecs() {
        return List.of(CallCodecDescriptor.video(VIDEO_DEC_TOKEN, VIDEO_ENC_NAME, 0, 0, 0));
    }

    /**
     * Returns the callee video codec descriptor echoed in an accept for a video call.
     *
     * <p>The accept advertises the negotiated decode codec without an {@code enc} encode name or screen
     * geometry: the answer carries a flat {@code <video dec="H264" device_orientation="0"/>}, the lighter
     * form the offer's full descriptor is trimmed to once the codec is agreed.
     *
     * @return the accept video format descriptor
     */
    private static List<CallCodecDescriptor> acceptVideoCodecs() {
        return List.of(CallCodecDescriptor.video(VIDEO_DEC_TOKEN, null, 0, -1, -1));
    }

    /**
     * Returns the callee video codec descriptor echoed in a preaccept for a video call.
     *
     * <p>The preaccept advertises the decode codec with the screen geometry but without an {@code enc}
     * encode name: the early ring ack carries a flat
     * {@code <video dec="H264" device_orientation="0" screen_width="0" screen_height="0"/>}.
     *
     * @return the preaccept video format descriptor
     */
    private static List<CallCodecDescriptor> preacceptVideoCodecs() {
        return List.of(CallCodecDescriptor.video(VIDEO_DEC_TOKEN, null, 0, 0, 0));
    }

    /**
     * Returns whether any offer key distribution slot is a prekey message.
     *
     * <p>An offer whose any per device {@code <enc>} is a {@code pkmsg} bootstraps a Signal session, so the
     * offer must carry the local device identity; this reports whether that is the case so the offer
     * builder attaches the identity only when needed.
     *
     * @param keyDistribution the per device key fanout
     * @return {@code true} when any slot is an encrypted prekey message envelope
     */
    private static boolean anyPreKeyMessage(List<CallKeyDistribution> keyDistribution) {
        return keyDistribution.stream()
                .filter(CallKeyDistribution::isEncrypted)
                .anyMatch(slot -> slot.typeValue().map("pkmsg"::equals).orElse(false));
    }

    /**
     * Bundles a call's in call control units, built once the call is answerable and closed on teardown.
     *
     * <p>This is the per call holder the controller stores on a {@link OrchestratedCall} once the call
     * becomes answerable: the four signaling plane control units the public in call control methods and the
     * inbound action handlers drive, all wired onto a single {@link ControlEventBridge} over the shared host
     * bus and a single {@link CallSignalingSender}, plus the three application data plane control units
     * (reactions, live transcription, IMU) when the call's media plane carries an {@link AppDataController},
     * plus the two call link control units (the call link controller and the waiting room controller) when
     * the call was joined through a call link token. The three app data units are {@code null} for a call
     * whose transport carries no app data plane (or that is torn down before its media plane is up), and the
     * two call link units are {@code null} for a call not joined through a link, so consumers must null check
     * them; the four signaling plane units are always present. Of the bundled units only {@link MuteController}
     * owns timers and is {@link AutoCloseable}, so {@link #close()} closes it and the rest are released with
     * the handle; the app data units own no timers of their own (the reaction clear and retransmission timers
     * live on the {@link AppDataController}, closed with the media session), and the two call link units own
     * no timers either, so closing the holder simply drops them.
     *
     * @implNote This implementation groups the per call control units bound to an answerable call; it carries
     * no state of its own beyond the unit references and a single closeable. The app data units are nullable
     * because they are built only once the media plane exposes an {@link AppDataController} (reactions and
     * transcripts ride the app data side channel, not signaling), unlike the four signaling plane units which
     * are always built when the bus is bound. The two call link units are nullable because they are built only
     * on the call link join path ({@link #joinCallLink(Jid, String, CallLinkMedia, boolean, MediaStreams)})
     * and folded into the bundle through {@link #withCallLink(CallLinkController, WaitingRoomController)} after
     * the call is answered.
     * @param mute          the mute control unit; never {@code null}
     * @param video         the video state control unit; never {@code null}
     * @param screenShare   the screen share control unit; never {@code null}
     * @param raiseHand     the raise hand control unit; never {@code null}
     * @param reaction      the reaction control unit, or {@code null} when the call carries no app data plane
     * @param transcription the live transcription control unit, or {@code null} when the call carries no
     *                      app data plane
     * @param imu           the IMU control unit, or {@code null} when the call carries no app data plane
     * @param callLink      the call link control unit, or {@code null} when the call was not joined through a
     *                      call link token
     * @param waitingRoom   the waiting room control unit, or {@code null} when the call was not joined through
     *                      a call link token
     */
    record CallControls(MuteController mute, VideoStateController video,
                              ScreenShareController screenShare, RaiseHandController raiseHand,
                              ReactionController reaction, TranscriptionController transcription,
                              ImuDataController imu, CallLinkController callLink,
                              WaitingRoomController waitingRoom)
            implements AutoCloseable {
        /**
         * Canonicalizes the holder over its control units.
         *
         * <p>The four signaling plane units are required; the three app data units and the two call link
         * units are optional and may be {@code null} for a call whose transport carries no app data plane or
         * that was not joined through a call link token.
         *
         * @throws NullPointerException if any of {@code mute}, {@code video}, {@code screenShare}, or
         *                              {@code raiseHand} is {@code null}
         */
        CallControls {
            Objects.requireNonNull(mute, "mute cannot be null");
            Objects.requireNonNull(video, "video cannot be null");
            Objects.requireNonNull(screenShare, "screenShare cannot be null");
            Objects.requireNonNull(raiseHand, "raiseHand cannot be null");
        }

        /**
         * Returns a copy of this bundle carrying the given call link and waiting room control units.
         *
         * <p>The call link join builds the two link units before the call is answered, then answers the call,
         * which builds this base bundle; this folds the two link units into the bundle so they are held for
         * the call's lifetime and dropped on teardown with the rest. The four signaling plane units and the
         * three app data units are carried over unchanged.
         *
         * @param callLink    the call link control unit to carry
         * @param waitingRoom the waiting room control unit to carry
         * @return a copy of this bundle carrying the two link units
         */
        CallControls withCallLink(CallLinkController callLink, WaitingRoomController waitingRoom) {
            return new CallControls(mute, video, screenShare, raiseHand, reaction, transcription, imu,
                    callLink, waitingRoom);
        }

        /**
         * Returns the call's reaction control unit, if the call carries an app data plane.
         *
         * @return an {@link Optional} with the reaction control unit, or empty when the call carries no
         *         app data plane
         */
        Optional<ReactionController> reactionControl() {
            return Optional.ofNullable(reaction);
        }

        /**
         * Closes the call's control units, shutting the mute controller's timers down.
         *
         * <p>Only {@link MuteController} owns a scheduler and is closeable; the video, screen share, and
         * raise hand units own no timers and need no shutdown. The reaction, transcription, and IMU units
         * own no timers of their own either; the reaction clear and retransmission timers live on the
         * {@link AppDataController}, which the media session closes on teardown, so the app data units are
         * simply dropped with this holder.
         */
        @Override
        public void close() {
            mute.close();
        }
    }
}
