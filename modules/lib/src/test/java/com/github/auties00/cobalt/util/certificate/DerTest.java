package com.github.auties00.cobalt.util.certificate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Known-answer tests for the {@link Der} builder, the parts of X.509 assembly not delegated to a JDK type. Each
 * vector is checked against the byte sequence ASN.1 DER mandates, exercised through {@code encode()} so the
 * single-pass serialization is what is asserted.
 */
@DisplayName("Der")
class DerTest {
    private static final HexFormat HEX = HexFormat.of();

    @Test
    @DisplayName("encodes an OID by folding the first two arcs and base-128 encoding the rest")
    void oid() {
        assertEquals("06082a8648ce3d040302", HEX.formatHex(Der.oid("1.2.840.10045.4.3.2").encode()));
    }

    @Test
    @DisplayName("encodes a small INTEGER without a leading zero")
    void smallInteger() {
        assertEquals("02017f", HEX.formatHex(Der.integer(BigInteger.valueOf(127)).encode()));
    }

    @Test
    @DisplayName("prepends a zero byte to keep a high-bit INTEGER positive")
    void highBitInteger() {
        assertEquals("02020080", HEX.formatHex(Der.integer(BigInteger.valueOf(128)).encode()));
    }

    @Test
    @DisplayName("encodes NULL as 05 00")
    void nullValue() {
        assertEquals("0500", HEX.formatHex(Der.nullValue().encode()));
    }

    @Test
    @DisplayName("uses long-form length once content reaches 128 bytes")
    void longFormLength() {
        var encoded = Der.bitString(new byte[200]).encode();
        assertEquals(0x03, encoded[0] & 0xFF);
        assertEquals(0x81, encoded[1] & 0xFF);
        assertEquals(0xC9, encoded[2] & 0xFF);
        assertEquals(3 + 201, encoded.length);
    }

    @Test
    @DisplayName("encodes a pre-2050 time as UTCTime and a 2050-or-later time as GeneralizedTime")
    void timeRepresentation() {
        assertEquals(0x17, Der.time(Instant.parse("2049-06-15T12:00:00Z")).encode()[0] & 0xFF);
        assertEquals(0x18, Der.time(Instant.parse("2050-01-01T00:00:00Z")).encode()[0] & 0xFF);
    }

    @Test
    @DisplayName("serializes a nested SEQUENCE in one pass")
    void sequence() {
        var encoded = Der.sequence()
                .add(Der.integer(BigInteger.ONE))
                .add(Der.nullValue())
                .encode();
        assertArrayEquals(HEX.parseHex("30050201010500"), encoded);
    }

    @Test
    @DisplayName("splices a raw pre-encoded value verbatim")
    void raw() {
        var inner = Der.oid("1.2.840.10045.4.3.2").encode();
        var encoded = Der.sequence().add(Der.raw(inner)).encode();
        assertEquals("300a" + HEX.formatHex(inner), HEX.formatHex(encoded));
    }

    @Test
    @DisplayName("wraps a BIT STRING with the zero unused-bits octet")
    void bitString() {
        assertEquals("03020041", HEX.formatHex(Der.bitString(new byte[]{0x41}).encode()));
    }
}
