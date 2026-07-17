package com.github.auties00.cobalt.calls;

import com.github.auties00.cobalt.ack.AckSender;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.context.CallContextRegistry;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.info.CallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateTransition;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerKind;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerScheduler;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaPlane;
import com.github.auties00.cobalt.calls.crypto.CallKeyExchange;
import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.platform.VoipHostApi;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.receive.CallMessageBuffer;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter;
import com.github.auties00.cobalt.calls.signaling.receive.CallReceiver;
import com.github.auties00.cobalt.calls.signaling.receive.TerminateReceiver;
import com.github.auties00.cobalt.calls.signaling.session.OfferNoticeStanza;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;
import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientListener;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.wire.linked.call.Call;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;
import com.github.auties00.cobalt.calls.signaling.CallStanza;

/**
 * Adversarial P8 wiring oracle for the inbound {@code <call>}/{@code <terminate>} routing seam.
 *
 * <p>This suite builds the two calls receivers exactly as {@code LiveNodeStreamService} wires them (an
 * {@link CallReceiver} under the {@code "call"} tag and a {@link TerminateReceiver} under the
 * {@code "terminate"} tag), feeds representative inbound stanzas, and asserts the inbound action reaches
 * the calls sink, the {@link LiveCallsService}, and (when an engine is wired) the
 * {@link LifecycleController}. The point is that the inbound routing reaches the calls engine
 * end to end through the calls receivers alone.
 *
 * <p>The harness uses a {@link TestWhatsAppClient} over a real temporary store (so receipts emitted via
 * {@code sendNodeWithNoResponse} surface through {@code onNodeSent}) and a real {@link LifecycleController}
 * assembled from minimal interface fakes, since the production controller's nine collaborators are all
 * interfaces and a real one would otherwise drag in transport and native units. The lifecycle reach is
 * observed through a recording {@link CallLifecycleEventSink}; the state-transition fake returns the prior
 * state so the controller's guarded transition actually fires its event.
 */
