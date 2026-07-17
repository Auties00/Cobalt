package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("DtlsPacketEnvelope sockaddr_conn discriminator")
class DtlsPacketEnvelopeTest {
    @Test
    @DisplayName("the factory stamps the fixed family and address length")
    void factoryStampsDiscriminator() {
        var envelope = DtlsPacketEnvelope.of(new byte[]{1, 2, 3});
        assertEquals(DtlsPacketEnvelope.DTLS_SOCKADDR_FAMILY, envelope.family());
        assertEquals(DtlsPacketEnvelope.DTLS_SOCKADDR_LEN, envelope.addressLength());
        assertArrayEquals(new byte[]{1, 2, 3}, envelope.payload());
    }

    @Test
    @DisplayName("the relay packet is the payload with no sockaddr prefix")
    void relayPacketIsPayloadOnly() {
        var payload = new byte[]{10, 20, 30, 40};
        assertArrayEquals(payload, DtlsPacketEnvelope.of(payload).relayPacket());
    }

    @Test
    @DisplayName("defensively copies the payload on construction and on read")
    void defensiveCopies() {
        var source = new byte[]{5, 6, 7};
        var envelope = DtlsPacketEnvelope.of(source);
        source[0] = 99;
        assertEquals(5, envelope.payload()[0], "mutating the source must not change the stored payload");
        var read = envelope.payload();
        read[0] = 88;
        assertEquals(5, envelope.payload()[0], "mutating a read copy must not change the stored payload");
        assertNotSame(envelope.payload(), envelope.payload());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10})
    @DisplayName("rejects a family other than 2")
    void rejectWrongFamily(int family) {
        assertThrows(WhatsAppCallException.DataChannel.class,
                () -> new DtlsPacketEnvelope(family, DtlsPacketEnvelope.DTLS_SOCKADDR_LEN, new byte[0]));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 8, 0x18})
    @DisplayName("rejects an address length other than 0x10")
    void rejectWrongLength(int length) {
        assertThrows(WhatsAppCallException.DataChannel.class,
                () -> new DtlsPacketEnvelope(DtlsPacketEnvelope.DTLS_SOCKADDR_FAMILY, length, new byte[0]));
    }
}
