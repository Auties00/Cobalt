package com.github.auties00.cobalt.calls.engine;

import com.github.auties00.cobalt.calls.crypto.CallKeyExchange;
import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.platform.VoipHostApi;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.info.CallInfoManager;
import com.github.auties00.cobalt.calls.engine.info.CallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerKind;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerScheduler;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaPlane;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.state.CallStateMachine;
import com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallContextRegistry;
import com.github.auties00.cobalt.calls.engine.context.CallManager;
import com.github.auties00.cobalt.calls.engine.context.LiveCallContextRegistry;
import com.github.auties00.cobalt.calls.engine.info.LiveCallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.engine.timer.CallTimers;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.telemetry.CallResult;

/**
 * Shared test harness wiring a real {@link LifecycleController} over recording fakes, so the P9
 * group-call placement, the per-participant {@code <enc_rekey>} fanout, and the timer teardown can be
 * driven end to end with no live client, no native code, and no network.
 *
 * <p>The controller's nine seams are all non-sealed interfaces (or records), which lets this harness
 * substitute every collaborator below signaling: the state guard is the production
 * {@link CallStateMachine} over a real {@link CallManager} (so transitions are genuinely
 * guarded), the context registry allocates real {@link CallContext} objects into that manager exactly
 * as the production {@code LiveCallContextRegistry} does, and the call key crypto, media plane, signaling
 * host, timer scheduler, info updater, and event sink are deterministic recorders that capture what the
 * controller drives. The crypto fake records each offer and rekey fanout and returns deterministic
 * envelopes, the media plane records each bring-up and hands back a closeable session, the host records
 * every {@code sendSignaling} stanza, and the timer scheduler records every arm and cancel and exposes the
 * armed callbacks so a test can fire a timeout. None of this touches libopus, libsrtp, usrsctp, or a
 * socket.
 */
final class ControllerHarness {
    /**
     * The deterministic call key the {@link RecordingCrypto} mints, so a test can assert the exact bytes
     * fanned out.
     */
    static final byte[] CALL_KEY = deterministicKey();

    private final CallManager manager = new CallManager();
    private final RecordingHost host = new RecordingHost();
    private final RecordingCrypto crypto = new RecordingCrypto();
    private final RecordingMediaPlane mediaPlane = new RecordingMediaPlane();
    private final RecordingTimers timers = new RecordingTimers();
    private final RecordingEvents events = new RecordingEvents();
    private final java.util.List<Stanza> sentOfferEnvelopes = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final LifecycleController controller;

    /**
     * Builds a harness whose offer-ack carries the default minimal relay block (enough for the media plane
     * to come up but with no group roster to reconcile).
     */
    ControllerHarness() {
        this(ackNode());
    }

    /**
     * Builds a harness whose offer-ack is the supplied stanza, used by a group test that needs the ack to
     * echo a connected {@code <group_info>} roster so {@code reconcileFromAck} populates the membership.
     *
     * @param ackStanza the stanza the fake offer-ack sender returns for every offer
     */
    ControllerHarness(Stanza ackStanza) {
        var stateMachine = new CallStateMachine(manager);
        var infoManager = new CallInfoManager();
        this.controller = new LifecycleController(
                offerEnvelope -> {
                    sentOfferEnvelopes.add(offerEnvelope);
                    return ackStanza;
                },
                crypto,
                host,
                new ManagerRegistry(manager),
                stateMachine,
                timers,
                new InfoUpdater(manager, infoManager),
                events,
                mediaPlane);
    }

    /**
     * Builds an offer-ack stanza carrying a minimal relay block plus a {@code <group_info>} roster, so the
     * controller records a relay (bringing up the recording media plane) and reconciles the call's
     * membership against the connected roster the ack echoes.
     *
     * @param roster the connected roster the ack echoes
     * @return the roster-bearing offer-ack stanza
     */
    static Stanza ackNodeWithRoster(com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza roster) {
        return new StanzaBuilder()
                .description("ack")
                .attribute("class", "call")
                .content(List.of(relayNode(), roster.toStanza()))
                .build();
    }

    LifecycleController controller() {
        return controller;
    }

    CallManager manager() {
        return manager;
    }

    RecordingHost host() {
        return host;
    }

    RecordingCrypto crypto() {
        return crypto;
    }

    RecordingMediaPlane mediaPlane() {
        return mediaPlane;
    }

    RecordingTimers timers() {
        return timers;
    }

    RecordingEvents events() {
        return events;
    }

