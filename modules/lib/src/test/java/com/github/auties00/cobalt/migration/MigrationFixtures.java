package com.github.auties00.cobalt.migration;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Test harness that loads migration-package fixtures captured from a live WhatsApp Web session and
 * exposes them to JUnit tests. Both fixture families live under
 * {@code src/test/resources/fixtures/migration/} and are discovered on the classpath; missing fixtures
 * are reported by {@link #isAvailable(String)} so a corpus-less checkout can opt in cleanly. Two shapes
 * are supported: JSONL stanza captures decoded back into {@link Stanza} instances via {@link #loadEvents(String)}
 * and {@link #buildNode(JSONObject)}, and eval-style oracle outputs exposed verbatim through
 * {@link #loadExpected(String)} or unwrapped through {@link #loadOracle(String)}.
 */
public final class MigrationFixtures {
    /**
     * The classpath prefix under which every migration fixture is
     * placed.
     */
    private static final String FIXTURE_ROOT = "fixtures/migration";

    /**
     * Prevents instantiation; this class exposes only static helpers.
     */
    private MigrationFixtures() {
        throw new AssertionError("MigrationFixtures is not instantiable");
    }

    /**
     * Loads every captured stanza event in the given JSONL fixture, in capture order. Pair with
     * {@link #loadEventWhere(String, String, JSONObject)} to single out a specific event by tag and
     * attributes.
     *
     * @param topic the fixture topic without the {@code .jsonl}
     *              extension
     * @return the {@code event} sub-objects, in capture order
     * @throws UncheckedIOException when the fixture is missing or
     *                              malformed
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
     * Returns the first event in the given fixture whose {@code tag} matches and whose attributes contain
     * every key/value pair in {@code attrs}, for picking a single representative stanza out of a longer
     * capture.
     *
     * @param topic the fixture topic
     * @param tag   the required stanza tag, or {@code null} to match
     *              any tag
     * @param attrs the required attribute key/value pairs, or
     *              {@code null} to skip attribute filtering
     * @return the first matching event
     * @throws AssertionError when no event matches
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
     * Reconstructs a Cobalt {@link Stanza} from the {@code stanza} sub-tree of a captured event, for driving
     * the Cobalt stanza receivers against captured input.
     *
     * @param event the event object returned by
     *              {@link #loadEvents(String)}
     * @return the reconstructed {@link Stanza}
     * @throws IllegalArgumentException when the tree is malformed
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
     * Reconstructs a {@link Stanza} from a bare plain-JSON {@code {tag, attrs, content}} tree, without the
     * enclosing {@code {event: {stanza: ...}}} envelope produced by {@link #loadEvents(String)}.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed {@link Stanza}
     * @throws IllegalArgumentException when {@code tree} has no tag
     */
    public static Stanza buildNodeFromTree(JSONObject tree) {
        return buildNode(tree);
    }

    /**
     * Loads the raw {@code <topic>.expected.json} document paired with the given fixture topic, so tests
     * can pick out the fields they care about. For the common eval-style payload use
     * {@link #loadOracle(String)} instead.
     *
     * @param topic the fixture topic
     * @return the parsed expected document
     * @throws UncheckedIOException when the fixture is missing or
     *                              malformed
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
     * Returns the unwrapped live-runtime result payload for an eval-style oracle fixture. Such captures
     * wrap the evaluation outcome as
     * {@code {schema, expression, result: {resultType: "string", value: "<json-string>"}}}: the oracle
     * stringifies its result so the live runtime can deliver it without structured-clone hazards, and this
     * helper undoes the stringification in one place.
     *
     * @param topic the fixture topic
     * @return the parsed inner result document
     * @throws IllegalStateException when the fixture is malformed
     *                               or the result is not a
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
     * Returns whether the given fixture topic exists on the classpath, serving as the opt-in gate for
     * fresh checkouts where the live captures are committed separately and a topic-specific fixture may
     * not yet be present.
     *
     * @param topic the fixture topic
     * @return {@code true} when {@code <topic>.jsonl} is on the
     *         classpath
     */
    public static boolean isAvailable(String topic) {
        return MigrationFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + topic + ".jsonl") != null;
    }

    /**
     * Returns the parsed expected document for the given topic, or an empty {@link Optional} when no
     * expected file is present, so tests can gracefully skip when the oracle output is not yet committed.
     *
     * @param topic the fixture topic
     * @return the parsed document, or {@link Optional#empty()} when
     *         missing
     */
    public static Optional<JSONObject> findExpected(String topic) {
        var resource = FIXTURE_ROOT + "/" + topic + ".expected.json";
        try (var stream = MigrationFixtures.class.getResourceAsStream("/" + resource)) {
            if (stream == null) return Optional.empty();
            return Optional.of(JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Creates an in-memory temporary {@link LinkedWhatsAppStore} configured with the given self-PN and optional
     * self-LID, the standard {@code store} dependency for any migration-package test that builds a
     * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient} harness. The store is pre-advanced past the
     * offline-delivery gate so {@link LidMigrationService#executeMigration()} proceeds immediately;
     * production code drives that gate through the connection lifecycle.
     *
     * @param selfPn  the local user's PN-form bare JID
     * @param selfLid the local user's LID-form bare JID, or
     *                {@code null} for a pre-LID-migration store
     * @return the configured temporary store
     * @throws UncheckedIOException when the underlying factory cannot
     *                              create the store
     */
    public static LinkedWhatsAppStore temporaryStore(Jid selfPn, Jid selfLid) {
        Objects.requireNonNull(selfPn, "selfPn");
        try {
            var store = LinkedWhatsAppStoreFactory.temporary()
                    .create(LinkedWhatsAppClientType.WEB, Long.parseLong(selfPn.user()));
            store.accountStore().setJid(selfPn);
            if (selfLid != null) {
                store.accountStore().setLid(selfLid);
            }
            store.connectionStore().setOfflineResumeState(LinkedWhatsAppClientOfflineResumeState.COMPLETE);
            return store;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create temporary store", e);
        }
    }

    /**
     * Opens the named classpath resource, centralising the lookup so every fixture-loading helper surfaces
     * a consistent error message when the resource is missing.
     *
     * @param resourcePath the resource path under
     *                     {@code src/test/resources/}
     * @return an open stream over the resource bytes
     * @throws IOException when the resource is missing
     */
    private static InputStream open(String resourcePath) throws IOException {
        var stream = MigrationFixtures.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("fixture not found on classpath: " + resourcePath);
        }
        return stream;
    }

    /**
     * Flattens a captured attribute value into the string form the Cobalt
     * {@link StanzaBuilder#attribute(String, String)} setter expects. Captured attributes may arrive as JID
     * objects with a {@code $1} wrapper encoding {@code user}, {@code server}, {@code domainType}, and
     * {@code device} fields; this helper unwraps it and rebuilds the canonical {@code user[:device]@server}
     * string form.
     *
     * @param value the raw captured attribute value
     * @return the canonical string form
     */
    private static String attrValueAsString(Object value) {
        if (value instanceof JSONObject obj && obj.containsKey("$1")) {
            var inner = obj.getJSONObject("$1");
            var user = inner.getString("user");
            var server = inner.getString("server");
            if (server != null) {
                return (user == null ? "" : user) + "@" + server;
            }
            var domainType = inner.getInteger("domainType");
            if (domainType != null) {
                var serverForDomain = switch (domainType) {
                    case 0 -> "s.whatsapp.net";
                    case 1 -> "lid";
                    default -> "domain" + domainType;
                };
                var deviceIndex = inner.getInteger("device");
                var deviceSuffix = (deviceIndex != null && deviceIndex != 0)
                        ? ":" + deviceIndex
                        : "";
                return (user == null ? "" : user) + deviceSuffix + "@" + serverForDomain;
            }
            return (user == null ? "" : user) + "@";
        }
        return String.valueOf(value);
    }

    /**
     * Decodes a captured binary leaf into raw bytes. Binary stanza content is captured as
     * {@code {kind: "binary", base64: "..."}}; this helper unwraps the base64 payload for
     * {@link #applyContent(StanzaBuilder, Object)}.
     *
     * @param binary the binary leaf object
     * @return the decoded bytes
     * @throws IllegalArgumentException when the object lacks the
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
     * Builds a {@link Stanza} by walking a plain-JSON {@code {tag, attrs, content}} tree. Shared core of
     * {@link #buildNodeFromEvent(JSONObject)} and {@link #buildNodeFromTree(JSONObject)}; recurses into
     * nested objects through {@link #applyContent(StanzaBuilder, Object)}.
     *
     * @param tree the captured tree
     * @return the reconstructed {@link Stanza}
     * @throws IllegalArgumentException when {@code tree} has no tag
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
     * Applies a captured {@code content} value onto the given builder, handling the four content shapes
     * WA Web emits over its capture channel (binary leaf, child array, string, or {@code null}) and
     * rebuilding the structure the Cobalt {@link StanzaBuilder} expects.
     *
     * @param builder the target builder
     * @param content the JSON-shaped content value
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