@DisplayName("calls P8 inbound routing wiring")
class InboundRoutingWiringTest {
    // The PN+LID pair the call corpus pairs with the primary session; reused so a parsed call-creator
    // LID is genuinely LID-addressed and clears the router's LID-only gate.
    private static final Jid SELF_PN = Jid.of("19153544650@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("39110693621863@lid");
    private static final Jid PEER_DEVICE_LID = Jid.of("55555555", JidServer.lid(), 7, 0);
    private static final String CALL_ID = "CAFEBABECAFEBABECAFEBABECAFEBABE";

    private static Stanza inboundOffer(String callId, Jid from, Jid callCreatorDevice, boolean withMedia) {
        var offer = new StanzaBuilder()
                .description("offer")
                .attribute("call-id", callId)
                .attribute("call-creator", callCreatorDevice);
        if (withMedia) {
            offer.content(new StanzaBuilder().description("media").attribute("type", "audio").build());
        }
        return new StanzaBuilder()
                .description("call")
                .attribute("from", from)
                .attribute("id", "stanza-" + callId)
                .attribute("sender_lid", callCreatorDevice)
                .content(offer.build())
                .build();
    }

    private static Stanza inboundEnvelope(Stanza payload, Jid from, Jid senderLid) {
        return new StanzaBuilder()
                .description("call")
                .attribute("from", from)
                .attribute("id", "stanza-x")
                .attribute("sender_lid", senderLid)
                .content(payload)
                .build();
    }

    @Nested
    @DisplayName("CallReceiver (the \"call\" tag handler)")
    class CallReceiverRouting {
        @Test
        @DisplayName("a fresh inbound offer is acked and replayed to the calls sink, never handed to a legacy receiver")
        void freshOfferRoutesToCalls2() throws IOException {
            var sentNodes = new ConcurrentLinkedQueue<Stanza>();
            var client = clientRecording(sentNodes);
            var forwarded = new ConcurrentLinkedQueue<CallMessage>();
            var receiver = new CallReceiver(client, new AckSender(client),
                    new CallSignalingRouter(), new CallMessageBuffer(),
                    callId -> false, (message, from) -> forwarded.add(message), notice -> {});

            receiver.handle(inboundOffer(CALL_ID, PEER_DEVICE_LID, PEER_DEVICE_LID, true));

            // callExists==false: the offer BUFFERs (calls disposition), but the offer is the call-creating
            // signal, so the receiver drains the call's buffer at once and replays the decoded offer to the
            // calls sink (the lifecycle controller forwarder), which rings the call. The legacy
            // CallService/CallReceiver is not on this path at all.
            assertEquals(1, forwarded.size(),
                    "a fresh offer must be replayed to the calls sink so the call rings; got " + forwarded);
            assertTrue(forwarded.peek() instanceof OfferStanza, "the replayed message is the decoded offer");
            var receipt = outgoing(sentNodes, "receipt");
            assertTrue(receipt.isPresent(), "offer must be acked with a <receipt>; got " + sentNodes);
            assertTrue(receipt.get().getChild("offer").isPresent(), "receipt child mirrors the offer tag");
        }

        @Test
        @DisplayName("a routable signal for an existing call is decoded and forwarded to the calls sink")
        void processForwardsToCalls2Sink() throws IOException {
            var sentNodes = new ConcurrentLinkedQueue<Stanza>();
            var client = clientRecording(sentNodes);
            var forwarded = new ConcurrentLinkedQueue<CallMessage>();
            var receiver = new CallReceiver(client, new AckSender(client),
                    new CallSignalingRouter(), new CallMessageBuffer(),
                    callId -> CALL_ID.equals(callId), (message, from) -> forwarded.add(message), notice -> {});

            var terminate = TerminateStanza.of(CALL_ID, PEER_DEVICE_LID, CallEndReason.HANGUP, List.of());
            receiver.handle(inboundEnvelope(terminate.toStanza(), PEER_DEVICE_LID, PEER_DEVICE_LID));

            assertEquals(1, forwarded.size(), "a PROCESS verdict must forward exactly one decoded message");
            assertTrue(forwarded.peek() instanceof TerminateStanza, "the forwarded message is the decoded terminate");
        }

        @Test
        @DisplayName("a stanza with no payload is dropped without an ack")
        void malformedStanzaDropped() throws IOException {
            var sentNodes = new ConcurrentLinkedQueue<Stanza>();
            var client = clientRecording(sentNodes);
            var forwarded = new ConcurrentLinkedQueue<CallMessage>();
            var receiver = new CallReceiver(client, new AckSender(client),
                    new CallSignalingRouter(), new CallMessageBuffer(),
                    callId -> true, (message, from) -> forwarded.add(message), notice -> {});

            var bare = new StanzaBuilder().description("call").attribute("from", PEER_DEVICE_LID).build();
            receiver.handle(bare);

            assertTrue(forwarded.isEmpty(), "no payload means nothing to forward");
            assertTrue(sentNodes.isEmpty(), "no payload means nothing to ack");
        }

        @Test
        @DisplayName("an offer_notice is acked with class=call type=offer_notice and surfaced to the offer-notice sink, never the engine sink")
        void offerNoticeSurfacedNotRoutedToEngine() throws IOException {
            var sentNodes = new ConcurrentLinkedQueue<Stanza>();
            var client = clientRecording(sentNodes);
            var forwarded = new ConcurrentLinkedQueue<CallMessage>();
            var notices = new ConcurrentLinkedQueue<OfferNoticeStanza>();
            var receiver = new CallReceiver(client, new AckSender(client),
                    new CallSignalingRouter(), new CallMessageBuffer(),
                    callId -> false, (message, from) -> forwarded.add(message), notices::add);

            var notice = new StanzaBuilder()
                    .description("offer_notice")
                    .attribute("call-id", CALL_ID)
                    .attribute("call-creator", PEER_DEVICE_LID)
                    .attribute("type", "group")
                    .attribute("media", "video")
                    .build();
            var envelope = new StanzaBuilder()
                    .description("call")
                    .attribute("from", PEER_DEVICE_LID)
                    .attribute("id", "stanza-" + CALL_ID)
                    .attribute("t", "1700000000")
                    .content(notice)
                    .build();
            receiver.handle(envelope);

            assertTrue(forwarded.isEmpty(), "an offer_notice is not engine signaling; got " + forwarded);
            assertEquals(1, notices.size(), "the offer_notice must reach the offer-notice sink; got " + notices);
            var decoded = notices.peek();
            assertEquals(CALL_ID, decoded.callId());
            assertTrue(decoded.group(), "type=group must decode as a group notice");
            assertTrue(decoded.video(), "media=video must decode as a video notice");
            var ack = outgoing(sentNodes, "ack");
            assertTrue(ack.isPresent(), "an offer_notice is acked with an <ack>; got " + sentNodes);
            assertEquals("call", ack.get().getAttributeAsString("class").orElse(null), "ack class must be call");
            assertEquals("offer_notice", ack.get().getAttributeAsString("type").orElse(null),
                    "ack type must echo the offer_notice tag");
        }
    }

    @Nested
    @DisplayName("TerminateReceiver (the \"terminate\" tag handler)")
    class TerminateReceiverRouting {
        @Test
        @DisplayName("a bare top-level terminate is decoded and forwarded to the calls sink")
        void bareTerminateRoutesToCalls2() throws IOException {
            var forwarded = new ConcurrentLinkedQueue<TerminateStanza>();
            var receiver = new TerminateReceiver((terminate, from) -> forwarded.add(terminate));

            var bare = TerminateStanza.of(CALL_ID, PEER_DEVICE_LID, CallEndReason.HANGUP, List.of()).toStanza();
            receiver.handle(bare);

            assertEquals(1, forwarded.size(), "a well-formed bare terminate must forward once");
            assertEquals(CALL_ID, forwarded.peek().callId().orElseThrow());
        }

        @Test
        @DisplayName("a bare terminate with no call-id is dropped")
        void terminateWithoutCallIdDropped() throws IOException {
            var forwarded = new ConcurrentLinkedQueue<TerminateStanza>();
            var receiver = new TerminateReceiver((terminate, from) -> forwarded.add(terminate));

            receiver.handle(new StanzaBuilder().description("terminate").attribute("reason", "hangup").build());

            assertTrue(forwarded.isEmpty(), "a terminate with no call-id cannot be associated with a call");
        }
    }

    @Nested
    @DisplayName("inbound reaches LiveCallsService and, when wired, the lifecycle controller")
    class LifecycleReach {
        @Test
        @DisplayName("an offer fed to a service backed by a wired controller fires CALL_OFFER_RECEIVED on the engine event sink")
        void offerReachesLifecycleEventSink() {
            var events = new RecordingEventSink();
            var service = serviceWithLifecycle(events);

            var offer = SignalingFixturesBridge.minimalOffer(CALL_ID, PEER_DEVICE_LID);
            service.handleInbound(offer, PEER_DEVICE_LID);

            assertTrue(events.emitted(CallEventType.CALL_OFFER_RECEIVED),
                    "an inbound offer must reach the lifecycle controller and fire CALL_OFFER_RECEIVED; got "
                            + events.eventTypes());
        }

        @Test
        @DisplayName("a terminate forwarded to the live service ends the tracked call through the lifecycle")
        void terminateReachesLifecycle() {
            var events = new RecordingEventSink();
            var service = serviceWithLifecycle(events);
            // Seed a live call (offer -> ring) so the controller tracks it, then end it with a terminate.
            service.handleInbound(SignalingFixturesBridge.minimalOffer(CALL_ID, PEER_DEVICE_LID), PEER_DEVICE_LID);
            events.clear();

            var terminate = TerminateStanza.of(CALL_ID, PEER_DEVICE_LID, CallEndReason.HANGUP, List.of());
            service.handleInbound(terminate, PEER_DEVICE_LID);

            assertTrue(events.emitted(CallEventType.CALL_TERMINATE_RECEIVED),
                    "a terminate for a tracked call must reach the lifecycle; got " + events.eventTypes());
        }
    }

    private static TestWhatsAppClient clientRecording(ConcurrentLinkedQueue<Stanza> sentStanzas) {
        var store = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
        store.addListener(new LinkedWhatsAppClientListener() {
            @Override
            public void onNodeSent(LinkedWhatsAppClient whatsapp, Stanza stanza) {
                sentStanzas.add(stanza);
            }
        });
        return TestWhatsAppClient.create().withStore(store);
    }

    private static Optional<Stanza> outgoing(ConcurrentLinkedQueue<Stanza> sentStanzas, String tag) {
        return sentStanzas.stream().filter(n -> tag.equals(n.description())).findFirst();
    }

    private LiveCallsService serviceWithLifecycle(RecordingEventSink events) {
        var client = clientRecording(new ConcurrentLinkedQueue<>());
        var controller = new LifecycleController(
                offerEnvelope -> new StanzaBuilder().description("ack").build(),
                new NoopCallKeyExchange(),
                new NoopVoipHostApi(),
                new NoopRegistry(),
                new PriorStateTransition(),
                new NoopTimers(),
                new NoopInfoUpdater(),
                events,
                new NoopMediaPlane());
        return new LiveCallsService(client, null, new StubMessageService(client), controller);
    }

    // Builds the minimal <call><offer> via CallStanza and routes it through client.sendNode so
    // the harness sees the outbound call; skips the real Signal encryption and device-list sync the
    // routing assertions do not exercise. Every other MessageService method is unreachable on this path.
    private static final class StubMessageService implements MessageService {
        private final LinkedWhatsAppClient client;

        StubMessageService(LinkedWhatsAppClient client) {
            this.client = client;
        }

        @Override
        public AckResult send(Jid chatJid, LinkedMessageContainer container) {
            throw new UnsupportedOperationException("StubMessageService.send not stubbed");
        }

        @Override
        public AckResult send(LinkedMessageInfo messageInfo) {
            throw new UnsupportedOperationException("StubMessageService.send not stubbed");
        }

        @Override
        public AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo) {
            throw new UnsupportedOperationException("StubMessageService.sendPeer not stubbed");
        }

        @Override
        public LinkedMessageInfo process(Stanza stanza) {
            throw new UnsupportedOperationException("StubMessageService.process not stubbed");
        }

        @Override
        public MessageService.CallPeerAddressing resolveCallPeerAddressing(Jid peer) {
            throw new UnsupportedOperationException("StubMessageService.resolveCallPeerAddressing not stubbed");
        }

        @Override
        public byte[] processCall(Jid senderJid, MessageEncryptionType encType, byte[] ciphertext) {
            return ciphertext;
        }

        @Override
        public void clearPendingMessages() {
        }
    }

