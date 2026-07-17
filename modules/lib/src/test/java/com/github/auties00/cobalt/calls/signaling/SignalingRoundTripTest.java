package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.calls.engine.control.PrivacyToken;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import com.github.auties00.cobalt.calls.signaling.incall.MuteV2Stanza;
import com.github.auties00.cobalt.calls.signaling.incall.RaiseHandStanza;
import com.github.auties00.cobalt.calls.signaling.session.AcceptStanza;
import com.github.auties00.cobalt.calls.signaling.session.CallCapability;
import com.github.auties00.cobalt.calls.signaling.session.CallCodecDescriptor;
import com.github.auties00.cobalt.calls.signaling.session.CallKeyDistribution;
import com.github.auties00.cobalt.calls.signaling.session.CallMediaDescriptor;
import com.github.auties00.cobalt.calls.signaling.session.OfferStanza;
import com.github.auties00.cobalt.calls.signaling.session.RejectStanza;
import com.github.auties00.cobalt.calls.signaling.session.RingingStanza;
import com.github.auties00.cobalt.calls.signaling.session.CallSummary;
import com.github.auties00.cobalt.calls.signaling.session.TerminateStanza;

/**
 * build -> parse -> equal round trips for every tag-routable {@link CallMessage}, asserting each
 * record reconstructs itself from the {@link Stanza} it produced.
 */
@DisplayName("calls signaling round trip")
class SignalingRoundTripTest {
    private static final Jid CALL_CREATOR = Jid.of("44444444", JidServer.lid(), 7, 0);
    private static final String CALL_ID = "0123456789ABCDEFabcdef0123456789ABCDEFabcdef0123456789ABCDEF0123";

    @ParameterizedTest
    @EnumSource(SignalingFixtures.Kind.class)
    @DisplayName("each representative action round-trips build -> parse -> equal")
    void representativeRoundTrip(SignalingFixtures.Kind kind) {
        var original = kind.build(CALL_ID, CALL_CREATOR);
        var reparsed = CallStanza.parse(original.toStanza()).orElseThrow();
        assertEquals(original, reparsed);
    }

    @Test
    @DisplayName("a fully-populated offer round-trips with its capability, codecs, fanout, and media")
    void richOfferRoundTrip() {
        var capabilities = List.of(new CallCapability(1, HexFormat.of().parseHex("f709e4bb13")));
        var audio = List.of(
                CallCodecDescriptor.audio("opus", 16000),
                CallCodecDescriptor.audio("opus", 48000));
        var video = List.of(CallCodecDescriptor.video("H264", "h.264", 0, 0, 0));
        var fanout = List.of(
                new CallKeyDistribution(Jid.of("44444444", JidServer.lid(), 7, 0), 2, "pkmsg", 0,
                        HexFormat.of().parseHex("aabbcc")),
                new CallKeyDistribution(Jid.of("44444444", JidServer.lid(), 9, 0), 2, "msg", 1,
                        HexFormat.of().parseHex("ddeeff")));
        var media = new CallMediaDescriptor(3, 16000);
        var groupInfo = new StanzaBuilder().description("group_info").attribute("phash", "2:abc").build();
        var voipSettings = List.of(
                new StanzaBuilder().description("voip_settings").attribute("type", "default")
                        .content(HexFormat.of().parseHex("00")).build());
        var original = new OfferStanza(CALL_ID, CALL_CREATOR,
                Jid.of("44444444", JidServer.user()), null, "alice", null, "sched-1", "phone",
                true, false, null, 1700000000L, 3,
                capabilities, audio, video, fanout, media, null, groupInfo,
                HexFormat.of().parseHex("ad00ad00"), new PrivacyToken(HexFormat.of().parseHex("70697670")),
                null, null, voipSettings, null);

        var reparsed = CallStanza.parse(original.toStanza()).orElseThrow();
        assertEquals(original, reparsed);
    }

    @Test
    @DisplayName("an accept with key blobs and an embedded transport round-trips")
    void richAcceptRoundTrip() {
        var capabilities = List.of(new CallCapability(1, HexFormat.of().parseHex("f709e4bb13")));
        var audio = List.of(CallCodecDescriptor.audio("opus", 16000));
        var encKeys = List.of(
                new StanzaBuilder().description("enc").content(HexFormat.of().parseHex("0011")).build(),
                new StanzaBuilder().description("enc").content(HexFormat.of().parseHex("2233")).build());
        var transport = new StanzaBuilder()
                .description("transport")
                .attribute("call-id", CALL_ID)
                .attribute("transport-message-type", 13)
                .build();
        var media = new CallMediaDescriptor(5, 48000);
        var original = new AcceptStanza(CALL_ID, CALL_CREATOR, 2, capabilities, audio, List.of(), encKeys, media,
                null, transport, null);

        var reparsed = CallStanza.parse(original.toStanza()).orElseThrow();
        assertEquals(original, reparsed);
    }

    @Test
    @DisplayName("a terminate with reason, fanout, and call_summary round-trips")
    void richTerminateRoundTrip() {
        var d1 = Jid.of("44444444", JidServer.lid(), 1, 0);
        var d2 = Jid.of("44444444", JidServer.lid(), 2, 0);
        var summary = new CallSummary(CALL_ID, CALL_CREATOR, 42, "audio", List.of(
                new CallSummary.Participant(d1, d2, "connected", false),
                new CallSummary.Participant(d2, null, "left", true)));
        var original = new TerminateStanza(CALL_ID, CALL_CREATOR, CallEndReason.MEDIA_RX_TIMEOUT,
                "media_rx_timeout", 2, true, 11, "hint-x", summary, List.of(d1, d2));

        var reparsed = CallStanza.parse(original.toStanza()).orElseThrow();
        assertEquals(original, reparsed);
    }

    @Test
    @DisplayName("a mute_v2 peer-mute request round-trips distinctly from a self-state report")
    void muteV2PeerRequestRoundTrip() {
        var request = MuteV2Stanza.ofPeerRequest(CALL_ID, CALL_CREATOR, true);
        assertEquals(request, CallStanza.parse(request.toStanza()).orElseThrow());

        var selfUnmuted = MuteV2Stanza.ofSelfState(CALL_ID, CALL_CREATOR, false, false);
        assertEquals(selfUnmuted, CallStanza.parse(selfUnmuted.toStanza()).orElseThrow());
    }

    @Test
    @DisplayName("a reject preserves an unrecognized reason literal verbatim across a round trip")
    void rejectUnknownReasonRoundTrip() {
        // SPEC 3: an unrecognized literal classifies to UNKNOWN but must re-emit verbatim
        var original = new RejectStanza(CALL_ID, CALL_CREATOR, CallEndReason.UNKNOWN, "some_future_reason", -1);
        var reparsed = assertInstanceOf(RejectStanza.class,
                CallStanza.parse(original.toStanza()).orElseThrow());
        assertEquals(original, reparsed);
        assertEquals("some_future_reason", reparsed.reasonWire());
        assertEquals(CallEndReason.UNKNOWN, reparsed.reason());
    }

    @Test
    @DisplayName("ringing and raise_hand round-trip directly through the stanza codec")
    void unorderedActionsRoundTrip() {
        // these have no taxonomy ordinal but ARE build/parse symmetric through CallStanza
        var ringing = new RingingStanza(CALL_ID, CALL_CREATOR);
        assertEquals(ringing, CallStanza.parse(ringing.toStanza()).orElseThrow());

        var raiseHand = new RaiseHandStanza(CALL_ID, CALL_CREATOR, true, true);
        assertEquals(raiseHand, CallStanza.parse(raiseHand.toStanza()).orElseThrow());
    }
}
