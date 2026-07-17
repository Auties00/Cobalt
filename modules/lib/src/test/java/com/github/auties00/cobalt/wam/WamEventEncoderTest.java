package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamEventSizes;
import com.github.auties00.cobalt.wam.binary.WamTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Behavioural tests for {@link WamEventEncoder} and
 * {@link WamEventDecoder} over the full primitive type matrix,
 * exercising three orthogonal properties: round-trip parity (encode then
 * decode bit-equals the input), sink parity (byte-array sink and
 * {@link ByteArrayOutputStream} sink agree byte-for-byte), and
 * {@link WamEventSizes} parity (the predictive size functions equal the
 * bytes actually written, and pre-allocating exactly that many bytes
 * succeeds without overflow).
 *
 * <p>This file asserts internal consistency only; the byte-level KAT
 * against the live WhatsApp Web bundle is {@link WamEventEncoderKatTest}.
 */
@DisplayName("WamEventEncoder / WamEventDecoder behavioural")
class WamEventEncoderTest {
    // Sized for the 70_000-byte UTF-8 sink-parity case (the matrix worst case).
    private static final int BUFFER = 80_000;

    @Nested
    @DisplayName("round-trip encode → decode")
    class RoundTrip {
        @TestFactory
        List<DynamicTest> intBoundaries() {
            var values = List.of(
                    0L, 1L,
                    (long) Byte.MIN_VALUE, (long) Byte.MAX_VALUE,
                    (long) Short.MIN_VALUE, (long) Short.MAX_VALUE,
                    (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE,
                    -1L, 42L, -42L);
            var tests = new ArrayList<DynamicTest>();
            for (var v : values) {
                tests.add(dynamicTest("int " + v, () -> assertIntRoundTrip(7, v, false)));
                tests.add(dynamicTest("int " + v + " (LAST)", () -> assertIntRoundTrip(7, v, true)));
                tests.add(dynamicTest("int " + v + " (WIDE_ID)", () -> assertIntRoundTrip(300, v, false)));
            }
            return tests;
        }

        @TestFactory
        List<DynamicTest> stringBoundaries() {
            var values = List.of(
                    "",
                    "a",
                    "hello",
                    "héllo " + new String(Character.toChars(128512)),
                    "a".repeat(255),
                    "a".repeat(256),
                    "a".repeat(65535),
                    "a".repeat(65536));
            var tests = new ArrayList<DynamicTest>();
            for (var s : values) {
                var label = s.length() <= 32 ? "\"" + s + "\"" : "<str:" + s.length() + ">";
                tests.add(dynamicTest("str " + label,
                        () -> assertStringRoundTrip(9, s, false)));
            }
            return tests;
        }

        @TestFactory
        List<DynamicTest> floatValues() {
            var values = List.of(
                    0.0, -0.0, 1.0, -1.0, 3.14159265358979, -2.5,
                    1e15, 1.5e-10, 0.5,
                    Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.MIN_VALUE, Double.MAX_VALUE);
            var tests = new ArrayList<DynamicTest>();
            for (var d : values) {
                tests.add(dynamicTest("float " + d, () -> assertFloatRoundTrip(11, d)));
            }
            return tests;
        }

        @Test
        @DisplayName("bool true/false round-trip")
        void boolRoundTrip() {
            for (var v : new boolean[]{true, false}) {
                var buffer = new byte[16];
                var encoder = WamEventEncoder.toBytes(buffer);
                encoder.writeBoolField(5, v, false);
                var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
                var header = decoder.readHeader();
                assertEquals(5, WamEventDecoder.fieldIdOf(header));
                var decoded = decoder.readInt(header) != 0L;
                assertEquals(v, decoded);
            }
        }

        // Only the GLOBAL role emits bytes for a null value (the dirty-write
        // null-transition path); the FIELD and EVENT no-op branches are covered
        // by WamEventDecoderTest#writeNullNonGlobalIsNoop().
        @Test
        @DisplayName("null GLOBAL entry: writeNull → tag with VALUE_NULL bits, no payload")
        void nullRoundTrip() {
            var buffer = new byte[16];
            var encoder = WamEventEncoder.toBytes(buffer);
            encoder.writeNull(5, WamTags.GLOBAL);
            assertEquals(2, encoder.written(),
                    "null GLOBAL entry encodes to exactly 2 bytes: tag + fieldId");
            var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
            var header = decoder.readHeader();
            assertEquals(5, WamEventDecoder.fieldIdOf(header));
            assertEquals(0x00, WamEventDecoder.valueTypeOf(header) & 0xF0,
                    "VALUE_NULL is the 0x00 nibble");
        }

        @Test
        @DisplayName("event marker: writeEventMarker → decoded eventId + negated weight")
        void eventMarkerRoundTrip() {
            var buffer = new byte[16];
            var encoder = WamEventEncoder.toBytes(buffer);
            encoder.writeEventMarker(2862, 1, false);
            var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
            var header = decoder.readHeader();
            assertEquals(2862, WamEventDecoder.fieldIdOf(header));
            assertEquals(-1L, decoder.readInt(header),
                    "event marker payload is the negated weight per JS convention");
        }
    }

