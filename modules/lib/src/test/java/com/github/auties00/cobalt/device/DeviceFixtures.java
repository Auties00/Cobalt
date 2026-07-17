package com.github.auties00.cobalt.device;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
 * Test harness that loads device-package fixtures captured from a live WhatsApp Web session.
 *
 * <p>Two fixture families live under {@code src/test/resources/fixtures/device/}: JSONL stanza
 * dumps (rehydrated into a Cobalt {@link Stanza} through {@link #loadEvents(String)} and
 * {@link #buildNodeFromEvent(JSONObject)}), and {@code .expected.json} oracle outputs (exposed as
 * raw {@link JSONObject}). Every fixture path lives under {@code FIXTURE_ROOT}; missing fixtures
 * are reported via {@link #isAvailable(String)} so tests can skip cleanly rather than hard-failing.
 */
public final class DeviceFixtures {
    private static final String FIXTURE_ROOT = "fixtures/device";

    private DeviceFixtures() {
        throw new AssertionError("DeviceFixtures is not instantiable");
    }

    /**
     * Returns every captured stanza event in the given JSONL fixture, in capture order.
     *
     * @param topic the fixture topic (e.g. {@code "usync-self"}), without the {@code .jsonl}
     *              extension
     * @return the list of {@code event} sub-objects
     * @throws UncheckedIOException if the fixture is missing or malformed
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
     * Returns the first event in the given fixture whose tag and attributes match.
     *
     * @param topic the fixture topic
     * @param tag   the required stanza tag, or {@code null} to match any
     * @param attrs the required attribute key/value pairs, or {@code null} to skip attribute
     *              filtering
     * @return the matching event
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
     * Reconstructs a Cobalt {@link Stanza} from the {@code stanza} sub-tree of a captured event.
     *
     * @param event the event object from {@link #loadEvents(String)}
     * @return the reconstructed {@link Stanza}
     * @throws IllegalArgumentException if the tree is malformed
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
     * Reconstructs a {@link Stanza} from a captured {@code {tag, attrs, content}} tree already
     * extracted from a parent event.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed stanza
     * @throws IllegalArgumentException if {@code tree} has no tag
     */
    public static Stanza buildNodeFromTree(JSONObject tree) {
        return buildNode(tree);
    }

    /**
     * Returns the {@code <topic>.expected.json} document paired with the given fixture topic, as a
     * raw fastjson document.
     *
     * @param topic the fixture topic
     * @return the parsed expected document
     * @throws UncheckedIOException if the fixture is missing or malformed
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
     * Returns the live-runtime result payload for an eval-style oracle fixture, unwrapping the
     * {@code result.value} field and re-parsing it as JSON.
     *
     * <p>Oracle captures wrap the evaluation outcome as
     * {@code {schema, expression, result: {resultType: "string", value: "<json-string>"}}}; the
     * oracle stringifies its result before returning so the live runtime can deliver it without
     * structured-clone hazards. This helper undoes that stringification.
     *
     * @param topic the fixture topic
     * @return the parsed inner result document
     * @throws IllegalStateException if the fixture is malformed or the result is not a
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
     * Returns whether the given fixture topic's JSONL stanza dump exists on the classpath.
     *
     * <p>Checks only for the JSONL dump; tests that also depend on an accompanying oracle should
     * check both.
     *
     * @param topic the fixture topic
     * @return {@code true} when {@code <topic>.jsonl} is on the classpath
     */
    public static boolean isAvailable(String topic) {
        return DeviceFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + topic + ".jsonl") != null;
    }

    /**
     * Creates an in-memory temporary {@link LinkedWhatsAppStore} pre-configured with the given self-PN
     * and self-LID.
     *
     * @param selfPn  the local user's PN-form bare JID
     * @param selfLid the local user's LID-form bare JID, or {@code null} for a pre-LID-migration
     *                store
     * @return the configured temporary store
     * @throws UncheckedIOException if the underlying factory cannot create the store
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
            return store;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create temporary store", e);
        }
    }

    /**
     * Opens the named classpath resource.
     *
     * @param resourcePath the resource path under {@code src/test/resources/}
     * @return an input stream over the resource bytes
     * @throws IOException if the resource is missing
     */
    private static InputStream open(String resourcePath) throws IOException {
        var stream = DeviceFixtures.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("fixture not found on classpath: " + resourcePath);
        }
        return stream;
    }

    /**
     * Flattens a captured attribute value into the string form the
     * {@link StanzaBuilder#attribute(String, String)} setter expects.
     *
     * <p>The capture wraps internal JIDs as
     * {@code {"$1": {"type": <int>, "user": <string|null>, "server": <string>}}}; Cobalt's
     * {@link Stanza} carries those as bare {@code user@server} strings (or {@code @server} when
     * {@code user} is {@code null}). Any other shape is delegated to {@link String#valueOf(Object)}.
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
     * Decodes a captured {@code {kind: "binary", base64: "..."}} leaf into raw bytes.
     *
     * @param binary the {@code {kind: "binary", base64: "..."}} object
     * @return the decoded bytes
     * @throws IllegalArgumentException if the object lacks the {@code base64} field
     */
    private static byte[] decodeBinary(JSONObject binary) {
        var base64 = binary.getString("base64");
        if (base64 == null) {
            throw new IllegalArgumentException("binary leaf missing 'base64' field: " + binary);
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Recursively builds a {@link Stanza} from a {@code {tag, attrs, content}} tree.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed stanza
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
     * Applies a {@code content} value onto the builder, handling the four shapes the capture
     * emits: a single binary leaf, a child array (which may interleave binary leaves with child
     * nodes), a bare string, or {@code null}.
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
            // A single embedded child stanza is sometimes captured as a bare object, not a one-element array
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

    /**
     * Returns the parsed expected document for the given topic, or {@link Optional#empty()} when no
     * expected file accompanies the fixture.
     *
     * <p>Non-throwing variant of {@link #loadExpected(String)} so callers can skip cleanly instead
     * of catching {@link UncheckedIOException}.
     *
     * @param topic the fixture topic
     * @return the document, or {@link Optional#empty()} when no expected file accompanies the
     *         fixture
     */
    public static Optional<JSONObject> findExpected(String topic) {
        var resource = FIXTURE_ROOT + "/" + topic + ".expected.json";
        try (var stream = DeviceFixtures.class.getResourceAsStream("/" + resource)) {
            if (stream == null) return Optional.empty();
            return Optional.of(JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }
}
