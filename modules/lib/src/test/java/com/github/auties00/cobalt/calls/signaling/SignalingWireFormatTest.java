package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.calls.capability.VoipCapabilities;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.calls.signaling.receive.CallAckParser;
import com.github.auties00.cobalt.calls.signaling.receive.CallSignalingRouter;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.CallCapability;
import com.github.auties00.cobalt.calls.signaling.session.CallCodecDescriptor;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.calls.signaling.session.CallMediaDescriptor;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.RingingStanza;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;

/**
 * Adversarial wire-format verification for the {@code calls.signaling} plane against SPEC sections
 * 1-3, 5, 6, and 9. The wire bytes are derived independently from the spec grammar and the RE ground
 * truth and then checked against the produced {@link Stanza} tree (tags, attributes, nesting), rather
 * than trusting the implementation's own constants.
 */
@DisplayName("calls signaling wire format")
class SignalingWireFormatTest {
    // A device JID for the call creator (call-creator is always a device JID, SPEC 1); LID-served so
    // the LID-only inbound gate in CallSignalingRouter accepts it.
    private static final Jid CALL_CREATOR = Jid.of("44444444", JidServer.lid(), 7, 0);
    private static final Jid PEER_USER_LID = Jid.of("44444444", JidServer.lid());
    private static final String CALL_ID = "FACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACEFACE";

    private static StanzaBuilder callChild(Stanza payload) {
        return new StanzaBuilder()
                .description(CallStanza.ELEMENT)
                .attribute("from", PEER_USER_LID)
                .attribute("id", "stanza-1")
                .content(payload);
    }

    @Nested
    @DisplayName("CallStanza envelope (SPEC 1)")
    class Envelope {
        @Test
        @DisplayName("wraps an action in <call to id> with the action as the single child")
        void wrapsOffer() {
            var offer = SignalingFixtures.minimalOffer(CALL_ID, CALL_CREATOR);
            var call = CallStanza.toCall(offer, PEER_USER_LID, "abc123");

            assertEquals("call", call.description());
            assertEquals(PEER_USER_LID, call.getAttributeAsJid("to").orElseThrow());
            assertEquals("abc123", call.getAttributeAsString("id").orElseThrow());
            var child = call.getChild().orElseThrow();
            assertEquals("offer", child.description());
            // the action stanza already carries the universal header; the envelope adds only addressing
            assertEquals(CALL_ID, child.getAttributeAsString("call-id").orElseThrow());
            assertEquals(CALL_CREATOR, child.getAttributeAsJid("call-creator").orElseThrow());
        }

        @Test
        @DisplayName("an unknown child tag parses to an empty result rather than throwing")
        void unknownChildIsEmpty() {
            var unknown = new StanzaBuilder().description("not_a_call_action").build();
            assertTrue(CallStanza.parse(unknown).isEmpty());
        }
    }