    /**
     * Records every {@link CallEventType} emitted so the test can assert the lifecycle was reached.
     */
    private static final class RecordingEventSink implements CallLifecycleEventSink {
        private final List<CallEventType> events = new CopyOnWriteArrayList<>();

        @Override
        public void emit(CallEventType eventType, byte[] payload) {
            events.add(eventType);
        }

        boolean emitted(CallEventType type) {
            return events.contains(type);
        }

        List<CallEventType> eventTypes() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }

    /**
     * Returns the prior state on every transition so the controller's guarded transition fires its event
     * (the guard suppresses the event only when the prior state is absent or equals the new state).
     */
    private static final class PriorStateTransition implements CallStateTransition {
        private final java.util.concurrent.ConcurrentHashMap<String, CallLifecycleState> states =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public Optional<CallLifecycleState> transition(String callId, CallLifecycleState newState) {
            var prior = states.put(callId, newState);
            return Optional.of(prior == null ? CallLifecycleState.NONE : prior);
        }
    }

    private static final class NoopRegistry implements CallContextRegistry {
        @Override
        public com.github.auties00.cobalt.calls.engine.context.CallContext allocate(Call call,
                                                                                 CallLifecycleState initialState) {
            return null;
        }

        @Override
        public void release(String callId) {
        }
    }

