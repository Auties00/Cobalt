package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static com.github.auties00.cobalt.wam.binary.WamTags.FIELD;
import static com.github.auties00.cobalt.wam.binary.WamTags.LAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Byte-identical known-answer test for full event-with-fields encoding
 * sequences against vectors captured from {@code WAWebWamLibProtocol},
 * reproducing what a generated Cobalt {@code *Impl.encode} emits for a
 * given (event id, sampling weight, field list) tuple: an event marker
 * followed by one field write per field, with the {@code LAST} flag on
 * the trailing entry only.
 *
 * <p>The vector matrix covers every {@code WamType} branch (integer,
 * boolean, string, float, enum-as-int, null) at every numeric boundary
 * ({@code 0}, {@code 1}, {@link Byte#MAX_VALUE}, {@link Short#MIN_VALUE},
 * {@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}) and at the WIDE_ID
 * transition. The JS encoder writes its weight argument verbatim while
 * {@link WamEventEncoder#writeEventMarker(int, int, boolean)} negates
 * internally, so {@link #assertVectorAgrees(JSONObject)} passes
 * {@code -capturedWeight} to recover identical bytes. Vectors live in
 * {@code fixtures/wam/wam-encoder-type-matrix.json}, pinned to snapshot
 * revision {@code 1039260921}.
 */
@DisplayName("WamEventEncoder KAT against live WhatsApp Web bundle")
class WamEventEncoderKatTest {
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    // Sized for the 256-byte UTF-8 boundary case plus header overhead.
    private static final int MAX_BUFFER = 4_096;

    @TestFactory
    List<DynamicTest> encoderBytesAgreeWithLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-encoder-type-matrix");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var vectors = fixture.getJSONArray("vectors");
        var tests = new ArrayList<DynamicTest>(vectors.size());
        for (var entry : vectors) {
            var vector = (JSONObject) entry;
            tests.add(dynamicTest(vector.getString("name"), () -> assertVectorAgrees(vector)));
        }
        return tests;
    }

    private static void assertVectorAgrees(JSONObject vector) {
        var eventId = vector.getIntValue("eventId");
        var capturedWeight = vector.getIntValue("weight");
        var fields = vector.getJSONArray("fields");
        var expectedHex = vector.getString("bytes");
        var expectedLength = vector.getIntValue("byteLength");

        var buffer = new byte[MAX_BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);
        var hasFields = fields != null && !fields.isEmpty();
        // The JS encoder writes weight verbatim; writeEventMarker negates internally,
        // so pass -capturedWeight to recover the captured bytes.
        encoder.writeEventMarker(eventId, -capturedWeight, hasFields);

        if (hasFields) {
            var fieldCount = fields.size();
            for (var i = 0; i < fieldCount; i++) {
                var field = fields.getJSONObject(i);
                var hasMore = i < fieldCount - 1;
                emitField(encoder, field, hasMore);
            }
        }

        var written = encoder.written();
        assertEquals(expectedLength, written, () -> "byteLength mismatch for " + vector.getString("name"));
        var actualHex = HexFormat.of().formatHex(buffer, 0, written);
        assertEquals(expectedHex, actualHex, () -> "bytes mismatch for " + vector.getString("name"));
    }

    private static void emitField(WamEventEncoder encoder, JSONObject field, boolean hasMore) {
        var id = field.getIntValue("id");
        var type = field.getString("type");
        var rawValue = field.get("value");
        switch (type) {
            case "null" -> encoder.writeNull(id, FIELD | (hasMore ? 0 : LAST));
            case "int", "bool", "enum" -> encoder.writeIntField(id, ((Number) rawValue).longValue(), hasMore);
            case "float" -> encoder.writeFloatField(id, ((Number) rawValue).doubleValue(), hasMore);
            case "str" -> encoder.writeStringField(id, extractString(rawValue), hasMore);
            default -> throw new IllegalStateException("unsupported field type: " + type);
        }
    }

    // The capture elides a long repeated-character string into a
    // {kind, length, head} summary; this expands it back.
    private static String extractString(Object raw) {
        if (raw instanceof String s) {
            return s;
        }
        if (raw instanceof JSONObject obj && "str".equals(obj.getString("kind"))) {
            var head = obj.getString("head");
            if (head == null || head.isEmpty()) {
                throw new IllegalStateException("summarised string has no head: " + obj);
            }
            return String.valueOf(head.charAt(0)).repeat(obj.getIntValue("length"));
        }
        throw new IllegalStateException("unsupported string value: " + raw);
    }

    // Catches the common fixture-format regression (vectors array renamed or
    // empty) before the per-vector tests run and obscure the cause.
    @TestFactory
    DynamicTest vectorCountIsNonZero() {
        return dynamicTest("matrix vectors are present", () -> {
            var fixture = WamFixtures.loadOracle("wam-encoder-type-matrix");
            var vectors = fixture.getJSONArray("vectors");
            if (vectors == null || vectors.isEmpty()) {
                throw new AssertionError("wam-encoder-type-matrix.json has no 'vectors' array");
            }
        });
    }
}