    @Nested
    @DisplayName("offer grammar (SPEC 3)")
    class Offer {
        @Test
        @DisplayName("stamps call-id/call-creator, emits flat audio and a net-medium child, and nests capability, destination, media")
        void offerStructure() {
            var capabilities = List.of(new CallCapability(1, HexFormat.of().parseHex("f709e4bb13")));
            var audio = List.of(CallCodecDescriptor.audio("opus", 16000));
            var keyDistribution = List.of(new CallKeyDistribution(
                    Jid.of("44444444", JidServer.lid(), 7, 0), 2, "pkmsg", 0,
                    HexFormat.of().parseHex("deadbeef")));
            var media = new CallMediaDescriptor(3, 16000);
            var offer = new OfferStanza(CALL_ID, CALL_CREATOR, null, null, null, null, null, null,
                    false, false, null, -1, 3, capabilities, audio, List.of(), keyDistribution, media,
                    null, null, null, null, null, null, List.of(), null);

            var node = offer.toStanza();
            assertEquals("offer", node.description());
            assertEquals(CALL_ID, node.getAttributeAsString("call-id").orElseThrow());
            assertEquals(CALL_CREATOR, node.getAttributeAsJid("call-creator").orElseThrow());
            // <net medium="3"/> child element, SPEC 3 offer
            var net = node.getChild("net").orElseThrow();
            assertEquals(3, net.getAttributeAsInt("medium").orElseThrow());

            // <capability ver="1">f709e4bb13</capability>
            var cap = node.getChild("capability").orElseThrow();
            assertEquals(1, cap.getAttributeAsInt("ver").orElseThrow());
            assertArrayEquals(HexFormat.of().parseHex("f709e4bb13"), cap.toContentBytes().orElseThrow());

            // <audio enc="opus" rate="16000"/> flat element
            var audioNode = node.getChild("audio").orElseThrow();
            assertEquals("opus", audioNode.getAttributeAsString("enc").orElseThrow());
            assertEquals(16000, audioNode.getAttributeAsInt("rate").orElseThrow());

            // <destination><to jid><enc v type count>CT</enc></to></destination>  (SPEC 8 fanout)
            var destination = node.getChild("destination").orElseThrow();
            var to = destination.getChild("to").orElseThrow();
            assertEquals(Jid.of("44444444", JidServer.lid(), 7, 0), to.getAttributeAsJid("jid").orElseThrow());
            var enc = to.getChild("enc").orElseThrow();
            assertEquals("pkmsg", enc.getAttributeAsString("type").orElseThrow());
            assertEquals("2", enc.getAttributeAsString("v").orElseThrow());
            assertArrayEquals(HexFormat.of().parseHex("deadbeef"), enc.toContentBytes().orElseThrow());

            // <media enc=N rate=R/>
            var mediaNode = node.getChild("media").orElseThrow();
            assertEquals(3, mediaNode.getAttributeAsInt("enc").orElseThrow());
            assertEquals(16000, mediaNode.getAttributeAsInt("rate").orElseThrow());
        }

        @Test
        @DisplayName("a group offer carries group-jid and joinable='1' as ASCII flags, no key fanout")
        void groupOfferFlags() {
            var group = Jid.of("120363000000000000", JidServer.groupOrCommunity());
            var offer = new OfferStanza(CALL_ID, CALL_CREATOR, null, null, null, group, null, null,
                    true, false, null, -1, -1, List.of(), List.of(), List.of(), List.of(), null,
                    null, null, null, null, null, null, List.of(), null);
            var node = offer.toStanza();
            assertEquals(group, node.getAttributeAsJid("group-jid").orElseThrow());
            // SPEC: booleans serialize as the ASCII characters '1'/'0'
            assertEquals("1", node.getAttributeAsString("joinable").orElseThrow());
            // group call ships no call key in the offer
            assertTrue(node.getChild("destination").isEmpty());
        }
    }

    @Nested
    @DisplayName("accept grammar (SPEC 3)")
    class Accept {
        @Test
        @DisplayName("emits capability, flat audio, net-medium child, enc key, media, transport under <accept>")
        void acceptStructure() {
            var capabilities = List.of(new CallCapability(1, HexFormat.of().parseHex("f709e4bb13")));
            var audio = List.of(CallCodecDescriptor.audio("opus", 16000));
            var encKey = new StanzaBuilder().description("enc").content(HexFormat.of().parseHex("cafe")).build();
            var transport = new StanzaBuilder().description("transport").attribute("call-id", CALL_ID).build();
            var media = new CallMediaDescriptor(5, 48000);
            var accept = new AcceptStanza(CALL_ID, CALL_CREATOR, 2, capabilities, audio, List.of(),
                    List.of(encKey), media, null, transport, null);

            var node = accept.toStanza();
            assertEquals("accept", node.description());
            assertEquals(CALL_ID, node.getAttributeAsString("call-id").orElseThrow());
            assertEquals(2, node.getChild("net").orElseThrow().getAttributeAsInt("medium").orElseThrow());
            assertTrue(node.getChild("capability").isPresent());
            assertEquals("opus", node.getChild("audio").orElseThrow().getAttributeAsString("enc").orElseThrow());
            assertArrayEquals(HexFormat.of().parseHex("cafe"),
                    node.getChild("enc").orElseThrow().toContentBytes().orElseThrow());
            assertEquals(48000, node.getChild("media").orElseThrow().getAttributeAsInt("rate").orElseThrow());
            assertTrue(node.getChild("transport").isPresent());
        }
    }

