package com.github.auties00.cobalt.calls.media.audio.codec.mlow;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import com.github.auties00.cobalt.calls.media.audio.neteq.decoder.MLowAudioDecoder;

/**
 * Dependency-free loader for the MLow decoder parity corpus, the fixture future
 * {@code MLowAudioDecoder} parity tests replay against.
 *
 * <p>Each capture JSON (under {@code src/test/resources/fixtures/call/mlow/decode/}) carries a flat
 * {@code sends} array of decoder-hook entries tagged {@code enc@<slot>} (the encoded MLow frame
 * fed to {@code AudioDecoderMLowImpl::DecodeInternal}) and {@code pcm@<slot>} (the little-endian
 * {@code int16} PCM that call produced). This loader filters to those tags, pairs each encoded
 * entry with the nearest-following PCM entry within {@value #PAIRING_WINDOW_MS} ms, and yields
 * {@link Pair} records. Unpaired encoded entries (no PCM match) and unpaired PCM entries
 * (concealment frames produced with no encoded input) are counted, not paired.
 *
 * <p>The JSON is scanned with a minimal brace/string walker rather than a JSON dependency: the
 * format is fixed and produced by the capture tooling, and the test module pulls in no JSON
 * library. Captures are paired per file because timestamps do not align across files.
 */
public final class MlowDecodeCorpus {
    /**
     * Classpath directory holding the capture resources.
     */
    private static final String RESOURCE_DIR = "fixtures/call/mlow/decode/";

    /**
     * Canonical capture set; extend this when new parity captures are lifted in so
     * {@link #loadAll()} sees them without per-test edits.
     */
    private static final String[] CANONICAL_CAPTURES = {
            "parity-3.json",
            "parity-4.json"
    };

    /**
     * Maximum milliseconds between an encoded entry and its post-call PCM. The live pre-hook fires
     * synchronously before the decode runs, so the post-hook lands within microseconds in practice;
     * five milliseconds absorbs JS event-loop jitter.
     */
    private static final long PAIRING_WINDOW_MS = 5L;

    private static final String ENC_SOURCE_PREFIX = "enc@";

    private static final String PCM_SOURCE_PREFIX = "pcm@";

    private final List<Pair> pairs;

    private final int unpairedEncodedCount;

    private final int unpairedPcmCount;

    private MlowDecodeCorpus(List<Pair> pairs, int unpairedEncodedCount, int unpairedPcmCount) {
        this.pairs = List.copyOf(pairs);
        this.unpairedEncodedCount = unpairedEncodedCount;
        this.unpairedPcmCount = unpairedPcmCount;
    }

    /**
     * One captured {@code (encoded, expectedPcm)} decode pair. {@code encoded} is the MLow frame fed
     * to the decoder; {@code expectedPcm} is the {@code int16} PCM the live decoder produced from it;
     * {@code timestampMillis} is the capture-time epoch for ordering only.
     *
     * @param encoded         the encoded MLow frame bytes; never empty
     * @param expectedPcm     the decoded {@code int16} PCM samples; never empty
     * @param timestampMillis capture-time epoch milliseconds
     */
    public record Pair(byte[] encoded, short[] expectedPcm, long timestampMillis) {
        public Pair {
            if (encoded == null || encoded.length == 0) {
                throw new IllegalArgumentException("encoded is empty");
            }
            if (expectedPcm == null || expectedPcm.length == 0) {
                throw new IllegalArgumentException("expectedPcm is empty");
            }
        }
    }

    /**
     * Returns the paired decode cases in capture-time order.
     *
     * @return the immutable list of pairs
     */
    public List<Pair> pairs() {
        return pairs;
    }

    /**
     * Returns the count of encoded entries that found no PCM match within the pairing window.
     *
     * @return the unpaired encoded count
     */
    public int unpairedEncodedCount() {
        return unpairedEncodedCount;
    }

    /**
     * Returns the count of PCM entries with no matching encoded input (concealment frames).
     *
     * @return the unpaired PCM count
     */
    public int unpairedPcmCount() {
        return unpairedPcmCount;
    }

    /**
     * Loads and merges every {@link #CANONICAL_CAPTURES canonical capture}. Pairing happens within
     * each file; the merged result concatenates each file's pairs in input order.
     *
     * @return the aggregated corpus
     */
    public static MlowDecodeCorpus loadAll() {
        var allPairs = new ArrayList<Pair>();
        var unpairedEncoded = 0;
        var unpairedPcm = 0;
        for (var name : CANONICAL_CAPTURES) {
            var sub = loadResource(name);
            allPairs.addAll(sub.pairs());
            unpairedEncoded += sub.unpairedEncodedCount();
            unpairedPcm += sub.unpairedPcmCount();
        }
        return new MlowDecodeCorpus(allPairs, unpairedEncoded, unpairedPcm);
    }

