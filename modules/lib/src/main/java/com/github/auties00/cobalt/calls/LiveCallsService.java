package com.github.auties00.cobalt.calls;

import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.calls.engine.event.CallEvent;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.telemetry.CallResult;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.device.DeviceService;
import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import com.github.auties00.cobalt.calls.engine.CallLifecycleObserver;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.telemetry.CallLogSync;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.event.LiveCallEventBus;
import com.github.auties00.cobalt.calls.signaling.receive.CallAckOutcome;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.session.OfferNoticeStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.listener.linked.LinkedCallOfferNoticeListener;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.util.ScheduledTask;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.call.CallInteraction;
import com.github.auties00.cobalt.wire.linked.call.CallLink;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.wire.linked.call.CallState;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CallEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.JoinableCallEventBuilder;
import com.github.auties00.cobalt.wire.wam.event.PreCallUserJourneyChatThreadEventBuilder;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingActivity;
import com.github.auties00.cobalt.wire.wam.type.CallFromUi;
import com.github.auties00.cobalt.wire.wam.type.CallNetworkMedium;
import com.github.auties00.cobalt.wire.wam.type.CallResultType;
import com.github.auties00.cobalt.wire.wam.type.CallSide;
import com.github.auties00.cobalt.wire.wam.type.CallSizeType;
import com.github.auties00.cobalt.wire.wam.type.CallTransportType;
import com.github.auties00.cobalt.wire.wam.type.PreCallActionType;
import com.github.auties00.cobalt.wire.wam.type.SubSurface;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import com.github.auties00.cobalt.calls.telemetry.CallStats;

/**
 * Live implementation of {@link CallsService} that coordinates one client's call activity over the
 * WhatsApp voip engine.
 *
 * <p>This service is the per client owner of the call subsystem: there is exactly one instance per
 * {@link LinkedWhatsAppClient}, constructed eagerly and held as a private final field, before the
 * stanza stream service that registers the inbound signaling receiver. It owns the registry of in flight
 * {@link CallRuntime} sessions, and it owns the two host boundary engine pieces the public surface
 * drives: the {@link LiveCallEventBus} that fans host facing events out to the registered listeners, and
 * the {@link LifecycleController} the public call methods delegate to. The at most two engine call
 * contexts are held by the engine's own {@link CallManager}, assembled into the controller rather
 * than owned here. The client's call control methods are thin delegators onto this service, and the call
 * signaling receivers forward every decoded inbound action to it.
 *
 * <p>Where the engine controller runs the call's signaling and media state machine, this service owns the
 * host API translation around it: it builds the public {@link Call} view, registers the
 * {@link CallRuntime} that owns the application media streams and the telemetry accumulator before
 * delegating the engine work, emits a pre call user journey funnel event as a call is placed, drains a
 * call's telemetry into a WAM Call event when it unregisters, and fans the call ended event out to
 * listeners. A placed or accepted call sits in {@link CallState#RINGING}
 * or {@link CallState#CONNECTING} until the engine wires its media plane and is terminated by either a
 * local {@link #terminate(String, CallEndReason)} or a peer termination.
 *
 * @implNote This implementation fans host facing events out to listeners directly, one virtual thread per
 * listener, rather than through a shared platform event queue. The at most two engine call contexts are held
 * by the engine's {@link CallManager}, assembled into the controller rather than owned here; the listener
 * fan out, the registry, and the inline end of call WAM emission are pure Java.
 */
public final class LiveCallsService implements CallsService, CallLifecycleObserver {
    /**
     * The logger for {@link LiveCallsService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCallsService.class);

    /**
     * The window within which an {@code <offer_notice>} is still acted on; a notice whose original offer
     * is older than this is dropped, and a recorded notice is purged from the call history after the same
     * window elapses.
     *
     * @implNote This implementation uses a 45 second window, the value WhatsApp applies both as the offer
     * staleness threshold and as the delay after which a recorded notice is removed from the call collection.
     */
    private static final Duration OFFER_NOTICE_STALENESS = Duration.ofSeconds(45);

    /**
     * Holds the owning client, used to resolve the local self {@link Jid}, resolve a peer's device list,
     * read the call store, and reach the listener registry for the call ended fan out.
     */
    private final LinkedWhatsAppClient whatsapp;

    /**
     * Tracks live call runtimes keyed by their unique call identifier.
     *
     * <p>An entry is added on
     * {@link #placeCall(Jid, AudioOutput, AudioInput, VideoOutput, VideoInput)} or
     * {@link #accept(IncomingCall, AudioOutput, AudioInput, VideoOutput, VideoInput)}
     * and removed by {@link #unregister(String)} when the call reaches {@link CallState#ENDED}. This is the
     * host's view of the at most two engine call contexts the {@link CallManager} holds.
     */
    private final ConcurrentHashMap<String, CallRuntime> activeCalls = new ConcurrentHashMap<>();

    /**
     * Holds the WAM service used to commit the per call telemetry event, or {@code null} when telemetry is
     * disabled for this client.
     */
    private final WamService wamService;

    /**
     * Holds the {@link MessageService} that owns the per device fanout encryption and addressing mode
     * resolution for outbound call offers and the Signal decryption for inbound key envelopes.
     */
    private final MessageService messageService;

    /**
     * Fans host facing call events out to the registered listeners on their own virtual threads.
     */
    private final LiveCallEventBus eventBus;

    /**
     * Drives a call through its phases over the engine units, or {@code null} when this service is built
     * without an engine.
     *
     * <p>The lifecycle controller is the action surface the public call methods delegate to and the sink the
     * inbound seam forwards into. The production client always injects an assembled
     * controller through the full dependency constructor; the constructors without a controller exist only for
     * the call API and inbound routing unit tests that exercise the host translation without a live engine.
     */
    private final LifecycleController lifecycleController;

    /**
     * The app state call log collaborator each ended call's finished context is pushed to, or {@code null}
     * for a build without one (the unit tests, which observe no ended call outcome).
     *
     * <p>Fired from {@link #onEnded(String, CallContext, CallEndReason)} when the engine reports a call
     * ended, so the {@code call_log} app state push runs at the same teardown moment the WAM stats drain
     * does; the two are independent end of call outputs the service orchestrates from one observer callback.
     */
    private final CallLogSync callLogSync;

    /**
     * Identifies the WAM app session this service runs under, stamped on every pre call user journey event.
     *
     * <p>WhatsApp tags each pre call funnel beacon with the app session identifier of the launch it was
     * emitted in so a whole journey correlates back to one session. This is a stable random identifier
     * minted once per service instance, matching WhatsApp's per app launch session id: there is exactly one
     * calls service per {@link LinkedWhatsAppClient} connection, so one identifier per instance is the
     * connection's app session.
     */
    private final String appSessionId = randomSessionId();

    /**
     * Constructs a service bound to the given client, WAM service, and {@link MessageService}, building a
     * fresh event bus.
     *
     * <p>A fresh {@link LiveCallEventBus} is built for this service since none is supplied. The
     * {@link LifecycleController} is left unset by this constructor and is supplied through
     * {@link #LiveCallsService(LinkedWhatsAppClient, WamService, MessageService, LifecycleController)}
     * once its collaborating engine units are assembled.
     *
     * @param whatsapp       the owning client
     * @param wamService     the WAM telemetry service used for end of call events, or {@code null} when
     *                       telemetry is disabled for this client
     * @param messageService the {@link MessageService} used to build, encrypt, and ship the outbound offer
     *                       per peer and to decrypt the inbound key envelope
     * @throws NullPointerException if {@code whatsapp} or {@code messageService} is {@code null}
     */
    public LiveCallsService(LinkedWhatsAppClient whatsapp, WamService wamService,
                             MessageService messageService) {
        this(whatsapp, wamService, messageService, null);
    }