    @Nested
    @DisplayName("terminate grammar (SPEC 3)")
    class Terminate {
        @Test
        @DisplayName("writes reason on the reason attribute and fans out <destination><to>")
        void terminateStructure() {
            var d1 = Jid.of("44444444", JidServer.lid(), 1, 0);
            var d2 = Jid.of("44444444", JidServer.lid(), 2, 0);
            var terminate = TerminateStanza.of(CALL_ID, CALL_CREATOR, CallEndReason.HANGUP, List.of(d1, d2));

            var node = terminate.toStanza();
            assertEquals("terminate", node.description());
            // SPEC 3: the reason literal is written on the reason attribute, the spelling the captures carry
            assertEquals("hangup", node.getAttributeAsString("reason").orElseThrow());
            var dests = node.getChild("destination").orElseThrow().streamChildren("to")
                    .map(to -> to.getAttributeAsJid("jid").orElseThrow())
                    .toList();
            assertEquals(List.of(d1, d2), dests);
        }

        @Test
        @DisplayName("decodes the reason attribute into a typed end reason")
        void terminateReasonAttribute() {
            // the reason literal is carried on the reason attribute (SPEC 3)
            var node = CallMessagesTestSupport.stamp("terminate", CALL_ID, CALL_CREATOR)
                    .attribute("reason", "accepted_elsewhere")
                    .build();
            var decoded = TerminateStanza.of(node);
            assertEquals(CallEndReason.ACCEPTED_ELSEWHERE, decoded.reason());
            assertEquals("accepted_elsewhere", decoded.reasonWire());
        }
    }

    @Nested
    @DisplayName("offer -> relay ACK (SPEC 5)")
    class OfferAck {
        @Test
        @DisplayName("parses <ack class='call' type='offer'> with a populated <relay> into an accept outcome")
        void offerAckSuccess() {
            // SPEC 5: the relay credentials arrive as the synchronous <ack class="call"> return value
            var relay = new StanzaBuilder()
                    .description("relay")
                    .attribute("call-creator", CALL_CREATOR)
                    .attribute("call-id", CALL_ID)
                    .attribute("uuid", "relay-uuid")
                    .content(new StanzaBuilder().description("key").content("0123456789abcdef").build())
                    .build();
            var ack = new StanzaBuilder()
                    .description("ack")
                    .attribute("class", "call")
                    .attribute("type", "offer")
                    .attribute("id", "stanza-1")
                    .attribute("from", PEER_USER_LID)
                    .content(relay)
                    .build();

            var outcome = CallAckParser.parse(ack).orElseThrow();
            assertTrue(outcome.isAck());
            assertFalse(outcome.isNack());
            assertEquals("offer", outcome.type().orElseThrow());
            // <key> rides VERBATIM as the pjsip password, not base64-decoded (SPEC 3 relay)
            assertArrayEquals("0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    outcome.relay().orElseThrow().keyValue().orElseThrow());
        }

        @Test
        @DisplayName("parses <ack ... error=N> as a NACK whose relay holds only denormalised header")
        void offerAckNack() {
            // SPEC 5 NACK: <ack ... error=N><relay/></ack> with relay carrying only call-creator/call-id
            var relay = new StanzaBuilder()
                    .description("relay")
                    .attribute("call-creator", CALL_CREATOR)
                    .attribute("call-id", CALL_ID)
                    .build();
            var ack = new StanzaBuilder()
                    .description("ack")
                    .attribute("class", "call")
                    .attribute("type", "offer")
                    .attribute("id", "stanza-1")
                    .attribute("error", 404)
                    .content(relay)
                    .build();

            var outcome = CallAckParser.parse(ack).orElseThrow();
            assertTrue(outcome.isNack());
            assertEquals(404, outcome.error().orElseThrow());
        }
    }

    @Nested
    @DisplayName("VoipCapabilities serialization (SPEC 9.1)")
    class Capabilities {
        @Test
        @DisplayName("standard().serialize() equals the captured literal 01 05 F7 09 E4 BB 13")
        void standardSerializeEqualsLiteral() {
            var expected = HexFormat.of().parseHex("0105f709e4bb13");
            assertArrayEquals(expected, VoipCapabilities.standard().serialize());
        }

