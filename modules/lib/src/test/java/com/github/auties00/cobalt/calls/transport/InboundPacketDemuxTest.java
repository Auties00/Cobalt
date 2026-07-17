package com.github.auties00.cobalt.calls.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("InboundPacketDemux RFC 7983 classification and routing")
class InboundPacketDemuxTest {
    private static final SocketAddress SOURCE = new InetSocketAddress("31.13.86.63", 3478);

    @Nested
    @DisplayName("classify")
    class Classify {
        @ParameterizedTest
        @CsvSource({
                "0, STUN", "3, STUN", "64, STUN", "79, STUN",
                "20, DTLS", "63, DTLS",
                "128, RTP", "191, RTP",
                "4, UNKNOWN", "19, UNKNOWN", "80, UNKNOWN", "127, UNKNOWN", "192, UNKNOWN", "255, UNKNOWN"
        })
        @DisplayName("maps each leading-byte boundary to its class")
        void boundaries(int leadingByte, PacketClass expected) {
            assertEquals(expected, InboundPacketDemux.classify(new byte[]{(byte) leadingByte, 0, 0, 0}));
        }

        @Test
        @DisplayName("classifies an empty datagram as unknown")
        void emptyIsUnknown() {
            assertEquals(PacketClass.UNKNOWN, InboundPacketDemux.classify(new byte[0]));
        }
    }

    @Nested
    @DisplayName("accept routing")
    class AcceptRouting {
        @Test
        @DisplayName("routes a STUN datagram to the STUN handler only, with its source address")
        void routesStun() {
            var stun = new AtomicReference<byte[]>();
            var stunSource = new AtomicReference<SocketAddress>();
            var dtls = new AtomicReference<byte[]>();
            var media = new AtomicReference<byte[]>();
            var demux = new InboundPacketDemux((bytes, source) -> {
                stun.set(bytes);
                stunSource.set(source);
            }, dtls::set, media::set, null);
            var packet = new byte[]{0x00, 0x01};
            assertEquals(PacketClass.STUN, demux.accept(packet, SOURCE));
            assertSame(packet, stun.get());
            assertSame(SOURCE, stunSource.get());
            assertNull(dtls.get());
            assertNull(media.get());
        }

        @Test
        @DisplayName("routes an RTP datagram to the media handler only")
        void routesMedia() {
            var stun = new AtomicReference<byte[]>();
            var media = new AtomicReference<byte[]>();
            var demux = new InboundPacketDemux((bytes, source) -> stun.set(bytes), null, media::set, null);
            var packet = new byte[]{(byte) 0x80, 0x60};
            assertEquals(PacketClass.RTP, demux.accept(packet, SOURCE));
            assertSame(packet, media.get());
            assertNull(stun.get());
        }

        @Test
        @DisplayName("routes an RTCP datagram to the RTCP handler by its second byte")
        void routesRtcp() {
            var media = new AtomicReference<byte[]>();
            var rtcp = new AtomicReference<byte[]>();
            var demux = new InboundPacketDemux(null, null, media::set, rtcp::set);
            // Leading byte in the RTP class with a second byte of 201 (RTCP RR) routes to the RTCP handler.
            var packet = new byte[]{(byte) 0x80, (byte) 201, 0, 1};
            assertEquals(PacketClass.RTP, demux.accept(packet, null));
            assertSame(packet, rtcp.get());
            assertNull(media.get());
        }

        @Test
        @DisplayName("routes a DTLS datagram to the DTLS handler")
        void routesDtls() {
            var dtls = new AtomicReference<byte[]>();
            var demux = new InboundPacketDemux(null, dtls::set, null, null);
            var packet = new byte[]{0x16, (byte) 0xfe, (byte) 0xfd};
            assertEquals(PacketClass.DTLS, demux.accept(packet, SOURCE));
            assertSame(packet, dtls.get());
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 100, 200})
        @DisplayName("drops an unknown datagram without invoking any handler")
        void dropsUnknown(int leadingByte) {
            var hits = new AtomicInteger();
            var demux = new InboundPacketDemux(
                    (bytes, source) -> hits.incrementAndGet(),
                    _ -> hits.incrementAndGet(),
                    _ -> hits.incrementAndGet(),
                    _ -> hits.incrementAndGet());
            assertEquals(PacketClass.UNKNOWN, demux.accept(new byte[]{(byte) leadingByte}, SOURCE));
            assertEquals(0, hits.get());
        }

        @Test
        @DisplayName("survives a throwing handler so the next datagram still classifies")
        void survivesThrowingHandler() {
            var demux = new InboundPacketDemux((bytes, source) -> {
                throw new IllegalStateException("boom");
            }, null, null, null);
            // A throwing handler must not propagate; the call returns the class normally.
            assertEquals(PacketClass.STUN, demux.accept(new byte[]{0x00}, SOURCE));
        }
    }
}
