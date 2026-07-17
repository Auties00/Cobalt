package com.github.auties00.cobalt.calls;

import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.calls.engine.*;
import com.github.auties00.cobalt.calls.engine.context.*;
import com.github.auties00.cobalt.calls.engine.state.*;
import com.github.auties00.cobalt.calls.telemetry.*;
import com.github.auties00.cobalt.calls.crypto.CallKeyExchange;
import com.github.auties00.cobalt.calls.crypto.CallRekeyEnvelope;
import com.github.auties00.cobalt.calls.platform.VoipHostApi;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.calls.stream.AudioInput;
import com.github.auties00.cobalt.calls.stream.AudioOutput;
import com.github.auties00.cobalt.calls.stream.VideoInput;
import com.github.auties00.cobalt.calls.stream.VideoOutput;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.message.MessageService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import com.github.auties00.cobalt.calls.engine.event.CallEventType;
import com.github.auties00.cobalt.calls.engine.info.CallInfoUpdater;
import com.github.auties00.cobalt.calls.engine.event.CallLifecycleEventSink;
import com.github.auties00.cobalt.calls.engine.state.CallLifecycleState;
import com.github.auties00.cobalt.calls.engine.state.CallStateTransition;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerKind;
import com.github.auties00.cobalt.calls.engine.timer.CallTimerScheduler;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaPlane;
import com.github.auties00.cobalt.calls.engine.mediaplane.MediaStreams;
import com.github.auties00.cobalt.calls.engine.LifecycleController;
import com.github.auties00.cobalt.calls.engine.context.CallContext;
import com.github.auties00.cobalt.calls.engine.context.CallContextRegistry;
import com.github.auties00.cobalt.calls.engine.participant.CallMembership;

/**
 * Adversarial P10 proof that the public call API is typed on the {@code calls.stream} interfaces and that
 * the application-supplied streams reach the calls lifecycle and media plane with their capture/playback
 * roles preserved. The finishing migration retyped {@code startCall}/{@code acceptCall} (and the service's
 * {@code placeCall}/{@code placeGroupCall}/{@code accept}) off the legacy {@code call.stream.*} onto
 * {@link AudioOutput}/{@link AudioInput}/{@link VideoOutput}/{@link VideoInput}; this suite drives a real
 * {@link LiveCallsService} backed by a wired {@link LifecycleController} (assembled from minimal
 * interface fakes and a recording media plane) so the streams a caller passes can be followed end to end.
 *
 * <p>The harness mirrors {@code InboundRoutingWiringTest}: a {@link TestWhatsAppClient} over a real
 * temporary store and a real lifecycle controller whose nine collaborators are deterministic fakes, with the
 * offer-ack carrying a {@code <relay>} block so the group placement path actually brings up the (recording)
 * media plane. No socket, native unit, or transport is touched. The key assertion is on the
 * {@link MediaStreams} bundle the media plane receives: the public {@code audioOut} must arrive as the
 * media plane's {@code audioCapture} and the public {@code audioIn} as its {@code audioPlayback}, the
 * semantic capture-vs-playback mapping the migration had to preserve.
 */
