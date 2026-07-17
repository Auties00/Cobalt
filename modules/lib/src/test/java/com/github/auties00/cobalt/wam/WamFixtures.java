package com.github.auties00.cobalt.wam;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Shared loader for the WAM-package fixtures captured from a live
 * WhatsApp Web session and used by every KAT and oracle test in this
 * package.
 *
 * <p>Three on-disk fixture shapes are supported: {@code .json} vector
 * fixtures whose header pins a captured snapshot revision (via
 * {@link #loadJson(String)} or {@link #loadVectors(String, Function)});
 * {@code .expected.json} oracle outputs captured by
 * {@code web_live_debug_eval_to_file} (raw via {@link #loadExpected(String)},
 * unwrapped via {@link #loadOracle(String)}); and {@code .jsonl} stanza
 * captures emitted by {@code web_live_stanza_dump_to_file} (via
 * {@link #loadEvents(String)}). All fixtures live under
 * {@code modules/lib/src/test/resources/fixtures/wam/}, and tests call
 * {@link #requireSnapshotRevision(JSONObject, long)} on load so revision
 * drift against the live bundle fails loudly rather than passing against
 * stale captures.
 */
public final class WamFixtures {
    private static final String FIXTURE_ROOT = "fixtures/wam";

    private WamFixtures() {
        throw new AssertionError("WamFixtures is not instantiable");
    }

    public static JSONObject loadJson(String resource) {
        Objects.requireNonNull(resource, "resource");
        var path = FIXTURE_ROOT + "/" + resource;
        try (var stream = open(path)) {
            return JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read fixture: " + path, error);
        }
    }

    public static <T> List<T> loadVectors(String resource, Function<JSONObject, T> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        var fixture = loadJson(resource);
        var vectors = fixture.getJSONArray("vectors");
        if (vectors == null) {
            throw new IllegalStateException("fixture " + resource + " has no 'vectors' array");
        }
        var out = new ArrayList<T>(vectors.size());
        for (var entry : vectors) {
            if (!(entry instanceof JSONObject obj)) {
                throw new IllegalStateException("non-object entry in 'vectors' of " + resource + ": " + entry);
            }
            out.add(mapper.apply(obj));
        }
        return out;
    }

    public static JSONObject loadExpected(String topic) {
        Objects.requireNonNull(topic, "topic");
        return loadJson(topic + ".expected.json");
    }

    // Captures wrap the eval outcome as
    // {schema, expression, result: {resultType: "string", value: "<json-string>"}};
    // oracle invocations stringify their result so the live runtime can ship it
    // through CDP without structured-clone hazards. This undoes the stringification.
    public static JSONObject loadOracle(String topic) {
        var outer = loadExpected(topic);
        var result = outer.getJSONObject("result");
        if (result == null) {
            throw new IllegalStateException("fixture " + topic + " missing 'result' wrapper");
        }
        var resultType = result.getString("resultType");
        var value = result.getString("value");
        if (!"string".equals(resultType) || value == null) {
            throw new IllegalStateException("fixture " + topic + " result is not a string payload: type=" + resultType);
        }
        return JSON.parseObject(value);
    }

    public static List<JSONObject> loadEvents(String topic) {
        Objects.requireNonNull(topic, "topic");
        var resource = FIXTURE_ROOT + "/" + topic + ".jsonl";
        try (var stream = open(resource);
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            var events = new ArrayList<JSONObject>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                var record = JSON.parseObject(line);
                var event = record.getJSONObject("event");
                if (event == null) {
                    throw new IllegalStateException("fixture line missing 'event': " + resource);
                }
                events.add(event);
            }
            return events;
        } catch (IOException error) {
            throw new UncheckedIOException("failed to read fixture: " + resource, error);
        }
    }

    public static boolean isAvailable(String resource) {
        Objects.requireNonNull(resource, "resource");
        return WamFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + resource) != null;
    }

    public static void requireSnapshotRevision(JSONObject fixtureHeader, long expected) {
        Objects.requireNonNull(fixtureHeader, "fixtureHeader");
        var actual = fixtureHeader.getLong("snapshotRevision");
        if (actual == null) {
            throw new AssertionError("fixture has no 'snapshotRevision' field; expected " + expected);
        }
        if (actual != expected) {
            throw new AssertionError("fixture snapshotRevision drift: expected " + expected + " but was " + actual);
        }
    }

    public static JSONArray rawVectors(String resource) {
        var fixture = loadJson(resource);
        var vectors = fixture.getJSONArray("vectors");
        if (vectors == null) {
            throw new IllegalStateException("fixture " + resource + " has no 'vectors' array");
        }
        return vectors;
    }

    private static InputStream open(String resourcePath) throws IOException {
        var stream = WamFixtures.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("fixture not found on classpath: " + resourcePath);
        }
        return stream;
    }
}