    @Nested
    @DisplayName("ByteArray vs Stream sink parity")
    class SinkParity {
        @Test
        @DisplayName("identical inputs yield identical bytes on both sinks")
        void parityAcrossTypes() {
            assertSinkParity(encoder -> encoder.writeNull(5, WamTags.GLOBAL));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, 0L));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, 1L));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, 127L));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, 200L));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, 70_000L));
            assertSinkParity(encoder -> encoder.writeInt(5, WamTags.GLOBAL, Integer.MAX_VALUE));
            assertSinkParity(encoder -> encoder.writeFloat(5, WamTags.GLOBAL, 3.14));
            assertSinkParity(encoder -> encoder.writeString(5, WamTags.GLOBAL, "hi"));
            assertSinkParity(encoder -> encoder.writeString(5, WamTags.GLOBAL, "a".repeat(300)));
            assertSinkParity(encoder -> encoder.writeString(5, WamTags.GLOBAL, "a".repeat(70_000)));
            assertSinkParity(encoder -> encoder.writeEventMarker(2862, 1, true));
            assertSinkParity(encoder -> encoder.writeInt(300, WamTags.FIELD, 7L));
            assertSinkParity(encoder -> encoder.writeString(300, WamTags.FIELD, "wide"));
        }
    }

    @Nested
    @DisplayName("WamEventSizes equals actual bytes written")
    class SizesParity {
        @Test
        @DisplayName("intFieldSize matches written bytes")
        void intFieldSizeParity() {
            for (var v : new long[]{0L, 1L, 127L, 200L, 70_000L, Integer.MAX_VALUE, Integer.MIN_VALUE, -1L}) {
                assertSizeParity(WamEventSizes.intFieldSize(7, v),
                        encoder -> encoder.writeIntField(7, v, false));
            }
        }

        @Test
        @DisplayName("boolFieldSize matches written bytes")
        void boolFieldSizeParity() {
            assertSizeParity(WamEventSizes.boolFieldSize(7),
                    encoder -> encoder.writeBoolField(7, true, false));
            assertSizeParity(WamEventSizes.boolFieldSize(7),
                    encoder -> encoder.writeBoolField(7, false, false));
        }

        @Test
        @DisplayName("stringFieldSize matches written bytes (across all length-prefix tiers)")
        void stringFieldSizeParity() {
            for (var s : new String[]{"", "hi", "a".repeat(255), "a".repeat(256), "a".repeat(65535), "a".repeat(65536)}) {
                assertSizeParity(WamEventSizes.stringFieldSize(7, s),
                        encoder -> encoder.writeStringField(7, s, false));
            }
        }

        @Test
        @DisplayName("floatFieldSize matches written bytes")
        void floatFieldSizeParity() {
            assertSizeParity(WamEventSizes.floatFieldSize(7),
                    encoder -> encoder.writeFloatField(7, 3.14, false));
        }

        @Test
        @DisplayName("eventMarkerSize matches written bytes")
        void eventMarkerSizeParity() {
            assertSizeParity(WamEventSizes.eventMarkerSize(2862, 1),
                    encoder -> encoder.writeEventMarker(2862, 1, false));
        }

        // nullSize returns the byte cost of a GLOBAL null entry; pairing it with
        // a FIELD or EVENT writeNull would always disagree because those roles
        // emit zero bytes per the null-shortcut.
        @Test
        @DisplayName("nullSize matches GLOBAL null-entry written bytes")
        void nullSizeParity() {
            assertSizeParity(WamEventSizes.nullSize(7),
                    encoder -> encoder.writeNull(7, WamTags.GLOBAL));
            assertSizeParity(WamEventSizes.nullSize(300),
                    encoder -> encoder.writeNull(300, WamTags.GLOBAL));
        }
    }

    @Nested
    @DisplayName("buffer-overflow boundary")
    class OverflowBoundary {
        @Test
        @DisplayName("pre-allocating exactly sizeOf bytes succeeds")
        void exactSizeSucceeds() {
            var size = WamEventSizes.intFieldSize(7, 70_000L);
            var buffer = new byte[size];
            var encoder = WamEventEncoder.toBytes(buffer);
            encoder.writeIntField(7, 70_000L, false);
            assertEquals(size, encoder.written());
        }

        @Test
        @DisplayName("encoding beyond the buffer throws IndexOutOfBoundsException")
        void overflowThrows() {
            var buffer = new byte[3];
            var encoder = WamEventEncoder.toBytes(buffer);
            assertThrows(IndexOutOfBoundsException.class,
                    () -> encoder.writeIntField(7, 70_000L, false));
        }
    }

    private static void assertIntRoundTrip(int fieldId, long value, boolean hasFollowing) {
        var buffer = new byte[BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeIntField(fieldId, value, hasFollowing);
        var written = encoder.written();
        var decoder = WamEventDecoder.fromBytes(buffer, 0, written);
        var header = decoder.readHeader();
        assertEquals(fieldId, WamEventDecoder.fieldIdOf(header));
        assertEquals(value, decoder.readInt(header));
    }

    private static void assertStringRoundTrip(int fieldId, String value, boolean hasFollowing) {
        var size = WamEventSizes.stringFieldSize(fieldId, value);
        var buffer = new byte[size];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeStringField(fieldId, value, hasFollowing);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertEquals(fieldId, WamEventDecoder.fieldIdOf(header));
        assertEquals(value, decoder.readString(header));
    }

    private static void assertFloatRoundTrip(int fieldId, double value) {
        var size = WamEventSizes.floatFieldSize(fieldId);
        var buffer = new byte[size];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeFloatField(fieldId, value, false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertEquals(fieldId, WamEventDecoder.fieldIdOf(header));
        var actual = decoder.readFloat(header);
        assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(actual),
                "float round-trip must preserve all 64 bits (including NaN payload)");
    }

    private static void assertSinkParity(Consumer<WamEventEncoder> op) {
        var arrayBuffer = new byte[BUFFER];
        var arrayEncoder = WamEventEncoder.toBytes(arrayBuffer);
        op.accept(arrayEncoder);

        var stream = new ByteArrayOutputStream();
        var streamEncoder = WamEventEncoder.toStream(stream);
        op.accept(streamEncoder);

        assertEquals(arrayEncoder.written(), streamEncoder.written(),
                "byte-array and stream sinks must report the same written count");
        var arrayBytes = new byte[arrayEncoder.written()];
        System.arraycopy(arrayBuffer, 0, arrayBytes, 0, arrayBytes.length);
        assertArrayEquals(arrayBytes, stream.toByteArray(),
                "byte-array and stream sinks must produce identical bytes");
    }

    private static void assertSizeParity(int expectedSize, Consumer<WamEventEncoder> op) {
        var buffer = new byte[expectedSize];
        var encoder = WamEventEncoder.toBytes(buffer);
        op.accept(encoder);
        assertEquals(expectedSize, encoder.written(),
                "WamEventSizes.xxxSize must equal actual bytes written");
    }
}
