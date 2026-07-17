package com.github.auties00.cobalt.wam;

import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corrupt-input and forward-compatibility tests for
 * {@link WamEventDecoder}, pinning the error semantics so a regression
 * in either direction (over-strict rejection of forward-compatible
 * input, or silent acceptance of truncated or type-mismatched input)
 * surfaces immediately. The matrix covers truncated event markers,
 * truncated payloads, type-tag mismatches across the read primitives,
 * and the forward-compatible unknown-fieldId path the generated
 * {@code *Impl.decode default -> skip(header)} branch relies on.
 */
@DisplayName("WamEventDecoder corrupt-input handling")
class WamEventDecoderTest {
    @Test
    @DisplayName("truncated event marker: no fieldId byte → IndexOutOfBoundsException")
    void truncatedEventMarker() {
        var truncated = new byte[]{(byte) (WamTags.EVENT | WamTags.LAST | WamTags.VALUE_INT_0)};
        var decoder = WamEventDecoder.fromBytes(truncated, 0, truncated.length);
        assertThrows(IndexOutOfBoundsException.class, decoder::readHeader);
    }

    @Test
    @DisplayName("truncated WIDE_ID fieldId: only 1 of 2 bytes → IndexOutOfBoundsException")
    void truncatedWideId() {
        var truncated = new byte[]{
                (byte) (WamTags.FIELD | WamTags.WIDE_ID | WamTags.VALUE_INT_0),
                0x01
        };
        var decoder = WamEventDecoder.fromBytes(truncated, 0, truncated.length);
        assertThrows(IndexOutOfBoundsException.class, decoder::readHeader);
    }

    @Test
    @DisplayName("truncated int32 payload: 2 of 4 bytes → IndexOutOfBoundsException")
    void truncatedInt32Payload() {
        var truncated = new byte[]{
                (byte) (WamTags.FIELD | WamTags.LAST | WamTags.VALUE_INT32),
                0x05,
                0x01, 0x02
        };
        var decoder = WamEventDecoder.fromBytes(truncated, 0, truncated.length);
        var header = decoder.readHeader();
        assertEquals(5, WamEventDecoder.fieldIdOf(header));
        assertThrows(IndexOutOfBoundsException.class, () -> decoder.readInt(header));
    }

    @Test
    @DisplayName("truncated STR8 payload: length byte declares more than remains → IndexOutOfBoundsException")
    void truncatedStringPayload() {
        var truncated = new byte[]{
                (byte) (WamTags.FIELD | WamTags.LAST | WamTags.VALUE_STR8),
                0x05,
                0x10,
                'a', 'b'
        };
        var decoder = WamEventDecoder.fromBytes(truncated, 0, truncated.length);
        var header = decoder.readHeader();
        assertThrows(IndexOutOfBoundsException.class, () -> decoder.readString(header));
    }

    @Test
    @DisplayName("type-tag mismatch: readString on int header → IllegalStateException")
    void readStringOnIntHeader() {
        var buffer = new byte[16];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeIntField(5, 42L, false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertThrows(IllegalStateException.class, () -> decoder.readString(header));
    }

    @Test
    @DisplayName("type-tag mismatch: readInt on string header → IllegalStateException")
    void readIntOnStringHeader() {
        var buffer = new byte[32];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeStringField(5, "hi", false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertThrows(IllegalStateException.class, () -> decoder.readInt(header));
    }

    @Test
    @DisplayName("type-tag mismatch: readFloat on int header → IllegalStateException")
    void readFloatOnIntHeader() {
        var buffer = new byte[16];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeIntField(5, 200L, false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertThrows(IllegalStateException.class, () -> decoder.readFloat(header));
    }

    // One entry per value-type encoding (null GLOBAL, int 0/1/7/200/70_000,
    // float, str8, str16) so a regression on any of them fails here rather
    // than as a downstream decoder hang.
    @Test
    @DisplayName("skip advances past every known value-type cleanly")
    void skipCoversAllTypes() {
        var buffer = new byte[512];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeNull(1, WamTags.GLOBAL);
        encoder.writeIntField(2, 0L, true);
        encoder.writeIntField(3, 1L, true);
        encoder.writeIntField(4, 7L, true);
        encoder.writeIntField(5, 200L, true);
        encoder.writeIntField(6, 70_000L, true);
        encoder.writeFloatField(7, 3.14, true);
        encoder.writeStringField(8, "hi", true);
        encoder.writeStringField(9, "a".repeat(300), false);
        var size = encoder.written();

        var decoder = WamEventDecoder.fromBytes(buffer, 0, size);
        var skipped = 0;
        while (decoder.hasMore()) {
            var header = decoder.readHeader();
            decoder.skip(header);
            skipped++;
        }
        assertEquals(9, skipped, "skip must walk past all 9 entries");
    }

    @Test
    @DisplayName("writeNull is a no-op for FIELD / EVENT roles (matches WA Web's writeField/writeEvent null shortcut)")
    void writeNullNonGlobalIsNoop() {
        var buffer = new byte[16];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeNull(5, WamTags.FIELD);
        assertEquals(0, encoder.written(),
                "writeNull(FIELD) must emit zero bytes, matching WAWebWamLibProtocol's behaviour");

        encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeNull(5, WamTags.FIELD | WamTags.LAST);
        assertEquals(0, encoder.written(),
                "writeNull(FIELD|LAST) must emit zero bytes");

        encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeNull(5, WamTags.EVENT);
        assertEquals(0, encoder.written(),
                "writeNull(EVENT) must emit zero bytes - WAWebWamLibProtocol.writeEvent skips null");
    }

    @Test
    @DisplayName("unknown fieldId is propagated to caller, not rejected")
    void unknownFieldIdIsPropagated() {
        var buffer = new byte[16];
        var encoder = WamEventEncoder.toBytes(buffer);
        encoder.writeIntField(99, 5L, false);
        var decoder = WamEventDecoder.fromBytes(buffer, 0, encoder.written());
        var header = decoder.readHeader();
        assertEquals(99, WamEventDecoder.fieldIdOf(header),
                "decoder passes the fieldId through; caller decides whether to skip");
        decoder.skip(header);
        assertTrue(!decoder.hasMore(),
                "after skip, the decoder should be at the buffer end");
    }
}
