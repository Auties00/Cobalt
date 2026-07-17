package com.github.auties00.cobalt.sync;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyData;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyDataBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyIdBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Test helper that loads sync-package fixtures captured from a live WhatsApp Web session and exposes
 * them to the JUnit suites in this package. All artefacts are rooted at {@link #FIXTURE_ROOT}:
 * JSONL stanza captures (reconstructed into Cobalt {@link Stanza} instances), {@code .expected.json}
 * oracle outputs, {@code .synckey.bin} binary sync-key material, and per-syncType history-sync
 * triplets under {@code history/<slug>/}. The captured trees carry WA Web's internal JID wrappers
 * ({@code {"$1": {...}}}) and a binary leaf shape ({@code {"kind": "binary", "base64": "..."}}), so
 * the tree is walked manually here rather than through the protobuf-JSON binding, which does not
 * understand those shapes.
 */
public final class SyncFixtures {
    /**
     * The classpath prefix every sync-package fixture lives under.
     */
    private static final String FIXTURE_ROOT = "fixtures/sync";

    /**
     * Hidden constructor; this class only exposes static helpers.
     *
     * @throws AssertionError always, to fail loudly on reflective
     *                        instantiation
     */
    private SyncFixtures() {
        throw new AssertionError("SyncFixtures is not instantiable");
    }

    /**
     * Returns every captured stanza event in the named JSONL fixture,
     * preserving capture order.
     *
     * <p>Each line carries an {@code event} subobject, the canonical
     * record emitted by the stanza logger; that subobject is what is
     * returned.
     *
     * @param topic the fixture topic (for example
     *              {@code "exchange/regular-low/upload-archive"}),
     *              without the {@code .jsonl} extension
     * @return the parsed event objects in the order the capture
     *         records them
     * @throws UncheckedIOException     if the fixture is missing or
     *                                  unreadable
     * @throws IllegalStateException    if any record lacks an
     *                                  {@code event} subobject
     */
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
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns the first event in the named fixture matching a tag
     * and an attribute filter.
     *
     * <p>The attribute filter narrows on key/value pairs that uniquely
     * identify the wanted stanza inside a busy capture.
     *
     * @param topic the fixture topic
     * @param tag   the required stanza tag, or {@code null} to match
     *              any
     * @param attrs the required attribute key/value pairs, or
     *              {@code null} to skip attribute filtering
     * @return the first matching event
     * @throws AssertionError if no event matches
     */
    public static JSONObject loadEventWhere(String topic, String tag, JSONObject attrs) {
        for (var event : loadEvents(topic)) {
            if (tag != null && !tag.equals(event.getString("tag"))) continue;
            if (attrs != null) {
                var eventAttrs = event.getJSONObject("attrs");
                var allMatch = true;
                for (var key : attrs.keySet()) {
                    var expected = attrs.get(key);
                    var actual = eventAttrs == null ? null : eventAttrs.get(key);
                    if (!Objects.equals(String.valueOf(expected), String.valueOf(actual))) {
                        allMatch = false;
                        break;
                    }
                }
                if (!allMatch) continue;
            }
            return event;
        }
        throw new AssertionError("no event matched topic=" + topic + " tag=" + tag + " attrs=" + attrs);
    }

    /**
     * Reconstructs a Cobalt {@link Stanza} from the {@code stanza}
     * subtree of a captured event.
     *
     * <p>Walks the recursive plain-JSON shape emitted by the stanza
     * logger ({@code tag}, {@code attrs}, {@code content}), with binary
     * leaves wrapped as {@code {"kind": "binary", "base64": "..."}} and
     * nested children as arrays of the same shape.
     *
     * @param event the event object from
     *              {@link #loadEvents(String)}
     * @return the reconstructed {@link Stanza}
     * @throws IllegalArgumentException if the {@code stanza} subtree is
     *                                  missing or malformed
     */
    public static Stanza buildNodeFromEvent(JSONObject event) {
        Objects.requireNonNull(event, "event");
        var nodeTree = event.getJSONObject("stanza");
        if (nodeTree == null) {
            throw new IllegalArgumentException("event missing 'stanza' subtree");
        }
        return buildNode(nodeTree);
    }

    /**
     * Recursively reconstructs a {@link Stanza} from a captured
     * plain-JSON tree, skipping the {@code event["stanza"]} unwrap step.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed stanza
     * @throws IllegalArgumentException if {@code tree} has no
     *                                  {@code tag} field
     */
    public static Stanza buildNodeFromTree(JSONObject tree) {
        return buildNode(tree);
    }

    /**
     * Returns the expected-output JSON document paired with the
     * named fixture topic.
     *
     * <p>Loaded from {@code <topic>.expected.json} alongside the stanza
     * capture; the wrapper shape depends on the capture tool (an eval
     * capture returns the eval-outcome wrapper, a plain capture returns
     * the raw oracle).
     *
     * @param topic the fixture topic
     * @return the parsed expected document
     * @throws UncheckedIOException if the fixture is missing or
     *                              unreadable
     */
    public static JSONObject loadExpected(String topic) {
        Objects.requireNonNull(topic, "topic");
        var resource = FIXTURE_ROOT + "/" + topic + ".expected.json";
        try (var stream = open(resource)) {
            return JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns the inner result document for an eval-style oracle
     * fixture, unwrapping the {@code result.value} stringification done
     * by the live runtime.
     *
     * <p>Eval captures wrap the outcome as
     * {@code {schema, expression, result: {resultType: "string", value: "<json-string>"}}}
     * because the live runtime stringifies the result before returning
     * it through CDP, avoiding structured-clone hazards; this helper
     * undoes that stringification so tests assert against the inner
     * document directly.
     *
     * @param topic the fixture topic
     * @return the parsed inner result document
     * @throws IllegalStateException if the fixture is malformed or
     *                               the result is not a
     *                               {@code string} payload
     */
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

    /**
     * Decodes a base64-encoded byte field out of an oracle document
     * by walking a dotted field path (for example
     * {@code "patch.snapshotMac"} or {@code "indexKey"}).
     *
     * <p>Live oracle expressions cannot transport raw
     * {@code Uint8Array} payloads through CDP, so byte fields are
     * wrapped as {@code {"base64": "..."}} objects on the WA Web side
     * and decoded back into a {@code byte[]} here.
     *
     * @param oracle    the oracle JSON document (from
     *                  {@link #loadOracle(String)} or an inner
     *                  subobject)
     * @param fieldPath the dotted path of the wrapping object
     * @return the decoded byte array
     * @throws IllegalArgumentException if the path does not resolve
     *                                  to a base64 wrapper or to a
     *                                  raw base64 string
     */
    public static byte[] decodeOracleBytes(JSONObject oracle, String fieldPath) {
        Objects.requireNonNull(oracle, "oracle");
        Objects.requireNonNull(fieldPath, "fieldPath");
        var segments = fieldPath.split("\\.");
        Object cursor = oracle;
        for (var segment : segments) {
            if (!(cursor instanceof JSONObject obj)) {
                throw new IllegalArgumentException("path " + fieldPath + " traverses non-object at " + segment);
            }
            cursor = obj.get(segment);
            if (cursor == null) {
                throw new IllegalArgumentException("path " + fieldPath + " missing segment " + segment);
            }
        }
        if (cursor instanceof String s) {
            return Base64.getDecoder().decode(s);
        }
        if (cursor instanceof JSONObject wrapper) {
            var b64 = wrapper.getString("base64");
            if (b64 == null) {
                throw new IllegalArgumentException("path " + fieldPath + " resolved to object without 'base64' field: " + wrapper);
            }
            return Base64.getDecoder().decode(b64);
        }
        throw new IllegalArgumentException("path " + fieldPath + " resolved to non-bytes value: " + cursor);
    }

    /**
     * Returns the raw sync-key bytes captured alongside the named
     * fixture.
     *
     * <p>The key is the material the captured payload was encrypted
     * under, stored as a binary sibling to the JSONL and expected.json
     * files so it is never stringified through JSON.
     *
     * @param topic the fixture topic
     * @return the raw sync-key bytes (32 bytes for a valid
     *         app-state sync key)
     * @throws UncheckedIOException if the resource is missing or
     *                              unreadable
     */
    public static byte[] loadSyncKey(String topic) {
        Objects.requireNonNull(topic, "topic");
        var resource = FIXTURE_ROOT + "/" + topic + ".synckey.bin";
        try (var stream = open(resource)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns whether a JSONL stanza fixture exists for the given
     * topic. Oracle-only fixtures are probed through
     * {@link #findExpected(String)} instead.
     *
     * @param topic the fixture topic
     * @return {@code true} when {@code <topic>.jsonl} is on the
     *         classpath
     */
    public static boolean isAvailable(String topic) {
        return SyncFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + topic + ".jsonl") != null;
    }

    /**
     * Returns whether an expected-output document exists for the
     * given topic.
     *
     * @param topic the fixture topic
     * @return {@code true} when {@code <topic>.expected.json} is on
     *         the classpath
     */
    public static boolean isOracleAvailable(String topic) {
        return SyncFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + topic + ".expected.json") != null;
    }

    /**
     * Returns the expected-output document for the given topic, or
     * {@link Optional#empty()} when none is on the classpath, allowing
     * callers to skip cleanly when the fixture is absent.
     *
     * @param topic the fixture topic
     * @return the parsed document wrapped in {@link Optional}, or
     *         {@link Optional#empty()} when the file is missing
     * @throws UncheckedIOException if the file exists but cannot be
     *                              read or parsed
     */
    public static Optional<JSONObject> findExpected(String topic) {
        var resource = FIXTURE_ROOT + "/" + topic + ".expected.json";
        try (var stream = SyncFixtures.class.getResourceAsStream("/" + resource)) {
            if (stream == null) return Optional.empty();
            return Optional.of(JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns whether the per-syncType history-sync fixture triplet
     * exists for the given chunk-type slug.
     *
     * <p>History-sync fixtures live under
     * {@code fixtures/sync/history/<slug>/} as a triplet
     * ({@code notification.json}, {@code chunk.b64},
     * {@code expected.json}) rather than the flat {@code .jsonl} layout
     * of stanza fixtures, so they cannot use
     * {@link #isAvailable(String)}. Only the {@code chunk.b64} file is
     * probed because the three siblings are always written together by
     * the {@code split-history.mjs} splitter script.
     *
     * @param typeSlug the chunk-type slug (for example
     *                 {@code "initial-bootstrap"} or
     *                 {@code "on-demand"}); the values come from the
     *                 {@code TYPE_SLUG} map in the splitter script
     * @return {@code true} when the captured fixture is present on
     *         the classpath
     */
    public static boolean isHistoryChunkAvailable(String typeSlug) {
        Objects.requireNonNull(typeSlug, "typeSlug");
        return SyncFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/history/" + typeSlug + "/chunk.b64") != null;
    }

    /**
     * Returns the inflated {@code HistorySync} protobuf bytes
     * captured for the named chunk-type slug.
     *
     * <p>The bytes are the plaintext history-sync payload (the CDN blob
     * after decrypt-and-inflate, or the inline bootstrap payload after
     * inflate); they round-trip to the value carried by the sibling
     * {@code expected.json} document.
     *
     * @param typeSlug the chunk-type slug
     * @return the raw protobuf bytes
     * @throws UncheckedIOException if the fixture is missing or
     *                              malformed
     */
    public static byte[] loadHistoryChunkBytes(String typeSlug) {
        Objects.requireNonNull(typeSlug, "typeSlug");
        var resource = FIXTURE_ROOT + "/history/" + typeSlug + "/chunk.b64";
        try (var stream = open(resource)) {
            var b64 = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            return Base64.getDecoder().decode(b64);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns the {@code HistorySync} oracle JSON for the named
     * chunk-type slug.
     *
     * <p>This is the parsed protobuf value WA Web decodes from the
     * inflated bytes, captured verbatim through an in-page hook
     * installed by the {@code split-history.mjs} splitter script.
     *
     * @param typeSlug the chunk-type slug
     * @return the oracle document
     * @throws UncheckedIOException if the fixture is missing or
     *                              malformed
     */
    public static JSONObject loadHistoryExpected(String typeSlug) {
        Objects.requireNonNull(typeSlug, "typeSlug");
        var resource = FIXTURE_ROOT + "/history/" + typeSlug + "/expected.json";
        try (var stream = open(resource)) {
            return JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Returns the captured {@code HistorySyncNotification} payload
     * (in its pre-decode protobuf-JSON form) paired with the named
     * chunk-type slug.
     *
     * <p>The notification carries the {@code syncType},
     * {@code chunkOrder}, optional
     * {@code initialHistBootstrapInlinePayload} (gzip-compressed inline
     * chunk), and the {@code directPath} / {@code mediaKey} download
     * options. The outer wrapper additionally exposes {@code msgKey} and
     * {@code progress} pulled out of the WA Web dispatcher context.
     *
     * @param typeSlug the chunk-type slug
     * @return the notification document
     * @throws UncheckedIOException if the fixture is missing or
     *                              malformed
     */
    public static JSONObject loadHistoryNotification(String typeSlug) {
        Objects.requireNonNull(typeSlug, "typeSlug");
        var resource = FIXTURE_ROOT + "/history/" + typeSlug + "/notification.json";
        try (var stream = open(resource)) {
            return JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Builds an in-memory temporary store seeded with a self-PN, a
     * self-LID, and one app-state sync key.
     *
     * <p>Encrypted-mutation tests need a store that already knows the
     * sync key the captured payload was encrypted under, otherwise the
     * decryption path takes the missing-key branch. This helper wraps
     * {@link DeviceFixtures#temporaryStore(Jid, Jid)} and inserts a
     * single {@link AppStateSyncKey} carrying the supplied id and key
     * material. The {@link AppStateSyncKeyData} timestamp is set to
     * {@link Instant#now()} because the decryption path does not inspect
     * it; only the key id and the 32-byte key bytes need to round-trip.
     *
     * @param selfPn      the local user's PN-form bare JID
     * @param selfLid     the local user's LID-form bare JID, or
     *                    {@code null} for a pre-LID-migration store
     * @param syncKeyId   the sync key's raw id bytes (typically a
     *                    SHA-256 prefix, used verbatim)
     * @param syncKeyData the 32-byte symmetric key material
     * @return the configured temporary store with the key planted
     */
    public static LinkedWhatsAppStore temporaryStoreWithSyncKey(Jid selfPn, Jid selfLid, byte[] syncKeyId, byte[] syncKeyData) {
        Objects.requireNonNull(syncKeyId, "syncKeyId");
        Objects.requireNonNull(syncKeyData, "syncKeyData");
        var store = DeviceFixtures.temporaryStore(selfPn, selfLid);
        var keyId = new AppStateSyncKeyIdBuilder()
                .keyId(syncKeyId)
                .build();
        var keyData = new AppStateSyncKeyDataBuilder()
                .keyData(syncKeyData)
                .timestamp(Instant.now())
                .build();
        var key = new AppStateSyncKeyBuilder()
                .keyId(keyId)
                .keyData(keyData)
                .build();
        store.syncStore().addWebAppStateKeys(List.of(key));
        return store;
    }

    /**
     * Opens the named classpath resource, surfacing a dedicated
     * {@link IOException} when it is missing so the wrapping
     * {@link UncheckedIOException} carries a clear cause.
     *
     * @param resourcePath the resource path under
     *                     {@code src/test/resources/}
     * @return an input stream over the resource bytes
     * @throws IOException if the resource is missing
     */
    private static InputStream open(String resourcePath) throws IOException {
        var stream = SyncFixtures.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("fixture not found on classpath: " + resourcePath);
        }
        return stream;
    }

    /**
     * Flattens a captured attribute value into the string form the
     * Cobalt {@link StanzaBuilder#attribute(String, String)} setter
     * expects.
     *
     * <p>The stanza logger captures WA Web's internal Jid wrappers as
     * {@code {"$1": {"type": <int>, "user": <string|null>, "server": <string>}}};
     * Cobalt's {@link Stanza} carries those same JIDs as bare strings of
     * the form {@code user@server} (or just {@code @server} when
     * {@code user} is {@code null}). Any other shape is delegated to
     * {@link String#valueOf(Object)}.
     *
     * @param value the raw captured attribute value
     * @return the string-form value
     */
    private static String attrValueAsString(Object value) {
        if (value instanceof JSONObject obj && obj.containsKey("$1")) {
            var inner = obj.getJSONObject("$1");
            var user = inner.getString("user");
            var server = inner.getString("server");
            return (user == null ? "" : user) + "@" + (server == null ? "" : server);
        }
        return String.valueOf(value);
    }

    /**
     * Decodes a binary leaf object into raw bytes.
     *
     * @param binary the {@code {kind: "binary", base64: "..."}}
     *               object
     * @return the decoded bytes
     * @throws IllegalArgumentException if the object lacks a
     *                                  {@code base64} field
     */
    private static byte[] decodeBinary(JSONObject binary) {
        var base64 = binary.getString("base64");
        if (base64 == null) {
            throw new IllegalArgumentException("binary leaf missing 'base64' field: " + binary);
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Builds a {@link Stanza} from a plain-JSON tree, recursively.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed stanza
     * @throws IllegalArgumentException if {@code tree} has no
     *                                  {@code tag} field
     */
    private static Stanza buildNode(JSONObject tree) {
        var tag = tree.getString("tag");
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("stanza tree missing 'tag': " + tree);
        }

        var builder = new StanzaBuilder().description(tag);

        var attrs = tree.getJSONObject("attrs");
        if (attrs != null) {
            for (var key : attrs.keySet()) {
                var value = attrs.get(key);
                if (value == null) continue;
                builder.attribute(key, attrValueAsString(value));
            }
        }

        var content = tree.get("content");
        applyContent(builder, content);
        return builder.build();
    }

    /**
     * Applies a {@code content} value onto the supplied builder.
     *
     * <p>Handles the four shapes the stanza logger emits for the
     * {@code content} field: a binary leaf object, a child array (with
     * optional inline binary sibling), a raw string, or {@code null}.
     *
     * @param builder the target {@link StanzaBuilder}
     * @param content the JSON-shaped content value, possibly
     *                {@code null}
     */
    private static void applyContent(StanzaBuilder builder, Object content) {
        if (content == null) return;

        if (content instanceof JSONObject leaf) {
            if ("binary".equals(leaf.getString("kind"))) {
                builder.content(decodeBinary(leaf));
                return;
            }
            builder.content(buildNode(leaf));
            return;
        }

        if (content instanceof JSONArray children) {
            var built = new ArrayList<Stanza>(children.size());
            byte[] inlineBytes = null;
            for (var entry : children) {
                if (entry instanceof JSONObject obj) {
                    if ("binary".equals(obj.getString("kind"))) {
                        inlineBytes = decodeBinary(obj);
                        continue;
                    }
                    built.add(buildNode(obj));
                } else if (entry != null) {
                    built.add(new StanzaBuilder().description("__text").content(String.valueOf(entry)).build());
                }
            }
            if (!built.isEmpty()) {
                builder.content(built);
            } else if (inlineBytes != null) {
                builder.content(inlineBytes);
            }
            return;
        }

        builder.content(String.valueOf(content));
    }
}