    /**
     * Returns the {@code <offer>} action nodes the controller shipped through the offer-ack sender,
     * unwrapped from their {@code <call>} envelopes. The offer rides the synchronous offer-ack seam rather
     * than the fire-and-forget host, so it is recorded here and not in {@link RecordingHost#signaling()}.
     *
     * @return the sent offer action nodes
     */
    List<Stanza> sentOffers() {
        var offers = new java.util.ArrayList<Stanza>();
        for (var envelope : sentOfferEnvelopes) {
            envelope.getChild("offer").ifPresent(offers::add);
        }
        return offers;
    }

    /**
     * The positive offer ack the fake offer-ack sender returns by default: an {@code <ack class="call">}
     * with no {@code error} (so the controller treats it as a successful ack) carrying a minimal
     * {@code <relay>} block with an {@code <hbh_key>}, so the controller records a relay and brings up the
     * (recording) media plane on the group placement path.
     *
     * @return the default offer-ack stanza
     */
    static Stanza ackNode() {
        return new StanzaBuilder()
                .description("ack")
                .attribute("class", "call")
                .content(relayNode())
                .build();
    }

    /**
     * Builds the minimal {@code <relay>} block (a {@code uuid} and a thirty-byte {@code <hbh_key>}) the
     * controller needs to record a relay and bring up the recording media plane.
     *
     * @return the minimal relay block stanza
     */
    private static Stanza relayNode() {
        return new StanzaBuilder()
                .description("relay")
                .attribute("uuid", "0123456789ABCDEF")
                .content(new StanzaBuilder()
                        .description("hbh_key")
                        .content("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                        .build())
                .build();
    }

    private static byte[] deterministicKey() {
        var key = new byte[com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange.CALL_KEY_LENGTH];
        for (var i = 0; i < key.length; i++) {
            key[i] = (byte) (0xA0 + i);
        }
        return key;
    }

    // ---- recording seams --------------------------------------------------------------------------------

    /**
     * Records every signaling stanza the controller ships and supplies deterministic call-id randomness.
     */
    static final class RecordingHost implements VoipHostApi {
        private final List<Stanza> signaling = new CopyOnWriteArrayList<>();

        List<Stanza> signaling() {
            return List.copyOf(signaling);
        }

        /**
         * Returns every signaling stanza whose nested action element has the given tag, unwrapping the
         * {@code <call>} envelope.
         */
        List<Stanza> actionsTagged(String tag) {
            var matches = new ArrayList<Stanza>();
            for (var envelope : signaling) {
                envelope.getChild(tag).ifPresent(matches::add);
            }
            return matches;
        }

        @Override
        public void sendSignaling(Stanza stanza) {
            signaling.add(stanza);
        }

        @Override
        public int sendDatagram(byte[] payload, SocketAddress destination) {
            return payload.length;
        }

        @Override
        public java.util.List<java.net.InetAddress> resolveHost(String hostName) {
            return java.util.List.of();
        }

        @Override
        public Optional<java.nio.file.Path> bweMlModelPath(int modelType) {
            return Optional.empty();
        }

        @Override
        public byte[] randomBytes(int length) {
            // Deterministic ascending bytes so the generated call id is stable across a test run.
            var bytes = new byte[length];
            for (var i = 0; i < length; i++) {
                bytes[i] = (byte) (i + 1);
            }
            return bytes;
        }

        @Override
        public boolean isKnownContact(Jid participant) {
            return true;
        }

        @Override
        public void renderVideoFrame(RenderedVideoFrame frame) {
        }

        @Override
        public int browserAudioProcessingStatus() {
            return 0;
        }

        @Override
        public void log(System.Logger.Level level, String message) {
        }

        @Override
        public void onCallEvent(CallEventType eventType, byte[] payload) {
        }
    }

    /**
     * A deterministic {@link CallKeyExchange} that records each offer and rekey fanout and returns an
     * encrypted slot or envelope per recipient device.
     */
    static final class RecordingCrypto implements CallKeyExchange {
        private final List<List<Jid>> offerFanouts = new CopyOnWriteArrayList<>();
        private final List<List<Jid>> rekeyFanouts = new CopyOnWriteArrayList<>();
        private final List<byte[]> mintedKeys = new CopyOnWriteArrayList<>();
        private volatile byte[] lastWrappedPlaintext;

        List<List<Jid>> offerFanouts() {
            return List.copyOf(offerFanouts);
        }

        List<List<Jid>> rekeyFanouts() {
            return List.copyOf(rekeyFanouts);
        }

        List<byte[]> mintedKeys() {
            return List.copyOf(mintedKeys);
        }

        byte[] lastWrappedPlaintext() {
            return lastWrappedPlaintext == null ? null : lastWrappedPlaintext.clone();
        }

        @Override
        public byte[] mintCallKey() {
            mintedKeys.add(CALL_KEY.clone());
            return CALL_KEY.clone();
        }

        @Override
        public byte[] wrapCallKey(byte[] callKey) {
            // The wrapped plaintext deterministically tags the raw key so the rekey fanout can be asserted
            // to carry exactly the minted key's wrapping.
            var wrapped = new byte[callKey.length + 1];
            wrapped[0] = (byte) 0x0A;
            System.arraycopy(callKey, 0, wrapped, 1, callKey.length);
            lastWrappedPlaintext = wrapped.clone();
            return wrapped;
        }

        @Override
        public List<CallKeyDistribution> encryptOfferFanout(Collection<Jid> deviceJids, byte[] plaintext) {
            offerFanouts.add(List.copyOf(deviceJids));
            var slots = new ArrayList<CallKeyDistribution>();
            for (var device : deviceJids) {
                slots.add(CallKeyDistribution.encrypted(device, 2, "pkmsg", 0, plaintext.clone()));
            }
            return slots;
        }

        @Override
        public List<CallRekeyEnvelope> encryptRekeyFanout(Collection<Jid> recipientDevices, byte[] plaintext) {
            rekeyFanouts.add(List.copyOf(recipientDevices));
            var envelopes = new ArrayList<CallRekeyEnvelope>();
            for (var device : recipientDevices) {
                // One <enc> per recipient (the single-32B unicast shape), with the wrapped key as the
                // ciphertext stand-in so a test can verify the per-recipient body length is constant.
                envelopes.add(new CallRekeyEnvelope(device, MessageEncryptionType.PKMSG, plaintext.clone(),
                        new byte[]{1, 2, 3}));
            }
            return envelopes;
        }

        @Override
        public Optional<byte[]> decryptCallKey(CallKeyDistribution slot, Jid senderJid) {
            // A one-to-one offer fixture supplies a slot whose ciphertext is the wrapped key; recover the
            // raw 32 bytes by stripping the one-byte wrapping tag.
            if (!slot.isEncrypted()) {
                return Optional.empty();
            }
            var ct = slot.ciphertext();
            if (ct.length == CALL_KEY.length + 1 && ct[0] == 0x0A) {
                return Optional.of(java.util.Arrays.copyOfRange(ct, 1, ct.length));
            }
            return Optional.empty();
        }

        @Override
        public byte[] signedDeviceIdentity() {
            return new byte[]{9, 9, 9};
        }
    }

    /**
     * Records each media-plane bring-up and hands back a closeable session that records its own close.
     */
    static final class RecordingMediaPlane implements MediaPlane {
        private final List<BringUp> bringUps = new CopyOnWriteArrayList<>();

        List<BringUp> bringUps() {
            return List.copyOf(bringUps);
        }

        @Override
        public Session bringUp(String callId, Stanza relay, List<Stanza> voipSettings, byte[] callKey, boolean isCaller,
                               boolean video, int participantCount,
                               com.github.auties00.cobalt.calls.engine.participant.CallMembership membership,
                               MediaStreams streams,
                               com.github.auties00.cobalt.wire.core.jid.Jid peerDeviceJid,
                               Optional<String> electedRelayName,
                               com.github.auties00.cobalt.calls.engine.mediaplane.MediaSessionListener listener) {
            var session = new RecordingSession(callId);
            bringUps.add(new BringUp(callId, isCaller, video, callKey.clone(), List.copyOf(voipSettings),
                    participantCount, streams, session));
            return session;
        }

        /**
         * One recorded bring-up call, its media streams, and the session it produced.
         */
        record BringUp(String callId, boolean isCaller, boolean video, byte[] callKey, List<Stanza> voipSettings,
                       int participantCount, MediaStreams streams, RecordingSession session) {
        }

        /**
         * A media-plane session that records whether it was closed, so a teardown can be asserted.
         */
        static final class RecordingSession implements Session {
            private final String callId;
            private volatile boolean closed;

            private RecordingSession(String callId) {
                this.callId = callId;
            }

            String callId() {
                return callId;
            }

            boolean closed() {
                return closed;
            }

            @Override
            public void close() {
                closed = true;
            }
        }
    }

    /**
     * Records every timer arm and cancel so a test can assert which timers the controller armed and
     * cancelled, standing in for the real virtual-thread {@link CallTimers} driver. The controller
     * supplied fire action is not run: these tests assert the arm and cancel choreography, not the fired
     * teardown, which the real driver covers.
     */
    static final class RecordingTimers implements CallTimerScheduler {
        private final List<Armed> armed = new CopyOnWriteArrayList<>();
        private final List<String> cancelledAll = new CopyOnWriteArrayList<>();

        List<CallTimerKind> armedKinds(String callId) {
            var kinds = new ArrayList<CallTimerKind>();
            for (var entry : armed) {
                if (entry.callId.equals(callId) && entry.live) {
                    kinds.add(entry.kind);
                }
            }
            return kinds;
        }

        List<String> cancelledAll() {
            return List.copyOf(cancelledAll);
        }

        boolean isArmed(String callId, CallTimerKind kind) {
            return armed.stream().anyMatch(a -> a.live && a.callId.equals(callId) && a.kind == kind);
        }

        @Override
        public void arm(String callId, CallTimerKind kind, Runnable action) {
            armed.add(new Armed(callId, kind));
        }

        @Override
        public void cancel(String callId, CallTimerKind kind) {
            armed.stream().filter(a -> a.callId.equals(callId) && a.kind == kind).forEach(a -> a.live = false);
        }

        @Override
        public void cancelAll(String callId) {
            cancelledAll.add(callId);
            armed.stream().filter(a -> a.callId.equals(callId)).forEach(a -> a.live = false);
        }

        private static final class Armed {
            private final String callId;
            private final CallTimerKind kind;
            private volatile boolean live = true;

            private Armed(String callId, CallTimerKind kind) {
                this.callId = callId;
                this.kind = kind;
            }
        }
    }

    /**
     * Records the ordered sequence of lifecycle events the controller emits onto the sink, so the
     * onCall*-facing sequence (and the per-change state-changed pulses) can be asserted.
     */
    static final class RecordingEvents implements CallLifecycleEventSink {
        private final List<CallEventType> events = Collections.synchronizedList(new ArrayList<>());

        List<CallEventType> events() {
            synchronized (events) {
                return List.copyOf(events);
            }
        }

        long count(CallEventType type) {
            synchronized (events) {
                return events.stream().filter(type::equals).count();
            }
        }

        @Override
        public void emit(CallEventType eventType, byte[] payload) {
            events.add(eventType);
        }
    }

    /**
     * Allocates real call contexts into the manager, mirroring the production {@code LiveCallContextRegistry}
     * (minus the connected-lonely timer-seam wiring, which the recording timer scheduler does not need).
     */
    private record ManagerRegistry(CallManager manager) implements CallContextRegistry {
        @Override
        public CallContext allocate(Call call, CallLifecycleState initialState) {
            var direction = call.isOutgoing()
                    ? CallContext.CallDirection.OUTGOING
                    : CallContext.CallDirection.INCOMING;
            var role = manager.hasPrimary()
                    ? CallContext.CallRole.SECONDARY
                    : CallContext.CallRole.PRIMARY;
            var context = new CallContext(call.callId(), role, direction, call.peer(), call.creator(),
                    call.creator(), call.chatJid(), call.isGroup(), call.isVideo());
            context.lock().lock();
            try {
                context.state(initialState);
            } finally {
                context.lock().unlock();
            }
            if (role == CallContext.CallRole.SECONDARY) {
                manager.startDualCall(context);
            } else {
                manager.startCall(context);
            }
            return context;
        }

        @Override
        public void release(String callId) {
            manager.endCall(callId);
        }
    }

    /**
     * Refreshes the info-manager snapshot from the resolved context, mirroring the production
     * {@code LiveCallInfoUpdater} so the controller's info-update calls are exercised against real state.
     */
    private record InfoUpdater(CallManager manager, CallInfoManager infoManager)
            implements CallInfoUpdater {
        @Override
        public void updateForEvent(String callId, CallEventType eventType) {
            var context = manager.getByCallId(callId).orElse(null);
            if (context == null) {
                return;
            }
            context.lock().lock();
            try {
                var active = java.time.Duration.ofMillis(context.activeDurationMillis());
                var lonely = java.time.Duration.ofMillis(context.lonelyDurationMillis());
                var result = context.result().orElse(CallResult.CALL_OFFER_ACK_NOT_RECEIVED);
                infoManager.updateForEvent(eventType, context.state(), result, active, lonely,
                        java.time.Duration.ZERO, null);
            } finally {
                context.lock().unlock();
            }
        }
    }
}
