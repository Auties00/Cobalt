package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static com.github.auties00.cobalt.wam.binary.WamTags.EVENT;
import static com.github.auties00.cobalt.wam.binary.WamTags.FIELD;
import static com.github.auties00.cobalt.wam.binary.WamTags.GLOBAL;
import static com.github.auties00.cobalt.wam.binary.WamTags.LAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Byte-identical known-answer test for {@link WamEventEncoder} against
 * vectors captured from {@code WAWebWamLibProtocol}'s three write helpers,
 * pinning the wire output of a single global-attribute, event-marker, or
 * field write. Agreement here is the strongest validation of the WAM tag
 * byte protocol: any divergence in role bits, the {@code LAST} flag, the
 * {@code WIDE_ID} flag, or the value-type mask surfaces as a hex mismatch.
 *
 * <p>The captured event weight is written as {@code -weight} per the JS
 * contract, so {@link #encodeEvent(WamEventEncoder, JSONArray)} passes the
 * negated value (see {@link WamEventEncoder#writeEventMarker(int, int, boolean)},
 * which negates internally). Vectors live in
 * {@code fixtures/wam/wam-tags-roundtrip.json}, pinned to snapshot revision
 * {@code 1039260921}
 */
@DisplayName("WamTags KAT against live WhatsApp Web bundle")
class WamTagsKatTest {
    private static final String FIXTURE = "wam-tags-roundtrip.json";

    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    // Holds the 65_536-byte UTF-8 boundary case plus header overhead.
    private static final int MAX_BUFFER = 70_000;

    @TestFactory
    List<DynamicTest> tagBytesAgreeWithLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-tags-roundtrip");
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
        var role = vector.getString("role");
        var args = vector.getJSONArray("args");
        var expectedHex = vector.getString("bytes");
        var expectedLength = vector.getIntValue("byteLength");

        var buffer = new byte[MAX_BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);

        switch (role) {
            case "global" -> encodeGlobal(encoder, args);
            case "event" -> encodeEvent(encoder, args);
            case "field" -> encodeField(encoder, args);
            default -> throw new IllegalStateException("unknown role: " + role);
        }

        var written = encoder.written();
        assertEquals(expectedLength, written, () -> "byteLength mismatch for " + vector.getString("name"));
        var actualHex = HexFormat.of().formatHex(buffer, 0, written);
        assertEquals(expectedHex, actualHex, () -> "bytes mismatch for " + vector.getString("name"));
    }

    private static void encodeGlobal(WamEventEncoder encoder, JSONArray args) {
        var fieldId = args.getIntValue(0);
        var raw = args.get(1);
        if (raw == null) {
            encoder.writeNull(fieldId, GLOBAL);
        } else if (raw instanceof Number n && isWhole(n)) {
            encoder.writeInt(fieldId, GLOBAL, n.longValue());
        } else if (raw instanceof Number n) {
            encoder.writeFloat(fieldId, GLOBAL, n.doubleValue());
        } else if (raw instanceof JSONObject obj) {
            encoder.writeString(fieldId, GLOBAL, reconstructString(obj));
        } else if (raw instanceof String s) {
            encoder.writeString(fieldId, GLOBAL, s);
        } else {
            throw new IllegalStateException("unsupported global value: " + raw.getClass() + " = " + raw);
        }
    }

    private static void encodeEvent(WamEventEncoder encoder, JSONArray args) {
        var eventId = args.getIntValue(0);
        var capturedWeight = args.getIntValue(1);
        var hasFields = args.getBooleanValue(2);
        // The captured weight is already the value the JS writer put into the
        // marker payload; writeEventMarker negates internally, so pass -weight.
        encoder.writeEventMarker(eventId, -capturedWeight, hasFields);
    }

    private static void encodeField(WamEventEncoder encoder, JSONArray args) {
        var fieldId = args.getIntValue(0);
        var raw = args.get(1);
        var hasFollowing = args.getBooleanValue(2);
        if (raw == null) {
            encoder.writeNull(fieldId, FIELD | (hasFollowing ? 0 : LAST));
        } else if (raw instanceof Number n && isWhole(n)) {
            encoder.writeIntField(fieldId, n.longValue(), hasFollowing);
        } else if (raw instanceof Number n) {
            encoder.writeFloatField(fieldId, n.doubleValue(), hasFollowing);
        } else if (raw instanceof JSONObject obj) {
            encoder.writeStringField(fieldId, reconstructString(obj), hasFollowing);
        } else if (raw instanceof String s) {
            encoder.writeStringField(fieldId, s, hasFollowing);
        } else {
            throw new IllegalStateException("unsupported field value: " + raw.getClass() + " = " + raw);
        }
    }

    // The fixture summarises long repeated-character strings to keep the corpus
    // small; every summarised string is a repeat of its first character, so the
    // head's first byte regenerates the original.
    private static String reconstructString(JSONObject summary) {
        if (!"str".equals(summary.getString("kind"))) {
            throw new IllegalStateException("unsupported arg shape: " + summary);
        }
        var length = summary.getIntValue("length");
        var head = summary.getString("head");
        if (head == null || head.isEmpty()) {
            throw new IllegalStateException("summarised string has no head: " + summary);
        }
        return String.valueOf(head.charAt(0)).repeat(length);
    }

    private static boolean isWhole(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            return true;
        }
        var d = n.doubleValue();
        return !Double.isNaN(d) && !Double.isInfinite(d) && d == Math.floor(d) && d == (double) (long) d;
    }

    // Guards against renumbering the role/LAST constants, which would silently
    // break every event Cobalt emits; fails faster than the per-vector KAT and
    // isolates the cause.
    @TestFactory
    List<DynamicTest> tagConstantsHaveExpectedWireBits() {
        return List.of(
                dynamicTest("GLOBAL is 0x00", () -> assertEquals(0x00, GLOBAL)),
                dynamicTest("EVENT is 0x01", () -> assertEquals(0x01, EVENT)),
                dynamicTest("FIELD is 0x02", () -> assertEquals(0x02, FIELD)),
                dynamicTest("LAST is 0x04", () -> assertEquals(0x04, LAST))
        );
    }
}