    private static final class NoopTimers implements CallTimerScheduler {
        @Override
        public void arm(String callId, CallTimerKind kind, Runnable action) {
        }

        @Override
        public void cancel(String callId, CallTimerKind kind) {
        }

        @Override
        public void cancelAll(String callId) {
        }
    }

    private static final class NoopInfoUpdater implements CallInfoUpdater {
        @Override
        public void updateForEvent(String callId, CallEventType eventType) {
        }
    }

    private static final class NoopMediaPlane implements MediaPlane {
        @Override
        public Session bringUp(String callId, Stanza relay, java.util.List<Stanza> voipSettings, byte[] callKey,
                               boolean isCaller, boolean video, int participantCount,
                               com.github.auties00.cobalt.calls.engine.participant.CallMembership membership,
                               com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams streams,
                               com.github.auties00.cobalt.wire.core.jid.Jid peerDeviceJid,
                               Optional<String> electedRelayName,
                               com.github.auties00.cobalt.calls.engine.mediaplane.MediaSessionListener listener) {
            return () -> {
            };
        }
    }

    private static final class NoopCallKeyExchange implements CallKeyExchange {
        @Override
        public byte[] mintCallKey() {
            return new byte[32];
        }

        @Override
        public byte[] wrapCallKey(byte[] callKey) {
            return callKey;
        }