    /**
     * Constructs a service bound to the given client, WAM service, {@link MessageService}, and an
     * assembled {@link LifecycleController}, building a fresh event bus.
     *
     * <p>This constructor builds a fresh {@link LiveCallEventBus} for the service. It is the seam the
     * call API and inbound routing unit tests build the service through, where the controller carries no
     * in call control units and the bus is therefore not shared with an engine. The production client
     * instead uses
     * {@link #LiveCallsService(LinkedWhatsAppClient, WamService, MessageService, LifecycleController, LiveCallEventBus)}
     * so the engine's control units and this service publish onto one shared bus.
     *
     * @param whatsapp            the owning client
     * @param wamService          the WAM telemetry service, or {@code null} when telemetry is disabled
     * @param messageService      the {@link MessageService} used for offer encryption and key decryption
     * @param lifecycleController the assembled engine lifecycle controller, or {@code null} when this service
     *                            is built without an engine
     * @throws NullPointerException if {@code whatsapp} or {@code messageService} is {@code null}
     */
    public LiveCallsService(LinkedWhatsAppClient whatsapp, WamService wamService, MessageService messageService,
                             LifecycleController lifecycleController) {
        this(whatsapp, wamService, messageService, lifecycleController, new LiveCallEventBus(whatsapp));
    }