        @Test
        @DisplayName("deserialize(standard) round-trips and the masks expand to the 22 captured indices")
        void standardRoundTrip() {
            var bytes = VoipCapabilities.standard().serialize();
            var parsed = VoipCapabilities.deserialize(bytes);
            assertArrayEquals(bytes, parsed.serialize());
            // independently derived from F7 09 E4 BB 13 (LSB-first), SPEC 9.1
            int[] indices = {0, 1, 2, 4, 5, 6, 7, 8, 11, 18, 21, 22, 23, 24, 25, 27, 28, 29, 31, 32, 33, 36};
            for (var index : indices) {
                assertTrue(parsed.contains(index, 1), () -> "expected capability bit " + index + " set");
            }
            assertEquals(indices.length, parsed.namedCapabilities().size());
        }

        @Test
        @DisplayName("contains() reads byte index>>3 bit index&7 least-significant-first")
        void contains() {
            // byte 0 = 0xF7 = 1111 0111 -> bit 3 (index 3) clear, bit 4 (index 4) set
            var caps = VoipCapabilities.standard();
            assertFalse(caps.contains(3, 1));
            assertTrue(caps.contains(4, 1));
        }
    }

    @Nested
    @DisplayName("inbound classification + routing (SPEC 1, 5)")
    class Routing {
        private final CallSignalingRouter router = new CallSignalingRouter();

        // Feeds router.classify by extracting its fields (tag, call-id, call-creator) off a rendered payload
        //  stanza, the same way CallReceiver reads them from the wire.
        private CallSignalingRouter.Verdict classify(Stanza payload, Jid senderLid, boolean callExists) {
            var description = payload.description();
            var callId = payload.getAttributeAsString("call-id", null);
            var callCreator = payload.getAttributeAsJid("call-creator", null);
            return router.classify(description, callId, callCreator, senderLid, callExists);
        }

        @Test
        @DisplayName("a LID-addressed <offer> for an existing call routes to PROCESS and parses to OfferStanza")
        void offerRoutesToOfferStanza() {
            var offer = SignalingFixtures.minimalOffer(CALL_ID, CALL_CREATOR);
            var call = callChild(offer.toStanza()).build();
            var payload = call.getChild().orElseThrow();

            var verdict = classify(payload, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.PROCESS, verdict.disposition());
            assertSame(SignalingType.OFFER, verdict.type().orElseThrow());

            var message = CallStanza.parse(payload).orElseThrow();
            assertInstanceOf(OfferStanza.class, message);
        }

        @ParameterizedTest
        @EnumSource(value = SignalingFixtures.Kind.class)
        @DisplayName("each representative inbound action classifies to its type and parses to its record")
        void inboundRoutesToRecord(SignalingFixtures.Kind kind) {
            var payload = kind.build(CALL_ID, CALL_CREATOR).toStanza();

            var verdict = classify(payload, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.PROCESS, verdict.disposition(),
                    () -> kind + " should route to PROCESS");
            assertSame(kind.expectedType(), verdict.type().orElse(null),
                    () -> kind + " should classify to " + kind.expectedType());

            var message = CallStanza.parse(payload).orElseThrow();
            assertSame(kind.recordClass(), message.getClass(),
                    () -> kind + " should parse to " + kind.recordClass().getSimpleName());
        }

        @Test
        @DisplayName("an offer for a not-yet-existing call is BUFFERed rather than processed")
        void offerBuffersWhenNoCall() {
            var offer = SignalingFixtures.minimalOffer(CALL_ID, CALL_CREATOR);
            var verdict = classify(offer.toStanza(), PEER_USER_LID, false);
            assertEquals(CallSignalingRouter.Disposition.BUFFER, verdict.disposition());
            assertEquals(CALL_ID, verdict.callId().orElseThrow());
        }

        @Test
        @DisplayName("a payload with no call-id is DROPped with an empty type")
        void missingCallIdDrops() {
            var bad = new StanzaBuilder().description("offer").attribute("call-creator", CALL_CREATOR).build();
            var verdict = classify(bad, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.DROP, verdict.disposition());
            assertTrue(verdict.type().isEmpty());
        }