@DisplayName("calls public call API accepts calls.stream types and reaches the lifecycle")
class PublicCallApiTest {
    private static final Jid SELF_PN = Jid.of("19153544650@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("39110693621863@lid");
    private static final Jid PEER = Jid.of("15551234567@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("120363000000000000@g.us");

    @Nested
    @DisplayName("placeGroupCall (the path that brings up the media plane)")
    class PlaceGroupCall {
        @Test
        @DisplayName("threads the public audio streams to the media plane with capture and playback roles preserved")
        void streamsReachMediaPlaneByRole() {
            var harness = new Harness();
            var audioOut = AudioOutput.fromSilence(); // capture source the app feeds into the call
            var audioIn = AudioInput.discard();     // playback sink the call feeds into the app

            var call = harness.service.placeGroupCall(Set.of(PEER), GROUP, audioOut, audioIn, null, null);

            assertNotNull(call, "placeGroupCall must return the live call view");
            assertEquals(GROUP, call.chatJid(), "a group call's chat is the group JID");
            assertTrue(call.isGroup());

            var bringUps = harness.mediaPlane.bringUps();
            assertEquals(1, bringUps.size(), "the group placement must bring up the media plane once");
            var streams = bringUps.get(0).streams();
            // The migration mandate: map by SEMANTICS, not the Input/Output word. The public audioOut is a
            // CAPTURE source; it must land on the media plane's audioCapture. The public audioIn is a
            // PLAYBACK sink; it must land on audioPlayback. Same instances, not merely same type.
            assertSame(audioOut, streams.audioCapture(),
                    "the public audioOut (capture source) must reach the media plane as audioCapture");
            assertSame(audioIn, streams.audioPlayback(),
                    "the public audioIn (playback sink) must reach the media plane as audioPlayback");
            assertNull(streams.videoCapture(), "an audio-only call carries no video capture source");
            assertNull(streams.videoPlayback(), "an audio-only call carries no video playback sink");
        }

        @Test
        @DisplayName("an audio call reaches CALL_OFFER_SENT on the lifecycle event sink")
        void reachesLifecycle() {
            var harness = new Harness();
            harness.service.placeGroupCall(Set.of(PEER), GROUP, AudioOutput.fromSilence(), AudioInput.discard(),
                    null, null);
            assertTrue(harness.events.emitted(CallEventType.CALL_OFFER_SENT),
                    "placing a call must reach the lifecycle controller and fire CALL_OFFER_SENT; got "
                            + harness.events.eventTypes());
        }

        @Test
        @DisplayName("a video call threads the video capture source and playback sink to the media plane")
        void videoStreamsReachMediaPlane() {
            var harness = new Harness();
            var audioOut = AudioOutput.fromSilence();
            var audioIn = AudioInput.discard();
            VideoOutput videoOut = VideoOutput.fromBlank(); // capture source
            VideoInput videoIn = VideoInput.discard();     // playback sink

            var call = harness.service.placeGroupCall(Set.of(PEER), GROUP, audioOut, audioIn, videoOut, videoIn);

            assertTrue(call.isVideo(), "supplying a video source makes the call a video call");
            var streams = harness.mediaPlane.bringUps().get(0).streams();
            assertSame(videoOut, streams.videoCapture(),
                    "the public videoOut (capture source) must reach the media plane as videoCapture");
            assertSame(videoIn, streams.videoPlayback(),
                    "the public videoIn (playback sink) must reach the media plane as videoPlayback");
        }

        @Test
        @DisplayName("the placed call is registered in the service runtime registry")
        void callIsRegistered() {
            var harness = new Harness();
            var call = harness.service.placeGroupCall(Set.of(PEER), GROUP, AudioOutput.fromSilence(),
                    AudioInput.discard(), null, null);
            assertTrue(harness.service.callExists(call.callId()),
                    "a placed call must be tracked by the service so it can be terminated");
            assertNotNull(harness.service.find(call.callId()));
        }
    }

    @Nested
    @DisplayName("argument validation on the public surface")
    class Validation {
        @Test
        @DisplayName("placeGroupCall rejects null required streams and an empty peer set")
        void rejectsBadArguments() {
            var harness = new Harness();
            assertThrows(NullPointerException.class, () -> harness.service.placeGroupCall(
                    Set.of(PEER), GROUP, null, AudioInput.discard(), null, null));
            assertThrows(NullPointerException.class, () -> harness.service.placeGroupCall(
                    Set.of(PEER), GROUP, AudioOutput.fromSilence(), null, null, null));
            assertThrows(IllegalArgumentException.class, () -> harness.service.placeGroupCall(
                    Set.of(), GROUP, AudioOutput.fromSilence(), AudioInput.discard(), null, null));
        }

        @Test
        @DisplayName("placeCall rejects a group JID, routing groups through placeGroupCall instead")
        void placeCallRejectsGroupJid() {
            var harness = new Harness();
            assertThrows(IllegalArgumentException.class, () -> harness.service.placeCall(
                    GROUP, AudioOutput.fromSilence(), AudioInput.discard(), null, null));
        }
    }

    /**
     * Builds a {@link LiveCallsService} over a wired {@link LifecycleController} whose collaborators
     * are deterministic fakes, with a {@link RecordingMediaPlane} that captures every bring-up's media
     * streams. The offer-ack sender returns an {@code <ack>} carrying a minimal {@code <relay>} block, so the
     * group placement path records a relay and brings the media plane up; the crypto fake mints a non-null
     * call key so the bring-up's relay-and-key precondition is met.
     */
    private static final class Harness {
        private final RecordingMediaPlane mediaPlane = new RecordingMediaPlane();
        private final RecordingEventSink events = new RecordingEventSink();
        private final LiveCallsService service;

        private Harness() {
            var store = MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
            var client = TestWhatsAppClient.create().withStore(store);
            var controller = new LifecycleController(
                    offerEnvelope -> ackWithRelay(),
                    new NoopCallKeyExchange(),
                    new RecordingVoipHostApi(),
                    new NoopRegistry(),
                    new PriorStateTransition(),
                    new NoopTimers(),
                    new NoopInfoUpdater(),
                    events,
                    mediaPlane);
            this.service = new LiveCallsService(client, null, new StubMessageService(client), controller);
        }

        private static Stanza ackWithRelay() {
            var relay = new StanzaBuilder()
                    .description("relay")
                    .attribute("uuid", "0123456789ABCDEF")
                    .content(new StanzaBuilder()
                            .description("hbh_key")
                            .content("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
                            .build())
                    .build();
            return new StanzaBuilder()
                    .description("ack")
                    .attribute("class", "call")
                    .content(relay)
                    .build();
        }
    }

    /**
     * Records every media-plane bring-up and the {@link MediaStreams} it carried, then hands back a
     * no-op closeable session.
     */
    private static final class RecordingMediaPlane implements MediaPlane {
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
            bringUps.add(new BringUp(callId, isCaller, video, List.copyOf(voipSettings), participantCount, streams));
            return () -> {
            };
        }

        record BringUp(String callId, boolean isCaller, boolean video, List<Stanza> voipSettings, int participantCount,
                       MediaStreams streams) {
        }
    }

    /**
     * Records every {@link CallEventType} the controller emits so a test can assert the lifecycle was reached.
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
    }

    /**
     * Returns the prior state on every transition so the controller's guarded transition fires its event.
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
        public com.github.auties00.cobalt.calls.engine.context.CallContext allocate(
                com.github.auties00.cobalt.wire.linked.call.Call call, CallLifecycleState initialState) {
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

    private static final class RecordingVoipHostApi implements VoipHostApi {
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
            var bytes = new byte[length];
            for (var i = 0; i < length; i++) {
                bytes[i] = (byte) (i + 1);
            }
            return bytes;
        }

        @Override
        public Optional<Path> bweMlModelPath(int modelType) {
            return Optional.empty();
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
     * A {@link MessageService} that no public-call-API path under test exercises (the offer rides the
     * lifecycle controller's offer-ack seam, not this service), so every method fails fast if reached.
     */
    private static final class StubMessageService implements MessageService {
        private final LinkedWhatsAppClient client;

        StubMessageService(LinkedWhatsAppClient client) {
            this.client = client;
        }

        @Override
        public AckResult send(Jid chatJid, LinkedMessageContainer container) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public AckResult send(LinkedMessageInfo messageInfo) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public LinkedMessageInfo process(Stanza stanza) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public CallPeerAddressing resolveCallPeerAddressing(Jid peer) {
            // Call addressing is always LID: resolve the peer to its LID user and a single LID device.
            var lid = Jid.of(peer.user() + "@lid");
            return new CallPeerAddressing(lid, List.of(lid));
        }

        @Override
        public byte[] processCall(Jid senderJid, MessageEncryptionType encType, byte[] ciphertext) {
            throw new UnsupportedOperationException("not stubbed");
        }

        @Override
        public void clearPendingMessages() {
        }
    }
}