    /**
     * Constructs a service over a pre-built {@link LifecycleController} and a shared
     * {@link LiveCallEventBus}.
     *
     * <p>This is the seam the call API and inbound routing unit tests build the service through: the
     * controller is built directly over recording fakes and passed in, and the bus is not shared with a
     * live engine. The controller carries no service sinks in this path (a test controller reports its
     * teardown, connected, and result outcomes to no ops), which the tests do not exercise. The production
     * client instead uses
     * {@link #LiveCallsService(LinkedWhatsAppClient, WamService, MessageService, MessageEncryption, DeviceService, LinkedWhatsAppStore, ABPropsService, LiveCallEventBus, CallLogSync)},
     * which assembles the controller and threads this service's sinks into it at construction.
     *
     * @param whatsapp            the owning client
     * @param wamService          the WAM telemetry service, or {@code null} when telemetry is disabled
     * @param messageService      the {@link MessageService} used for offer encryption and key decryption
     * @param lifecycleController the pre-built engine lifecycle controller, or {@code null} when this service
     *                            is built without an engine
     * @param eventBus            the shared host event bus that gates and fans out the host facing call
     *                            events
     * @throws NullPointerException if {@code whatsapp}, {@code messageService}, or {@code eventBus} is
     *                              {@code null}
     */
    public LiveCallsService(LinkedWhatsAppClient whatsapp, WamService wamService, MessageService messageService,
                             LifecycleController lifecycleController, LiveCallEventBus eventBus) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.wamService = wamService;
        this.messageService = Objects.requireNonNull(messageService, "messageService cannot be null");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
        this.lifecycleController = lifecycleController;
        this.callLogSync = null;
    }

    /**
     * Constructs the production service, assembling its own engine lifecycle controller.
     *
     * <p>This is the full dependency constructor the client uses: it builds the controller through
     * {@link LifecycleController#create}, passing itself as the {@link CallLifecycleObserver}, so the engine
     * reports its media connected, result, and ended outcomes back into this service as a plain constructor
     * dependency with no post construction wiring step. The one {@link LiveCallEventBus} is shared with the
     * engine so the host facing events the in call control units emit and the call ended events this service
     * emits are gated and fanned out by the same bus. The {@code messageEncryption}, {@code deviceService},
     * {@code store}, and {@code abPropsService} are used only to assemble the controller and are not
     * retained.
     *
     * @param whatsapp          the owning client
     * @param wamService        the WAM telemetry service, or {@code null} when telemetry is disabled
     * @param messageService    the {@link MessageService} used for offer encryption and key decryption
     * @param messageEncryption the encryption service the call key crypto wraps the key with
     * @param deviceService     the device service the call key crypto ensures sessions through
     * @param store             the store supplying the local ADV signed device identity
     * @param abPropsService    the AB props service the engine feature gate reads its calling flags from
     * @param eventBus          the shared host event bus that gates and fans out the host facing call events
     * @param callLogSync       the app state call log collaborator each ended call's finished context is
     *                          pushed to
     * @throws NullPointerException if {@code whatsapp}, {@code messageService}, or {@code eventBus} is
     *                              {@code null}
     */
    public LiveCallsService(LinkedWhatsAppClient whatsapp, WamService wamService, MessageService messageService,
                             MessageEncryption messageEncryption, DeviceService deviceService,
                             LinkedWhatsAppStore store, ABPropsService abPropsService, LiveCallEventBus eventBus,
                             CallLogSync callLogSync) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.wamService = wamService;
        this.messageService = Objects.requireNonNull(messageService, "messageService cannot be null");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
        this.callLogSync = callLogSync;
        this.lifecycleController = LifecycleController.create(whatsapp, messageEncryption, messageService,
                deviceService, store, abPropsService, eventBus, this);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation stamps the connected instant through {@link #markCallConnected(String)}.
     */
    @Override
    public void onConnected(String callId) {
        markCallConnected(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation records the result on the call's telemetry through
     * {@link #recordCallResult(String, CallResult)}.
     */
    @Override
    public void onResult(String callId, CallResult result) {
        recordCallResult(callId, result);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation pushes the app state call log first, when a context and a
     * {@link CallLogSync} are both present, then frees the call's service level state and drains its end of
     * call WAM telemetry through {@link #unregister(String)}. The two are independent end of call outputs the
     * service orchestrates from this one callback; the order matches the former separate call log and
     * teardown sinks.
     */
    @Override
    public void onEnded(String callId, CallContext context, CallEndReason reason) {
        if (context != null && callLogSync != null) {
            callLogSync.recordEndOfCall(context, reason);
        }
        unregister(callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Call placeCall(Jid peer, AudioOutput audioOut, AudioInput audioIn,
                          VideoOutput videoOut, VideoInput videoIn) {
        Objects.requireNonNull(peer, "peer cannot be null");
        Objects.requireNonNull(audioOut, "audioOut cannot be null");
        Objects.requireNonNull(audioIn, "audioIn cannot be null");
        if (peer.hasGroupOrCommunityServer()) {
            throw new IllegalArgumentException(
                    "placeCall is for one-to-one calls; use placeGroupCall for a group or community JID: "
                            + peer);
        }
        var self = requireSelfJid();
        var video = videoOut != null;
        // WhatsApp addresses 1:1 call signaling by LID: resolve the peer to its LID and fan out over its
        // LID device JIDs. A peer with no resolvable LID aborts here rather than sending a phone number
        // offer the server rejects with error="439".
        var addressing = messageService.resolveCallPeerAddressing(peer);
        var streams = mediaStreams(audioOut, audioIn, videoOut, videoIn);
        var call = engine().startCall(self, addressing.peer(), addressing.peerDevices(), video, streams);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "placed 1:1 call {0}, video={1}, devices={2}", call.callId(), video, addressing.peerDevices().size());
        emitPreCallJourney(call.callId(), video ? PreCallActionType.CLICK_VIDEO_CALL
                        : PreCallActionType.CLICK_AUDIO_CALL, CallSizeType.ONE_TO_ONE, video,
                SubSurface.CHAT_HEADER, 2L, null, null);
        if (notifyIfEndedDuringPlacement(call, addressing.peer())) {
            return call;
        }
        registerRuntime(call, CallSide.CALLER, video, audioOut, audioIn, videoOut, videoIn);
        return call;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Call placeGroupCall(Set<Jid> peers, Jid groupJid, AudioOutput audioOut,
                               AudioInput audioIn, VideoOutput videoOut, VideoInput videoIn) {
        Objects.requireNonNull(peers, "peers cannot be null");
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(audioOut, "audioOut cannot be null");
        Objects.requireNonNull(audioIn, "audioIn cannot be null");
        if (peers.isEmpty()) {
            throw new IllegalArgumentException("peers cannot be empty");
        }
        var self = requireSelfJid();
        var video = videoOut != null;
        var streams = mediaStreams(audioOut, audioIn, videoOut, videoIn);
        var participantDevices = new LinkedHashMap<Jid, List<Jid>>();
        for (var peer : peers) {
            var addressing = messageService.resolveCallPeerAddressing(peer);
            participantDevices.put(addressing.peer(), addressing.peerDevices());
        }
        var call = engine().startGroupCall(self, participantDevices, groupJid, video, streams);
        var callSize = (long) (peers.size() + 1);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "placed group call {0}, video={1}, peer_count={2}", call.callId(), video, peers.size());
        emitPreCallJourney(call.callId(), PreCallActionType.CLICK_START_CALL, CallSizeType.LGC, video,
                SubSurface.CHAT_HEADER, callSize, callSize, false);
        if (notifyIfEndedDuringPlacement(call, groupJid)) {
            return call;
        }
        registerRuntime(call, CallSide.CALLER, video, audioOut, audioIn, videoOut, videoIn);
        return call;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation resolves the local self JID, derives the join with video flag from
     * whether a video source was supplied, bundles the streams, and delegates the link query and join
     * handshake and the answer to {@link LifecycleController#joinCallLink(Jid, String, CallLinkMedia,
     * boolean, MediaStreams)}, which applies the call link feature gate. The runtime is registered as a
     * caller side telemetry session, matching a placed group call.
     */
    @Override
    public Call joinCallLink(String token, CallLinkMedia media, AudioOutput audioOut, AudioInput audioIn,
                             VideoOutput videoOut, VideoInput videoIn) {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(media, "media cannot be null");
        Objects.requireNonNull(audioOut, "audioOut cannot be null");
        Objects.requireNonNull(audioIn, "audioIn cannot be null");
        var self = requireSelfJid();
        var video = videoOut != null;
        var streams = mediaStreams(audioOut, audioIn, videoOut, videoIn);
        var call = engine().joinCallLink(self, token, media, video, streams);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "joined call link, call={0}, media={1}, video={2}", call.callId(), media, video);
        emitPreCallJourney(call.callId(), PreCallActionType.CLICK_CALL_LINK, CallSizeType.CALL_LINK, video,
                null, null, null, null);
        registerRuntime(call, CallSide.CALLER, video, audioOut, audioIn, videoOut, videoIn);
        return call;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation mirrors {@link #accept(IncomingCall, AudioOutput, AudioInput, VideoOutput,
     * VideoInput)}: it derives the join with video flag from whether a video source was supplied, bundles the
     * streams, and delegates to {@link LifecycleController#joinGroupCall(String, boolean,
     * MediaStreams)}, which sends the {@code <lobby>} then the {@code <accept>} for the call the engine
     * already tracks from its inbound offer. The runtime is registered as a callee side telemetry session,
     * matching an accepted call; no pre call journey beacon is emitted, since the offer was already received.
     */
    @Override
    public Call joinGroupCall(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                              VideoOutput videoOut, VideoInput videoIn) {
        Objects.requireNonNull(offer, "offer cannot be null");
        Objects.requireNonNull(audioOut, "audioOut cannot be null");
        Objects.requireNonNull(audioIn, "audioIn cannot be null");
        var video = videoOut != null;
        var streams = mediaStreams(audioOut, audioIn, videoOut, videoIn);
        var call = engine().joinGroupCall(offer.callId(), video, streams);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "joined group call {0}, video={1}", call.callId(), video);
        registerRuntime(call, CallSide.CALLEE, video, audioOut, audioIn, videoOut, videoIn);
        return call;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Call accept(IncomingCall offer, AudioOutput audioOut, AudioInput audioIn,
                       VideoOutput videoOut, VideoInput videoIn) {
        Objects.requireNonNull(offer, "offer cannot be null");
        Objects.requireNonNull(audioOut, "audioOut cannot be null");
        Objects.requireNonNull(audioIn, "audioIn cannot be null");
        var video = videoOut != null;
        var streams = mediaStreams(audioOut, audioIn, videoOut, videoIn);
        var call = engine().acceptCall(offer.callId(), video, streams);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "accepted call {0}, video={1}", call.callId(), video);
        registerRuntime(call, CallSide.CALLEE, video, audioOut, audioIn, videoOut, videoIn);
        return call;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reject(IncomingCall offer, CallEndReason reason) {
        Objects.requireNonNull(offer, "offer cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "rejecting call {0}, reason={1}", offer.callId(), reason);
        engine().rejectCall(offer.callId(), offer.peer(), reason);
        whatsapp.store().chatStore().removeCall(offer.callId());
        notifyEnded(offer.callId(), offer.peer(), reason.wireValue());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation ends the call through the engine
     * ({@link LifecycleController#endCall(String, CallEndReason)}, which sends the terminate and runs
     * the engine teardown) and then fans the {@code onCallEnded} event out through
     * {@link #notifyEnded(String, Jid, String)} with the call creator and wire reason. The host end event is
     * fired on the local hangup path here to match {@link #reject(IncomingCall, CallEndReason)} and the
     * inbound terminate path, so the application observes the call ending regardless of who hung up. The
     * engine teardown drives the registry removal, the runtime stream and media session shutdown through
     * {@link CallRuntime#end(CallEndReason)}, and the end of call WAM drain through the teardown sink bound
     * to {@link #unregister(String)}, so this method only adds the host listener fan out the slim engine event
     * sink cannot carry.
     */
    @Override
    public void terminate(String callId, CallEndReason reason) {
        Objects.requireNonNull(reason, "reason cannot be null");
        var runtime = find(callId);
        if (runtime == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "terminate ignored, call {0} not tracked", callId);
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "terminating call {0}, reason={1}", callId, reason);
        var creator = runtime.call().creator();
        engine().endCall(callId, reason);
        // Fan the local hangup out to the application's onCallEnded listeners, matching reject() and the
        // inbound terminate path; the engine teardown fires the registry/WAM/runtime teardown sink but not the
        // host end event, so the local end path surfaces it here for symmetry with every peer driven end.
        notifyEnded(callId, creator, reason.wireValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preaccept(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (!callExists(callId)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "preaccept ignored, call {0} not tracked", callId);
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending preaccept for call {0}", callId);
        engine().preaccept(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation routes the membership update through the engine's outbound group call
     * unit ({@link LifecycleController#sendGroupParticipants(String, Jid, Jid, List, boolean)}), which
     * builds the {@code <group_update>} with a {@code <group_info>} roster of the affected participants,
     * ships it fire and forget to the group target, and reconciles the call's {@code CallMembership} against
     * the add or remove so the unit's per peer offer sweep tracks a newly added participant. A call that is
     * not tracked, or one that is not a group call, does nothing in the unit.
     */
    @Override
    public void sendGroupParticipants(String callId, Jid target, Jid creator, List<Jid> participants,
                                      boolean added) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(creator, "creator cannot be null");
        Objects.requireNonNull(participants, "participants cannot be null");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants cannot be empty");
        }
        engine().sendGroupParticipants(callId, target, creator, participants, added);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation routes the camera turn on through
     * {@link LifecycleController#startLocalVideo(String)}, which announces video on to the peer through
     * the call's {@code VideoStateController} and starts the outbound camera capture and encode path on the
     * call's media plane. A session brought up audio only raises its local video track through the engine's
     * mid call audio to video upgrade seam, which builds the video pipeline and starts both loops, so the
     * camera track start is effected on such a call rather than being announce only.
     */
    @Override
    public void startLocalVideo(String callId) {
        var runtime = find(callId);
        if (runtime == null) {
            return;
        }
        engine().startLocalVideo(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation drives the engine's
     * {@link LifecycleController#startScreenShare(String)}, which sends the {@code <screen_share>}
     * action at the negotiated protocol version through the call's
     * {@link com.github.auties00.cobalt.calls.engine.control.ScreenShareController}. That announce is the
     * complete V2 single stream half: the screen content port swaps onto the existing camera video stream, so
     * no separate media track is created for it.
     */
    @Override
    public void startScreenShare(String callId) {
        var runtime = find(callId);
        if (runtime == null) {
            return;
        }
        // TODO: start the auxiliary screen media stream for the V3 dual stream path. The
        //  ScreenShareController.start() announce the engine drives below is the complete signaling half; the
        //  V2 single stream path needs only that announce, because the screen content port swaps onto the
        //  existing camera video stream and reuses it. The V3 dual stream path builds a second video stream for
        //  the screen and is blocked on four layers that are unwired across calls, not on this seam, so an aux
        //  stream cannot be started faithfully here yet:
        //   1. The per participant subscription publish is not wired into the production media plane. The V3
        //      path builds a stream descriptor and publishes it before allocating the aux stream, and a
        //      receiver only decodes a stream after it subscribes; without that publish the relay has no route
        //      for the new screenshare SSRCs and no peer receives the stream. The media plane builds the
        //      stream descriptors from the call's stream layout but does not yet route them through the relay
        //      transport's subscription envelope send seam, so the descriptor publish is unwired for every
        //      stream, not just this one (the camera mid call upgrade likewise sends no subscription update).
        //   2. The stream layout carries no screenshare SSRC field and the subscription publisher emits no
        //      screenshare stream descriptor, so the two screenshare stream layers cannot be published until
        //      the layout and publisher learn them.
        //   3. The SSRC generator derives no screenshare SSRC. WhatsApp derives the screenshare SSRC over the
        //      same transmit media type code the camera uses but over a screenshare distinguished identifier
        //      that must contain a screenshare marker substring. That identifier format is only partially
        //      recovered and is not verified against a live screenshare call, so deriving it from the partial
        //      shape would be an unverified guess.
        //   4. The media session exposes no aux stream seam (only local video start and key frame request), so
        //      there is nothing for the controller to drive even once the layers above land; a second video
        //      stream (a second video pipeline and RTP packetizer bound to the transport on the screenshare
        //      SSRC) would be built in the media plane mirroring the mid call video upgrade and reached the same
        //      way local video start reaches the media session.
        //  Building a second video stream on a stand in SSRC with no subscription would make every screenshare
        //  frame unroutable by real peers; the faithful prerequisite is the subscription/descriptor wiring of
        //  layers 1 and 2, then the screenshare SSRC derivation of layer 3 verified live, then the aux stream media
        //  seam of layer 4.
        engine().startScreenShare(callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopScreenShare(String callId) {
        var runtime = find(callId);
        if (runtime == null) {
            return;
        }
        engine().stopScreenShare(callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMute(Jid peer, Jid creator, String callId, boolean muted) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} audio muted={1}", callId, muted);
        var runtime = find(callId);
        if (runtime != null) {
            runtime.call().setAudioMuted(muted);
        }
        engine().setMuted(callId, muted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendVideoState(Jid peer, Jid creator, String callId, boolean enabled) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} video enabled={1}", callId, enabled);
        var runtime = find(callId);
        if (runtime != null) {
            runtime.call().setVideoMuted(!enabled);
        }
        engine().setVideoEnabled(callId, enabled);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation expresses the request as an enabling video state announce and delegates
     * to {@link #sendVideoState(Jid, Jid, String, boolean)} so the whole video state family funnels through
     * one method.
     */
    @Override
    public void sendVideoUpgradeRequest(Jid peer, Jid creator, String callId) {
        sendVideoState(peer, creator, callId, true);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation expresses the rejection as a disabling video state announce and
     * delegates to {@link #sendVideoState(Jid, Jid, String, boolean)}.
     */
    @Override
    public void sendVideoUpgradeReject(Jid peer, Jid creator, String callId) {
        sendVideoState(peer, creator, callId, false);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation routes the interaction onto its real engine control plane through
     * {@link LifecycleController#sendInteraction(String, CallInteraction)}, which dispatches a
     * raise hand, peer mute, or video upgrade onto the matching signaling plane control unit, routes a
     * reaction onto the call's {@code ReactionController} over the media plane's application data side channel,
     * and arms the outbound video encoder for a fresh key frame through the call's media session.
     */
    @Override
    public void sendInteraction(Jid peer, Jid creator, String callId, CallInteraction interaction) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(interaction, "interaction cannot be null");
        var runtime = find(callId);
        if (runtime == null) {
            return;
        }
        engine().sendInteraction(callId, interaction);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link LifecycleController#createCallLink(CallLinkMedia, boolean)}, which runs the blocking
     * {@code link_create} request reply against the {@code call} service and applies the call link feature
     * gate; the minted {@link CallLink} is returned directly rather than surfaced as a listener event.
     */
    @Override
    public CallLink createCallLink(CallLinkMedia media, boolean waitingRoomEnabled) {
        Objects.requireNonNull(media, "media cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating call link, media={0}, waiting_room={1}", media, waitingRoomEnabled);
        var link = engine().createCallLink(media, waitingRoomEnabled);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created call link {0}", new LogRedactable.Token(link.token()));
        return link;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link LifecycleController#setWaitingRoomEnabled(String, boolean)}, which drives the call's
     * waiting room controller when the call is a tracked call link call and does nothing otherwise.
     */
    @Override
    public void setWaitingRoomEnabled(String callId, boolean enabled) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} waiting room enabled={1}", callId, enabled);
        engine().setWaitingRoomEnabled(callId, enabled);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link LifecycleController#admitWaitingRoomParticipant(String, Jid)}, which drives the call's
     * waiting room controller when the call is a tracked call link call and does nothing otherwise.
     */
    @Override
    public void admitWaitingRoomParticipant(String callId, Jid userJid) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(userJid, "userJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} admitting waiting room participant {1}", callId, userJid);
        engine().admitWaitingRoomParticipant(callId, userJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link LifecycleController#admitAllWaitingRoomParticipants(String)}, which drives the call's
     * waiting room controller when the call is a tracked call link call and does nothing otherwise.
     */
    @Override
    public void admitAllWaitingRoomParticipants(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} admitting all waiting room participants", callId);
        engine().admitAllWaitingRoomParticipants(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to
     * {@link LifecycleController#denyWaitingRoomParticipant(String, Jid)}, which drives the call's
     * waiting room controller when the call is a tracked call link call and does nothing otherwise.
     */
    @Override
    public void denyWaitingRoomParticipant(String callId, Jid userJid) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(userJid, "userJid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} denying waiting room participant {1}", callId, userJid);
        engine().denyWaitingRoomParticipant(callId, userJid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallRuntime find(String callId) {
        return callId == null ? null : activeCalls.get(callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean callExists(String callId) {
        return callId != null && activeCalls.containsKey(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation routes an {@link OfferStanza} through
     * {@link LifecycleController#handleIncomingOffer(OfferStanza, Jid)} so the controller rings the
     * new call and the returned {@link IncomingCall} can be fanned out as the {@code onCall} listener event
     * through the {@link LiveCallEventBus}; every other decoded action goes through
     * {@link LifecycleController#handleIncomingMessage(CallMessage, Jid)} for its per type dispatch.
     * The offer path is split out here because the slim opaque engine event sink cannot carry the
     * {@link IncomingCall} the {@code onCall} callback needs, so the service surfaces it from the
     * controller's return value.
     */
    @Override
    public void handleInbound(CallMessage message, Jid senderJid) {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        if (message instanceof OfferStanza offer) {
            // Learn the caller's PN<->LID pairing from the server stamped caller_pn so onCallEnded and any
            // later signaling can present the caller's phone number rather than its LID.
            offer.callerPnValue().ifPresent(callerPn ->
                    whatsapp.store().contactStore().registerLidMapping(callerPn.toUserJid(),
                            offer.callCreator().orElseThrow().toUserJid()));
            engine().handleIncomingOffer(offer, senderJid)
                    .ifPresent(incoming -> {
                        if (Log.DEBUG) {
                            LOGGER.log(Level.DEBUG, "incoming call offer {0}, video={1}, group={2}",
                                    incoming.callId(), incoming.videoOffered(), incoming.group());
                        }
                        // Track the received offer in the call store so it can be joined from the ongoing call
                        // banner (joinGroupCall) after the ring is dismissed; it is dropped by removeCall when
                        // the call is accepted, joined, rejected, or ends.
                        whatsapp.store().chatStore().addCall(incoming);
                        eventBus.emit(new CallEvent.IncomingOffer(incoming));
                    });
            return;
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "handling inbound call message, type={0}", message.getClass().getSimpleName());
        engine().handleIncomingMessage(message, senderJid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation drops a notice whose original offer is older than
     * {@link #OFFER_NOTICE_STALENESS}. A fresh notice is recorded in the call history as an
     * {@link IncomingCall} flagged as an offline offer, its peer resolved from the {@code call-creator}
     * device JID and mapped back to its phone number when known, and is fanned out to the
     * {@code onCallOfferNotice} listeners; for a one to one notice the chat JID is the peer, and a group
     * notice carries no group JID because the wire form does not include one. The notice is purged from the
     * call history after {@link #OFFER_NOTICE_STALENESS} elapses.
     */
    @Override
    public void handleOfferNotice(OfferNoticeStanza notice) {
        Objects.requireNonNull(notice, "notice cannot be null");
        if (Duration.between(notice.offerTime(), Instant.now()).compareTo(OFFER_NOTICE_STALENESS) > 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dropping stale offer_notice for call {0}", notice.callId());
            return;
        }
        var peer = toListenerJid(notice.callCreator().toUserJid());
        var incoming = new IncomingCall(notice.callId(), peer, peer, notice.offerTime(),
                notice.video(), notice.group(), null, true);
        whatsapp.store().chatStore().addCall(incoming);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "recorded offline offer notice for call {0}, video={1}, group={2}", notice.callId(), notice.video(), notice.group());
        notifyOfferNotice(incoming);
        ScheduledTask.scheduleDelayed(OFFER_NOTICE_STALENESS,
                () -> whatsapp.store().chatStore().removeCall(notice.callId()));
    }

    /**
     * Fans an offline offer notice out to every registered {@link LinkedCallOfferNoticeListener}.
     *
     * <p>Each listener is invoked on its own virtual thread, and a listener exception is logged and
     * swallowed rather than propagated, so one faulty listener neither stalls the signaling reader nor
     * blocks the fan out to the other listeners.
     *
     * @implNote This implementation delivers the offer notice on a dedicated path rather than through
     * {@link LiveCallEventBus}. In WhatsApp Web the offer notice is not part of the voip call event
     * pipeline: {@code WAWebHandleVoipOfferNotice} fires
     * {@code WAWebBackendApi.frontendFireAndForget("handleIncomingOfferNotice")}, which reaches
     * {@code WAWebIncomingOfferNoticeVoipHandlerAction} and, past its own staleness and calling enabled
     * gates, surfaces the notice through {@code WAWebCallNotificationBus.trigger("alert_call")} only when
     * calling is disabled (when calling is enabled the notice is logged as an unexpected error and
     * dropped), a separate notification bus from the general call event dispatch. Routing this notice
     * through {@link LiveCallEventBus} and its host facing gate would therefore diverge from WA, so the
     * direct fan out is intentional, not a shortcut around the bus.
     *
     * @param call the offline offer descriptor to deliver
     */
    private void notifyOfferNotice(IncomingCall call) {
        for (var listener : whatsapp.store().listeners()) {
            if (listener instanceof LinkedCallOfferNoticeListener typed) {
                Thread.startVirtualThread(() -> {
                    try {
                        typed.onCallOfferNotice(whatsapp, call);
                    } catch (RuntimeException exception) {
                        if (Log.WARNING) {
                            LOGGER.log(Level.WARNING, "onCallOfferNotice listener threw for call " + call.callId(), exception);
                        }
                    }
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation forwards the terminate to the engine for its teardown and then, only when
     * the engine reports the terminate was effected, fans the {@code onCallEnded} listener event out through
     * {@link #notifyEnded(String, Jid, String)} with the terminate's call creator and wire reason, so a
     * peer driven or accepted elsewhere call end reaches the application even though the opaque engine event
     * sink carries no typed end payload. The engine's
     * {@link LifecycleController#handleIncomingMessage(CallMessage, Jid)} returns whether the inbound
     * terminate tore the call down rather than being suppressed by the companion device or
     * joinable on expired offer guard (or dropped by the router as a duplicate); a suppressed or dropped
     * terminate fires no host end, so the end of call host event fires only after the ignore guards pass.
     */
    @Override
    public void handleInboundTerminate(TerminateStanza terminate, Jid senderJid) {
        Objects.requireNonNull(terminate, "terminate cannot be null");
        Objects.requireNonNull(senderJid, "senderJid cannot be null");
        var torn = engine().handleIncomingMessage(terminate, senderJid);
        if (torn) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "inbound terminate tore down call {0}, wire_reason={1}",
                        terminate.callId().orElse(null), terminate.reasonWire());
            }
            notifyEnded(terminate.callId().orElseThrow(), terminate.callCreator().orElseThrow(), terminate.reasonWire());
        } else if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "inbound terminate for call {0} suppressed or dropped", terminate.callId().orElse(null));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation first stamps the accept ack NACK result onto the tracked call's
     * telemetry accumulator (so the WAM Call event reports the engine's own result code, which survives the
     * controller's release of the engine context at teardown), then forwards the outcome to the engine's
     * {@link LifecycleController#handleIncomingAck(CallAckOutcome)}, which keys it to the answered call
     * and, on an accept NACK, ends the call with the accept side setup failure; a success ack or an ack for
     * an untracked call does nothing.
     */
    @Override
    public void handleInboundAck(CallAckOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome cannot be null");
        if (outcome.isNack()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call {0} accept ack nack, error={1}", outcome.id(), outcome.error().getAsInt());
            var runtime = activeCalls.get(outcome.id());
            if (runtime != null) {
                CallResult.fromAcceptAckError(outcome.error().getAsInt())
                        .ifPresent(result -> runtime.stats().result(result));
            }
        }
        engine().handleIncomingAck(outcome);
    }

    /**
     * Records the engine resolved call result on a tracked call's telemetry accumulator.
     *
     * <p>The lifecycle controller reports a distinct engine result through its result sink for outcomes whose
     * terminal end reason is lossy (an offer NACK is {@link CallResult#SERVER_NACK}); this stamps it on
     * the call's {@link CallStats} so the WAM Call event reports the engine's own result code rather
     * than the end reason projection. A result for an untracked call is ignored.
     *
     * @param callId the call identifier
     * @param result the engine resolved call result
     */
    public void recordCallResult(String callId, CallResult result) {
        var runtime = activeCalls.get(callId);
        if (runtime != null) {
            runtime.stats().result(result);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation is driven once per call by the engine lifecycle controller's teardown
     * sink, which the service binds to this method through
     * {@link LifecycleController#bindTeardownSink(java.util.function.Consumer)} in its constructor. The
     * controller fires it from its single ENDED transition for every end path (local hangup, peer terminate,
     * reject, timeout, offer NACK, or setup failure), so the registry removal, the runtime stream and
     * media session shutdown through {@link CallRuntime#end(CallEndReason)}, and the end of call WAM Call
     * and JoinableCall events fire exactly once for every ended call regardless of who ended it. The runtime
     * teardown runs here as the sole owner of {@link CallRuntime#end(CallEndReason)}, using the terminal end
     * reason the controller stamped on the call view before firing this sink; an unknown {@code callId}
     * removes nothing and ends nothing, and a call with no telemetry accumulator (telemetry disabled) still
     * has its runtime shut down but skips the WAM commit.
     */
    @Override
    public void unregister(String callId) {
        var runtime = activeCalls.remove(callId);
        whatsapp.store().chatStore().removeCall(callId);
        if (runtime == null) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "unregister ignored, call {0} not tracked", callId);
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unregistered call {0}", callId);
        // Own the runtime teardown here: this sink is fired once at the ENDED transition for every end path
        // (local hangup, peer terminate, reject, timeout, offer NACK, setup failure), so shutting the four
        // streams and the media session from here unblocks the application's blocked reads and writes and
        // reaches CallState#ENDED regardless of who ended the call. The controller has already stamped the
        // terminal end reason on the shared call view before firing this sink.
        runtime.end(runtime.call().endReason().orElse(CallEndReason.UNKNOWN));
        if (wamService == null) {
            return;
        }
        emitFieldStatsEvent(runtime);
    }

    /**
     * Stamps a tracked call's telemetry accumulator with its connected instant when its media plane first
     * goes active.
     *
     * <p>Bound onto the engine lifecycle controller's connected sink through
     * {@link LifecycleController#bindConnectedSink(java.util.function.Consumer)} in this service's
     * constructor, this is driven once as a call first reaches {@link CallState#ACTIVE}, symmetric with the
     * ended instant the runtime stamps at teardown, so the end of call WAM Call event reports a nonzero
     * connected duration rather than zero. A signal for an untracked call stamps nothing.
     *
     * @param callId the identifier of the call whose media plane became active
     */
    private void markCallConnected(String callId) {
        var runtime = activeCalls.get(callId);
        if (runtime != null) {
            runtime.stats().markConnected();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} connected", callId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyEnded(String callId, Jid fromJid, String wireReason) {
        var parsed = CallEndReason.fromWireValue(wireReason);
        // Call signaling is LID addressed on the wire; applications expect the peer's phone number, so
        // map the LID back to its PN through the learned mapping before fanning the event out.
        var from = toListenerJid(fromJid);
        // Every end cause (local hangup, reject, placement NACK, inbound terminate) is a terminate, not a
        // local fatal engine failure, so this reports CALL_TERMINATE_RECEIVED, which LiveCallEventBus admits;
        // the gate can never suppress an Ended. WA likewise surfaces call ended for all causes with no
        // per-cause gate: every JS end path drives the native state machine to terminal, forwarded to the
        // host through one unconditional setCallState (WAWebVoipHandleNativeCallEvent).
        eventBus.emit(new CallEvent.Ended(callId, from, parsed, false));
    }

    /**
     * Surfaces a call the engine ended synchronously during placement to the application, reporting
     * whether it did.
     *
     * <p>An outbound offer the server rejects (an offer NACK, e.g. server {@code error="439"}) is torn
     * down inside the engine placement before it returns, leaving the {@link Call} in
     * {@link CallState#ENDED}. This fans the {@code onCallEnded} event out with the recorded end reason so
     * the application learns the call never connected, rather than the placement path registering a dead
     * runtime and returning a call indistinguishable from a ringing one.
     *
     * @param call the call returned by the engine placement
     * @param peer the peer the offer addressed, surfaced as the ended event JID
     * @return {@code true} when the call had already ended and the {@code onCallEnded} event was fanned out
     */
    private boolean notifyIfEndedDuringPlacement(Call call, Jid peer) {
        if (call.state() != CallState.ENDED) {
            return false;
        }
        var reason = call.endReason().orElse(CallEndReason.UNKNOWN);
        notifyEnded(call.callId(), peer, reason.wireValue());
        return true;
    }

    /**
     * Maps a possibly LID addressed call peer JID back to the phone number JID applications expect.
     *
     * <p>Call signaling is LID addressed on the wire, but listeners are handed the peer's phone number so
     * it matches the JID applications hold for contacts and chats. A LID server JID is resolved through
     * the learned PN to LID mapping; a JID that is already a phone number, has no known mapping, or is
     * {@code null} is returned unchanged.
     *
     * @param jid the JID surfaced by the call layer, or {@code null}
     * @return the phone number JID when one is known, otherwise the input unchanged
     */
    private Jid toListenerJid(Jid jid) {
        if (jid == null || !jid.hasLidServer()) {
            return jid;
        }
        return whatsapp.store().contactStore().findPhoneByLid(jid.toUserJid()).orElse(jid);
    }

    /**
     * Returns the event bus that fans host facing call events out to the registered listeners.
     *
     * @return the event bus
     */
    public LiveCallEventBus eventBus() {
        return eventBus;
    }

    /**
     * Builds a fresh {@link CallRuntime} for a call backed by the supplied media streams and registers it
     * in the active call registry.
     *
     * <p>The runtime builds and owns its own telemetry accumulator with the supplied side and video flag.
     * An audio only call carries idle buffered video streams the engine never drains or fills, so the
     * runtime always plumbs four streams.
     *
     * @param call     the public data view the runtime drives
     * @param side     which side initiated the call, for the telemetry accumulator
     * @param video    whether the call was placed or accepted with video enabled
     * @param audioOut the source the engine drains local audio from for transmission
     * @param audioIn  the sink the engine fills with received remote audio
     * @param videoOut the source the engine drains local video from for transmission, or {@code null} for
     *                 an audio only call
     * @param videoIn  the sink the engine fills with received remote video, or {@code null} for an
     *                 audio only call
     * @return the registered runtime
     */
    private CallRuntime registerRuntime(Call call, CallSide side, boolean video,
                                          AudioOutput audioOut, AudioInput audioIn,
                                          VideoOutput videoOut, VideoInput videoIn) {
        var runtimeVideoOut = videoOut != null ? videoOut : VideoOutput.fromBlank();
        var runtimeVideoIn = videoIn != null ? videoIn : VideoInput.discard();
        var stats = new CallStats(call.callId(), side, video, Instant.now());
        var runtime = new CallRuntime(call, stats, audioOut, audioIn, runtimeVideoOut, runtimeVideoIn);
        activeCalls.put(call.callId(), runtime);
        return runtime;
    }

    /**
     * Bundles the application capture sources and playback sinks for the engine media plane, mapped by media
     * flow rather than by public parameter name.
     *
     * <p>The public call API names its streams from the application's point of view: {@code audioOut} and
     * {@code videoOut} are the streams the application writes outbound media into, and {@code audioIn} and
     * {@code videoIn} are the streams it reads inbound media from. The media plane instead groups them by
     * the direction it drives them: the {@code out} streams are the capture sources it encodes and the
     * {@code in} streams are the playback sinks it delivers decoded media to. An audio only call passes a
     * {@code null} video source and sink, so the engine starts no video pipeline; the idle buffered video
     * streams {@link #registerRuntime} plumbs onto the runtime for teardown are deliberately not handed to
     * the engine, since the engine drives only the streams the call actually carries.
     *
     * @param audioOut the local audio capture source the engine encodes
     * @param audioIn  the remote audio playback sink the engine renders decoded audio to
     * @param videoOut the local video capture source the engine encodes, or {@code null} for an audio only
     *                 call
     * @param videoIn  the remote video playback sink the engine renders decoded video to, or {@code null}
     *                 for an audio only call
     * @return the engine media streams bundle
     */
    private static MediaStreams mediaStreams(AudioOutput audioOut, AudioInput audioIn,
                                                   VideoOutput videoOut, VideoInput videoIn) {
        return new MediaStreams(audioOut, audioIn, videoOut, videoIn);
    }

    /**
     * Returns the local self device JID in the LID addressing mode call signaling requires.
     *
     * <p>WhatsApp stamps {@code call-creator} with the local LID carrying the current device suffix, not
     * the phone number. This returns the account LID promoted to the phone number JID's device, falling
     * back to the phone number JID only for an account that has no LID assigned.
     *
     * @return the local self JID, LID addressed when a LID is available
     * @throws IllegalStateException if the client is not logged in
     */
    private Jid requireSelfJid() {
        var accountStore = whatsapp.store().accountStore();
        var selfPn = accountStore.jid()
                .orElseThrow(() -> new IllegalStateException("Not logged in"));
        return accountStore.lid()
                .map(Jid::toUserJid)
                .map(lid -> selfPn.hasDevice() ? lid.withDevice(selfPn.device()) : lid)
                .orElse(selfPn);
    }

    /**
     * Returns the engine lifecycle controller, requiring it to have been injected.
     *
     * <p>The production client builds this service through the
     * {@link #LiveCallsService(LinkedWhatsAppClient, WamService, MessageService, MessageEncryption, DeviceService, LinkedWhatsAppStore, ABPropsService, LiveCallEventBus, CallLogSync)
     * full dependency constructor}, which assembles the controller through {@link LifecycleController#create},
     * so this method returns it directly. A service built through the
     * {@link #LiveCallsService(LinkedWhatsAppClient, WamService, MessageService) constructor without a
     * controller} has no engine, and any call control method that reaches the engine through this accessor
     * fails fast rather than silently dropping the action.
     *
     * @return the engine lifecycle controller
     * @throws IllegalStateException if this service was built without an engine controller
     */
    private LifecycleController engine() {
        if (lifecycleController == null) {
            throw new IllegalStateException("calls engine lifecycle controller is not wired");
        }
        return lifecycleController;
    }

    /**
     * Builds and commits the WAM Call event for one ended call.
     *
     * <p>The event combines the call's start time telemetry dimensions, drawn from its
     * {@link CallRuntime#stats()} accumulator, with the call's terminal {@link CallEndReason} mapped to a
     * {@link CallResultType}, and is committed through {@link WamService}. The completed call is also reported
     * to the ctlv2 thread logging aggregator through
     * {@link LinkedWhatsAppClient#recordThreadActivity(com.github.auties00.cobalt.wire.core.jid.JidProvider, ThreadLoggingActivity)}
     * as a {@link ThreadLoggingActivity.Call} keyed by the call's {@link com.github.auties00.cobalt.wire.linked.call.Call#chatJid() chat JID},
     * carrying the direction and connected duration. Any {@link RuntimeException} raised while building,
     * committing, or recording is swallowed so that telemetry never propagates a failure into the call path.
     *
     * @param runtime the runtime of the call that just ended
     */
    private void emitFieldStatsEvent(CallRuntime runtime) {
        try {
            var stats = runtime.stats();
            var endReason = runtime.call().endReason().orElse(CallEndReason.UNKNOWN);
            var resultType = stats.result()
                    .map(LiveCallsService::toResultType)
                    .orElseGet(() -> mapToResultType(endReason));
            var connectedDuration = stats.connectedDurationSeconds();
            var group = runtime.call().isGroup();
            var builder = new CallEventBuilder()
                    .callRandomId(stats.callId())
                    .callSide(stats.side())
                    .callResult(resultType)
                    .videoEnabled(stats.videoEnabled())
                    .videoEnabledAtCallStart(stats.videoEnabled())
                    .callOfferElapsedT(stats.startedAt())
                    .isLidCall(true)
                    .callTransport(CallTransportType.UDP_RELAY)
                    .callNetwork(CallNetworkMedium.WIFI)
                    .isRejoin(false)
                    .isCallFull(false);
            if (connectedDuration > 0) {
                builder.durationTSs(Instant.ofEpochSecond(connectedDuration));
                if (!group) {
                    builder.numConnectedPeers(1);
                }
            }
            if (stats.side() == CallSide.CALLER) {
                builder.callFromUi(group ? CallFromUi.GROUP_CALL_INFO : CallFromUi.CONVERSATION);
            }
            wamService.commit(builder.build());
            whatsapp.recordThreadActivity(runtime.call().chatJid(), new ThreadLoggingActivity.Call(
                    stats.side() == CallSide.CALLER, connectedDuration));
            if (group) {
                emitJoinableCallEvent(runtime, resultType);
            }
        } catch (RuntimeException exception) {
            // Telemetry is best effort and must never break the call path; a build or commit failure is
            // swallowed.
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to emit end of call telemetry for call " + runtime.callId(), exception);
        }
    }

    /**
     * Builds and commits the WAM Joinable Call event for one ended joinable (group or call link) call.
     *
     * <p>WhatsApp emits a JoinableCall event in addition to the Call event over a joinable call's lifetime;
     * this is its end of call counterpart, committed alongside the Call event for a group or call link call.
     * It carries the call identity, side, result, and video dimensions the service has and marks the call as
     * not one to one; the lobby latency timers, the connected and invited peer counts, and the call link
     * dimensions stay unset until the group call manager feeds them in.
     *
     * @param runtime    the runtime of the joinable call that just ended
     * @param resultType the call result already resolved for the Call event
     */
    private void emitJoinableCallEvent(CallRuntime runtime, CallResultType resultType) {
        var stats = runtime.stats();
        var event = new JoinableCallEventBuilder()
                .callRandomId(stats.callId())
                .callSide(stats.side())
                .legacyCallResult(resultType)
                .videoEnabled(stats.videoEnabled())
                .isOneOnOneCall(false)
                .build();
        wamService.commit(event);
    }

    /**
     * Builds and commits the WAM pre call user journey event for a call this client is placing.
     *
     * <p>WhatsApp records a pre call funnel beacon at the moment the user initiates a call from a chat
     * thread (tapping the audio or video call control, starting a group call, or joining a call link), well
     * before the call connects, so the pre call journey can be reconciled against the call that follows. This
     * emits that beacon the instant the engine has minted the call identifier, keyed to it through
     * {@link PreCallUserJourneyChatThreadEventBuilder#callRandomId(String)}, and stamps the initiating
     * action, the call size class, and the video flag drawn from the placement call, together with the
     * service's {@link #appSessionId} and freshly minted per call surface and funnel identifiers. The
     * event fires only when {@link WamService telemetry} is enabled; a build or commit failure is swallowed
     * so telemetry never breaks the call placement path. The optional dimensions are set only when the
     * caller supplies them, so an audio only one to one placement carries no group size and a call link join
     * carries no sub surface.
     *
     * @param callId           the identifier of the call being placed, correlating the funnel to the call
     * @param action           the pre call action that initiated the placement
     * @param sizeType         the call size class of the placement
     * @param video            whether the call is being placed with video enabled
     * @param subSurface       the UI sub surface the placement originated from, or {@code null} when none
     *                         applies
     * @param callSize         the number of participants the call is placed to, or {@code null} when it is
     *                         not known at placement
     * @param groupSize        the size of the group the call is placed within, or {@code null} for a
     *                         nongroup placement
     * @param isCommunityGroup whether the group is a community group, or {@code null} for a nongroup
     *                         placement
     */
    private void emitPreCallJourney(String callId, PreCallActionType action, CallSizeType sizeType,
                                    boolean video, SubSurface subSurface, Long callSize, Long groupSize,
                                    Boolean isCommunityGroup) {
        if (wamService == null) {
            return;
        }
        try {
            var builder = new PreCallUserJourneyChatThreadEventBuilder()
                    .appSessionId(appSessionId)
                    .callRandomId(callId)
                    .callSizeType(sizeType)
                    .isVideoCall(video)
                    .preCallActionType(action)
                    .surfaceSessionId(randomSessionId())
                    .userJourneyFunnelId(randomSessionId())
                    .userJourneyEventMs(System.currentTimeMillis());
            if (subSurface != null) {
                builder.subSurface(subSurface);
            }
            if (callSize != null) {
                builder.callSize(callSize);
            }
            if (groupSize != null) {
                builder.groupSize(groupSize);
            }
            if (isCommunityGroup != null) {
                builder.isCommunityGroup(isCommunityGroup);
            }
            wamService.commit(builder.build());
        } catch (RuntimeException exception) {
            // Telemetry is best effort and must never break the call placement path; a build or commit
            // failure is swallowed.
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to emit pre call journey event for call " + callId, exception);
        }
    }

    /**
     * Mints a fresh random 16 hex character identifier for a WAM session or funnel field.
     *
     * <p>WhatsApp identifies an app session and a pre call funnel with opaque random strings the receiver
     * only groups by, never parses; this produces one such identifier from the low 64 bits of a
     * {@link ThreadLocalRandom} draw, zero padded to 16 hexadecimal characters.
     *
     * @return a random 16 hex character identifier
     */
    private static String randomSessionId() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }

    /**
     * Maps a {@link CallEndReason} to the {@link CallResultType} reported in the WAM Call event.
     *
     * @implNote This implementation reports {@link CallEndReason#ACCEPTED_ELSEWHERE} as
     * {@link CallResultType#CONNECTED}: it arrives as a peer side terminate, but from the local user's
     * perspective the call did connect, just not on this device.
     *
     * @param reason the canonical end reason
     * @return the matching WAM result type
     */
    private static CallResultType mapToResultType(CallEndReason reason) {
        return switch (reason) {
            case HANGUP, MEDIA_TX_TIMEOUT, MEDIA_RX_TIMEOUT, AV_UPGRADABLE, AV_UPGRADE -> CallResultType.CONNECTED;
            case TIMEOUT -> CallResultType.MISSED;
            case REJECT_DO_NOT_DISTURB, REJECT_BLOCKED, REJECTED -> CallResultType.REJECTED_BY_USER;
            case REJECTED_ELSEWHERE -> CallResultType.REJECTED_ELSEWHERE;
            case MIC_PERMISSION_DENIED, CAMERA_PERMISSION_DENIED, SETUP_FAILED, RELAY_BIND_FAILED ->
                    CallResultType.SETUP_ERROR;
            case ACCEPTED_ELSEWHERE -> CallResultType.CONNECTED;
            case DEVICE_SWITCH -> CallResultType.ACTIVE_ELSEWHERE;
            case UNKNOWN -> CallResultType.INVALID;
        };
    }

    /**
     * Maps an engine {@link CallResult} the service recorded onto the {@link CallResultType} the WAM
     * Call event reports.
     *
     * <p>WhatsApp reports the engine's own numeric call result code on the WAM event, not the terminate
     * reason. The service records that result for the outcomes whose terminal end reason is lossy, in
     * particular the accept ack NACK; this maps those engine results onto the matching WAM result type:
     * {@link CallResult#CALL_DOES_NOT_EXIST_FOR_REJOIN}, {@link CallResult#CALL_IS_FULL}, and
     * {@link CallResult#CALL_OFFER_ACK_NOT_RECEIVED} carry their own {@link CallResultType} of the same
     * meaning, while any other recorded result falls back to its end reason projection through
     * {@link #mapToResultType(CallEndReason)} (so an engine result on the terminate axis still reports the
     * same type the reason projection would).
     *
     * @param result the engine call result the service recorded
     * @return the matching WAM result type
     */
    private static CallResultType toResultType(CallResult result) {
        return switch (result) {
            case CALL_DOES_NOT_EXIST_FOR_REJOIN -> CallResultType.CALL_DOES_NOT_EXIST_FOR_REJOIN;
            case CALL_IS_FULL -> CallResultType.CALL_IS_FULL;
            case CALL_OFFER_ACK_NOT_RECEIVED -> CallResultType.CALL_OFFER_ACK_NOT_RECEIVED;
            case SERVER_NACK -> CallResultType.SERVER_NACK;
            default -> mapToResultType(result.toEndReason());
        };
    }
}