    /**
     * Loads one capture by its file name under {@link #RESOURCE_DIR}.
     *
     * @param fileName the capture file name, e.g. {@code parity-3.json}
     * @return the loaded corpus for that single capture
     */
    public static MlowDecodeCorpus loadResource(String fileName) {
        var resource = RESOURCE_DIR + fileName;
        var loader = MlowDecodeCorpus.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new UncheckedIOException(new IOException("resource not found: " + resource));
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Loads one capture from a filesystem path.
     *
     * @param jsonPath the path to a capture JSON
     * @return the loaded corpus
     */
    public static MlowDecodeCorpus load(Path jsonPath) {
        try {
            return parse(Files.readString(jsonPath, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Parses raw capture JSON into a corpus. Walks the {@code sends} array with a brace/string-aware
     * scanner, collects {@code enc@}/{@code pcm@} entries, and pairs them by timestamp.
     *
     * @param json the raw capture JSON text
     * @return the parsed corpus
     */
    public static MlowDecodeCorpus parse(String json) {
        var encs = new ArrayList<RawEntry>();
        var pcms = new ArrayList<RawEntry>();
        var sendsStart = json.indexOf("\"sends\":[");
        if (sendsStart < 0) {
            throw new IllegalArgumentException("missing sends array");
        }
        var pos = sendsStart + "\"sends\":[".length();
        while (pos < json.length()) {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
            if (pos >= json.length() || json.charAt(pos) == ']') {
                break;
            }
            if (json.charAt(pos) == ',') {
                pos++;
                continue;
            }
            if (json.charAt(pos) != '{') {
                throw new IllegalArgumentException("expected '{' at pos " + pos);
            }
            var endObj = findObjectEnd(json, pos);
            var entry = parseEntry(json, pos, endObj);
            if (entry != null) {
                if (entry.source.startsWith(ENC_SOURCE_PREFIX)) {
                    encs.add(entry);
                } else if (entry.source.startsWith(PCM_SOURCE_PREFIX)) {
                    pcms.add(entry);
                }
            }
            pos = endObj + 1;
        }
        encs.sort(Comparator.comparingLong(entry -> entry.timestamp));
        pcms.sort(Comparator.comparingLong(entry -> entry.timestamp));
        return pair(encs, pcms);
    }

    private static MlowDecodeCorpus pair(List<RawEntry> encs, List<RawEntry> pcms) {
        var consumed = new boolean[pcms.size()];
        var pairs = new ArrayList<Pair>();
        var unpairedEncoded = 0;
        var pcmCursor = 0;
        for (var enc : encs) {
            while (pcmCursor < pcms.size() && pcms.get(pcmCursor).timestamp < enc.timestamp) {
                pcmCursor++;
            }
            var match = -1;
            var bestDelta = Long.MAX_VALUE;
            for (var i = pcmCursor; i < pcms.size(); i++) {
                if (consumed[i]) {
                    continue;
                }
                var delta = pcms.get(i).timestamp - enc.timestamp;
                if (delta < 0) {
                    continue;
                }
                if (delta > PAIRING_WINDOW_MS) {
                    break;
                }
                if (delta < bestDelta) {
                    bestDelta = delta;
                    match = i;
                }
            }
            if (match < 0) {
                unpairedEncoded++;
                continue;
            }
            consumed[match] = true;
            pairs.add(new Pair(enc.bytes, decodePcm(pcms.get(match).bytes), enc.timestamp));
        }
        var unpairedPcm = 0;
        for (var c : consumed) {
            if (!c) {
                unpairedPcm++;
            }
        }
        return new MlowDecodeCorpus(pairs, unpairedEncoded, unpairedPcm);
    }

    private static short[] decodePcm(byte[] bytes) {
        var truncated = bytes.length & ~1;
        var samples = new short[truncated / 2];
        var buffer = ByteBuffer.wrap(bytes, 0, truncated).order(ByteOrder.LITTLE_ENDIAN);
        for (var i = 0; i < samples.length; i++) {
            samples[i] = buffer.getShort();
        }
        return samples;
    }

    private static RawEntry parseEntry(String json, int start, int end) {
        var source = readStringField(json, start, end, "source");
        var b64 = readStringField(json, start, end, "bytesB64");
        var tsStr = readNumberField(json, start, end, "ts");
        if (source == null || b64 == null || tsStr == null) {
            return null;
        }
        return new RawEntry(source, Long.parseLong(tsStr), Base64.getDecoder().decode(b64));
    }

    private static int findObjectEnd(String json, int start) {
        var depth = 0;
        var inString = false;
        var escape = false;
        for (var i = start; i < json.length(); i++) {
            var c = json.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException("unterminated object at " + start);
    }

    private static String readStringField(String json, int start, int end, String key) {
        var marker = "\"" + key + "\":\"";
        var idx = json.indexOf(marker, start);
        if (idx < 0 || idx >= end) {
            return null;
        }
        var valueStart = idx + marker.length();
        var valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0 || valueEnd > end) {
            return null;
        }
        return json.substring(valueStart, valueEnd);
    }

    private static String readNumberField(String json, int start, int end, String key) {
        var marker = "\"" + key + "\":";
        var idx = json.indexOf(marker, start);
        if (idx < 0 || idx >= end) {
            return null;
        }
        var valueStart = idx + marker.length();
        var valueEnd = valueStart;
        while (valueEnd < end) {
            var c = json.charAt(valueEnd);
            if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                break;
            }
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd);
    }

    private record RawEntry(String source, long timestamp, byte[] bytes) {
    }
}
