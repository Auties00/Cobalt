package com.github.auties00.cobalt.wam.privatestats.ed25519;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.privatestats.WamPrivateStatsTokenBlinder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins {@link Ed25519HashToPoint}, {@link WamPrivateStatsTokenBlinder#blind}, and
 * {@link WamPrivateStatsTokenBlinder#unblind} against known-answer vectors captured from the live
 * WhatsApp Web JavaScript bundle. Byte-identical agreement is the strongest validation possible: if
 * any constant or formula in the Java port diverges from the JS reference, at least one output
 * mismatches.
 *
 * <p>The vectors live in {@code fixtures/wam/ed25519-live-bundle-vectors.json}, captured against
 * snapshot revision 1038176432 (live-runtime revision 1038189736) on 2026-04-27 via the
 * {@code mcp__whatsapp__web_live_debug_eval} tool. Re-capture if the WhatsApp Web implementation
 * changes. Vector inputs are seeded deterministically by the capture harness; outputs are the bytes
 * returned by the live JS bundle, with {@code sk}/{@code pk}/{@code signed} simulating a server that
 * applies its secret scalar to the blinded point.
 */
class Ed25519LiveBundleKatTest {
    private static final String FIXTURE = "fixtures/wam/ed25519-live-bundle-vectors.json";

    private static final int VECTOR_BYTE_LENGTH = 32;

    private static final List<Vector> VECTORS = loadVectors();

    private record Vector(int index, byte[] msg, byte[] scalar, byte[] sk,
                          byte[] hashToPoint, byte[] blinded, byte[] pk,
                          byte[] signed, byte[] unblinded) {
    }

    @Test
    void hashToPointMatchesLiveBundle() {
        for (var v : VECTORS) {
            var p = Ed25519HashToPoint.compute(v.msg());
            var actual = new byte[32];
            Ed25519Point.pack(actual, p);
            assertArrayEquals(v.hashToPoint(), actual,
                    "hashToPoint mismatch on vector " + v.index());
        }
    }

    @Test
    void blindMatchesLiveBundle() {
        for (var v : VECTORS) {
            var actual = WamPrivateStatsTokenBlinder.blind(v.msg(), v.scalar());
            assertArrayEquals(v.blinded(), actual,
                    "blind mismatch on vector " + v.index());
        }
    }

    @Test
    void unblindMatchesLiveBundle() {
        for (var v : VECTORS) {
            var actual = WamPrivateStatsTokenBlinder.unblind(v.signed(), v.scalar(), v.pk());
            assertArrayEquals(v.unblinded(), actual,
                    "unblind mismatch on vector " + v.index());
        }
    }

    @Test
    void vectorByteLengthsAreCorrect() {
        for (var v : VECTORS) {
            assertEquals(VECTOR_BYTE_LENGTH, v.msg().length, "msg length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.scalar().length, "scalar length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.sk().length, "sk length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.hashToPoint().length, "hashToPoint length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.blinded().length, "blinded length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.pk().length, "pk length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.signed().length, "signed length on vector " + v.index());
            assertEquals(VECTOR_BYTE_LENGTH, v.unblinded().length, "unblinded length on vector " + v.index());
        }
    }

    private static List<Vector> loadVectors() {
        try (var in = Ed25519LiveBundleKatTest.class.getResourceAsStream("/" + FIXTURE)) {
            if (in == null) {
                throw new IOException("fixture not found on classpath: " + FIXTURE);
            }
            var json = JSON.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            var arr = json.getJSONArray("vectors");
            var out = new ArrayList<Vector>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                out.add(toVector(arr.getJSONObject(i)));
            }
            return List.copyOf(out);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + FIXTURE, e);
        }
    }

    private static Vector toVector(JSONObject o) {
        return new Vector(
                o.getIntValue("index"),
                fromHex(o.getString("msg")),
                fromHex(o.getString("scalar")),
                fromHex(o.getString("sk")),
                fromHex(o.getString("hashToPoint")),
                fromHex(o.getString("blinded")),
                fromHex(o.getString("pk")),
                fromHex(o.getString("signed")),
                fromHex(o.getString("unblinded")));
    }

    private static byte[] fromHex(String hex) {
        var out = new byte[hex.length() / 2];
        for (var i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