        @Override
        public List<CallKeyDistribution> encryptOfferFanout(Collection<Jid> deviceJids, byte[] plaintext) {
            return List.of();
        }

        @Override
        public List<CallRekeyEnvelope> encryptRekeyFanout(Collection<Jid> recipientDevices, byte[] plaintext) {
            return List.of();
        }

        @Override
        public Optional<byte[]> decryptCallKey(CallKeyDistribution slot, Jid senderJid) {
            return Optional.empty();
        }

        @Override
        public byte[] signedDeviceIdentity() {
            return new byte[0];
        }
    }

    private static final class NoopVoipHostApi implements VoipHostApi {
        @Override
        public void sendSignaling(Stanza stanza) {
        }

        @Override
        public int sendDatagram(byte[] payload, SocketAddress destination) {
            return 0;
        }

        @Override
        public List<InetAddress> resolveHost(String hostName) {
            return List.of();
        }

        @Override
        public byte[] randomBytes(int length) {
            return new byte[length];
        }

        @Override
        public Optional<Path> bweMlModelPath(int modelType) {
            return Optional.empty();
        }

        @Override
        public boolean isKnownContact(Jid participant) {
            return false;
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
     * Bridges to the package-private {@code SignalingFixtures.minimalOffer} so this test in package
     * {@code calls} can build a representative {@link OfferStanza} without re-declaring its long
     * constructor. Lives here rather than in {@code calls.signaling} so the routing assertions stay in
     * one file; it builds the same minimal offer the signaling tests use.
     */
    private static final class SignalingFixturesBridge {
        private static OfferStanza minimalOffer(String callId, Jid callCreator) {
            return new OfferStanza(callId, callCreator, null, null, null, null, null, null,
                    false, false, null, -1, -1, List.of(), List.of(), List.of(), List.of(), null,
                    null, null, null, null, null, null, List.of(), null);
        }
    }
}