        @Test
        @DisplayName("a non-LID-addressed stanza is DROPped even when well-formed")
        void nonLidDrops() {
            var pnCreator = Jid.of("44444444", JidServer.user(), 7, 0);
            var node = CallMessagesTestSupport.stamp("offer", CALL_ID, pnCreator).build();
            var verdict = classify(node, null, true);
            assertEquals(CallSignalingRouter.Disposition.DROP, verdict.disposition());
            // the type still resolves; only the LID context gate fails
            assertSame(SignalingType.OFFER, verdict.type().orElseThrow());
        }

        @Test
        @DisplayName("a decodable <ringing> with no taxonomy ordinal routes to PROCESS with an empty type")
        void ringingRoutesToProcessWithEmptyType() {
            // SPEC 3 lists <ringing call-id call-creator/> and the RE ground truth confirms ringing is a
            // real inbound signal. It has no SignalingType entry, so ofWireTag returns empty, but
            // CallStanza decodes it; the router falls back to the parser-known tag set and routes it
            // to PROCESS so a RingingStanza reaches the sink. The verdict type stays empty because the
            // action carries no taxonomy ordinal.
            var ringing = new RingingStanza(CALL_ID, CALL_CREATOR).toStanza();
            assertTrue(CallStanza.parse(ringing).isPresent(), "parser decodes <ringing>");
            var verdict = classify(ringing, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.PROCESS, verdict.disposition(),
                    "router routes <ringing> via the parser-known tag fallback");
            assertTrue(verdict.type().isEmpty(), "ringing carries no taxonomy ordinal");
            assertEquals(CALL_ID, verdict.callId().orElseThrow());
            assertInstanceOf(RingingStanza.class, CallStanza.parse(ringing).orElseThrow());
        }

        @Test
        @DisplayName("a decodable <raise_hand> with no taxonomy ordinal routes to PROCESS with an empty type")
        void raiseHandRoutesToProcessWithEmptyType() {
            // Same fallback as <ringing>: raise_hand is a real inbound signal in the RE ground truth,
            // decodable by the parser but absent from SignalingType, so the router routes it via the
            // parser-known tag set rather than dropping it.
            var raiseHand = new RaiseHandStanza(CALL_ID, CALL_CREATOR, true, false).toStanza();
            assertTrue(CallStanza.parse(raiseHand).isPresent(), "parser decodes <raise_hand>");
            var verdict = classify(raiseHand, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.PROCESS, verdict.disposition(),
                    "router routes <raise_hand> via the parser-known tag fallback");
            assertTrue(verdict.type().isEmpty(), "raise_hand carries no taxonomy ordinal");
            assertInstanceOf(RaiseHandStanza.class, CallStanza.parse(raiseHand).orElseThrow());
        }

        @Test
        @DisplayName("an ordinal-less decodable action buffers when its call object does not yet exist")
        void ringingBuffersWhenNoCall() {
            // the ordinal-less fallback honours the same buffer-before-call gate as a taxonomy action
            var ringing = new RingingStanza(CALL_ID, CALL_CREATOR).toStanza();
            var verdict = classify(ringing, PEER_USER_LID, false);
            assertEquals(CallSignalingRouter.Disposition.BUFFER, verdict.disposition());
            assertEquals(CALL_ID, verdict.callId().orElseThrow());
            assertTrue(verdict.type().isEmpty());
        }

        @Test
        @DisplayName("a tag that is neither a taxonomy type nor a known decoder is still DROPped")
        void trulyUnknownTagDrops() {
            var unknown = CallMessagesTestSupport.stamp("not_a_call_action", CALL_ID, CALL_CREATOR).build();
            var verdict = classify(unknown, PEER_USER_LID, true);
            assertEquals(CallSignalingRouter.Disposition.DROP, verdict.disposition());
            assertTrue(verdict.type().isEmpty());
        }
    }

    @Nested
    @DisplayName("RingingStanza/RaiseHandStanza carry no taxonomy ordinal (SPEC 2)")
    class NoOrdinal {
        @Test
        @DisplayName("ringing reports a null taxonomy type")
        void ringingNullType() {
            assertNull(new RingingStanza(CALL_ID, CALL_CREATOR).type());
        }

        @Test
        @DisplayName("raise_hand reports a null taxonomy type")
        void raiseHandNullType() {
            assertNull(new RaiseHandStanza(CALL_ID, CALL_CREATOR, true, false).type());
        }
    }
}
