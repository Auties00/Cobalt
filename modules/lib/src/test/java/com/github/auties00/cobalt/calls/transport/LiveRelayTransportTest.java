package com.github.auties00.cobalt.calls.transport;

import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreams;
import com.github.auties00.cobalt.wire.linked.call.datachannel.SrtpAfbStreamsBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.transport.srtp.HbhSrtpRelay;
import com.github.auties00.cobalt.calls.transport.subscription.RtcpRxSubscriptionTable;
import com.github.auties00.cobalt.calls.transport.warp.BweConfigSender;
import com.github.auties00.cobalt.calls.transport.warp.WarpMessage;

/**
 * Verifies the relay transport's send, demux, and event wiring with a fake hop-by-hop SRTP context and a
 * fake data channel backend. The fake SRTP protects by appending a fixed marker tag and unprotects by
 * stripping it, so the test asserts the transport applies the context on the send path and strips it on the
 * receive path without the native SRTP library; the fake data channel captures everything the transport
 * writes as SCTP DATA and lets the test push inbound SCTP DATA messages back. The transport is driven with a
 * {@code null} ICE agent so {@link LiveRelayTransport#start()} reports the already-open backend synchronously
 * rather than launching the live ICE/DTLS bring-up, which needs a real relay.
 */
@DisplayName("LiveRelayTransport")
class LiveRelayTransportTest {
    @Test
    @DisplayName("start over an already-open backend emits a relay-create-success event")
    void startEmitsEvent() {
        var events = new ArrayList<TransportEvent>();
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, null, 0);
        transport.onTransportEvent(events::add);
        transport.start();
        assertEquals(List.of(TransportEvent.RELAY_CREATE_SUCCESS), events);
    }

    @Test
    @DisplayName("sendMedia protects the packet and writes it as SCTP DATA on the data channel")
    void sendMediaProtectsAndShips() {
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, null, 0);
        transport.start();
        var packet = new byte[64];
        packet[0] = (byte) 0x80;
        transport.sendMedia(packet, 12);
        assertEquals(1, channel.sent.size());
        assertEquals(12 + FakeHbhSrtp.TAG_LEN, channel.sent.getFirst().length);
    }

    @Test
    @DisplayName("an inbound RTP data-channel message is unprotected and reaches the media sink")
    void inboundRtpReachesSink() {
        var media = new ArrayList<byte[]>();
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, media::add, 0);
        transport.start();
        var inbound = new byte[20];
        inbound[0] = (byte) 0x80;
        channel.deliver(inbound);
        assertEquals(1, media.size());
        assertEquals(inbound.length - FakeHbhSrtp.TAG_LEN, media.getFirst().length);
    }

    @Test
    @DisplayName("sendStandaloneWarp appends the negotiated message-integrity tag and writes it on the channel")
    void standaloneWarpAppendsTag() {
        var channel = new FakeDataChannel(true);
        var authKey = new byte[32];
        var transport = newTransport(new FakeHbhSrtp(), channel, bytes -> {
        }, 10, authKey);
        transport.start();
        var warp = BweConfigSender.build(0, 300);
        transport.sendStandaloneWarp(warp);
        var expectedLength = warp.encode().length + 10;
        assertEquals(expectedLength, channel.sent.getFirst().length);
    }

    @Test
    @DisplayName("sendStandaloneWarp stamps the rolling-clock timestamp into the WARP header")
    void standaloneWarpStampsRollingClock() {
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, bytes -> {
        }, 0);
        transport.start();
        transport.sendStandaloneWarp(BweConfigSender.build(0, 300));
        var datagram = channel.sent.getFirst();
        // Byte 0 is the WARP type; bytes 2 and 3 are the big-endian rolling-ms clock sampled at send.
        assertEquals(WarpMessage.WARP_TYPE, datagram[0] & 0xff);
        var timestamp = ((datagram[WarpMessage.TIMESTAMP_OFFSET] & 0xff) << 8)
                | (datagram[WarpMessage.TIMESTAMP_OFFSET + 1] & 0xff);
        // The send happens within milliseconds of construction, well under the 65.5 s rollover, so the
        // sampled clock is a small, in-range millisecond count rather than the old fixed-zero placeholder.
        assertTrue(timestamp >= 0 && timestamp <= 0xFFFF, "timestamp out of 16-bit range: " + timestamp);
        assertTrue(timestamp < 10_000, "timestamp implausibly large for an immediate send: " + timestamp);
    }

    @Test
    @DisplayName("a send on a closed transport throws")
    void sendAfterCloseThrows() {
        var transport = newTransport(new FakeHbhSrtp(), new FakeDataChannel(true), null, 0);
        transport.start();
        transport.close();
        assertThrows(IllegalStateException.class, () -> transport.sendMedia(new byte[16], 8));
    }

    @Test
    @DisplayName("a relay transport with no negotiated tag reports no WARP message integrity")
    void noWarpMiByDefault() {
        var transport = newTransport(new FakeHbhSrtp(), new FakeDataChannel(true), null, 0);
        assertFalse(transport.hasWarpMessageIntegrity());
    }

    @Test
    @DisplayName("a report tick after an audio send writes a Sender Report immediately followed by a Source Description")
    void driveRtcpEmitsSenderReportWithSdes() {
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, null, 0);
        transport.start();
        var audio = new byte[64];
        audio[0] = (byte) 0x80;  // version two, no header extension
        audio[1] = (byte) 120;   // audio payload type
        transport.sendMedia(audio, 12);
        transport.tick(System.nanoTime());
        // Among everything the tick wrote (a keepalive, then the report compound), find the Sender Report.
        var compound = channel.sent.stream()
                .filter(message -> message.length > 4 && (message[1] & 0xFF) == 0xC8)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Sender Report was written"));
        // The Source Description (payload type 202) immediately follows the Sender Report in the datagram.
        var senderReportWords = ((compound[2] & 0xFF) << 8) | (compound[3] & 0xFF);
        var senderReportLength = (senderReportWords + 1) * 4;
        assertEquals(0xCA, compound[senderReportLength + 1] & 0xFF, "Source Description does not follow the Sender Report");
    }

    @Test
    @DisplayName("sendNack writes a 16-byte generic NACK standalone with the fixed sender SSRC 1")
    void sendNackWritesNack() {
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, null, 0);
        transport.start();
        transport.sendNack(0x12345678, List.of(100, 101, 103));
        // 100, 101, 103 are within one packet-identifier-plus-bitmask run, so exactly one record is sent.
        assertEquals(1, channel.sent.size());
        var nack = channel.sent.getFirst();
        assertEquals(0xCD, nack[1] & 0xFF);          // payload type 205 (RTPFB)
        assertEquals(0x00, nack[4] & 0xFF);
        assertEquals(0x00, nack[5] & 0xFF);
        assertEquals(0x00, nack[6] & 0xFF);
        assertEquals(0x01, nack[7] & 0xFF);          // fixed sender SSRC 0x00000001
    }

    @Test
    @DisplayName("close releases the data channel")
    void closeReleasesChannel() {
        var channel = new FakeDataChannel(true);
        var transport = newTransport(new FakeHbhSrtp(), channel, null, 0);
        transport.start();
        transport.close();
        assertTrue(channel.closed);
    }

    private static LiveRelayTransport newTransport(HbhSrtpRelay hbh, FakeDataChannel channel,
                                                   Consumer<byte[]> mediaSink, int warpMiTagLength) {
        return newTransport(hbh, channel, mediaSink, warpMiTagLength, warpMiTagLength > 0 ? new byte[32] : null);
    }

    private static LiveRelayTransport newTransport(HbhSrtpRelay hbh, FakeDataChannel channel,
                                                   Consumer<byte[]> mediaSink, int warpMiTagLength,
                                                   byte[] warpAuthKey) {
        var sink = mediaSink == null ? (Consumer<byte[]>) bytes -> {
        } : mediaSink;
        return new LiveRelayTransport(hbh, warpAuthKey, warpMiTagLength, null, null, null, sink, null,
                (payload, destination) -> payload.length, channel, null, null);
    }

    /**
     * A data channel backend fake: it captures every message the transport writes as SCTP DATA, reports a
     * fixed readiness, and lets the test push inbound SCTP DATA messages to the registered consumer.
     */
    private static final class FakeDataChannel implements LiveRelayTransport.DataChannel {
        private final List<byte[]> sent = new ArrayList<>();
        private final boolean ready;
        private Consumer<byte[]> consumer;
        private boolean closed;

        private FakeDataChannel(boolean ready) {
            this.ready = ready;
        }

        private void deliver(byte[] message) {
            if (consumer != null) {
                consumer.accept(message);
            }
        }

        @Override
        public void connect() {
            // The fake is already open; a real backend runs the DTLS handshake and SCTP connect here.
        }

        @Override
        public void feedDtlsRecord(byte[] record) {
            // The fake does not run DTLS, so an inbound record is ignored.
        }

        @Override
        public boolean send(byte[] message) {
            sent.add(message);
            return true;
        }

        @Override
        public void onMessage(Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        public boolean isReady() {
            return ready && !closed;
        }

        @Override
        public long bufferedAmount() {
            return 0;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /**
     * A hop-by-hop SRTP fake that protects by growing the length by a fixed tag and unprotects by
     * shrinking it, standing in for the native context so the transport wiring can be tested in
     * isolation.
     */
    private static final class FakeHbhSrtp implements HbhSrtpRelay {
        private static final int TAG_LEN = 10;

        @Override
        public int protectRtp(byte[] packet, int length) {
            return length + TAG_LEN;
        }

        @Override
        public int unprotectRtp(byte[] packet, int length) {
            return length - TAG_LEN;
        }

        @Override
        public int protectRtcp(byte[] packet, int length) {
            return length + TAG_LEN;
        }

        @Override
        public int unprotectRtcp(byte[] packet, int length) {
            return length - TAG_LEN;
        }

        @Override
        public RtcpRxSubscriptionTable rtcpFeedbackSubscriptions() {
            return new RtcpRxSubscriptionTable();
        }

        @Override
        public SrtpAfbStreams srtpAfbStreams() {
            return new SrtpAfbStreamsBuilder().srtpAfb(List.of()).build();
        }

        @Override
        public void close() {
        }
    }
}
