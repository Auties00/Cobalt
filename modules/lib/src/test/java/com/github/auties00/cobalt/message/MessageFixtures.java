package com.github.auties00.cobalt.message;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientOfflineResumeState;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVSignedDeviceIdentityBuilder;
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
 * Test helper that loads message-package fixtures captured from a live WhatsApp Web session
 * and exposes them to JUnit tests. The fixtures come in two flavours, both rooted under
 * {@code src/test/resources/fixtures/message/}: JSONL stanza captures re-hydrated into Cobalt
 * {@link Stanza} instances by {@link #loadEvents(String)} and {@link #buildNodeFromEvent(JSONObject)},
 * and {@code .expected.json} oracle outputs returned as raw {@link JSONObject} so individual
 * tests can pull only the fields they care about. The class also builds in-memory temporary
 * stores via {@link #temporaryStore(Jid, Jid)} for any message-package class that needs a
 * {@link LinkedWhatsAppStore}.
 */
public final class MessageFixtures {
    private static final String FIXTURE_ROOT = "fixtures/message";

    private MessageFixtures() {
        throw new AssertionError("MessageFixtures is not instantiable");
    }

    /**
     * Returns every captured stanza event in the given JSONL fixture in capture order. Reads
     * the {@code <topic>.jsonl} file from the classpath, parses each line as a JSON record, and
     * extracts its {@code event} sub-object.
     *
     * @param topic the fixture topic (for example
     *              {@code "send/stanza/chat-fanout-self-lid"}), without the
     *              {@code .jsonl} extension
     * @return the {@code event} sub-objects, each shaped like the output of
     *         the MCP stanza logger
     * @throws NullPointerException if {@code topic} is {@code null}
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
     * Returns the first event in the given fixture whose {@code tag} matches and whose
     * attributes contain every key/value pair in {@code attrs}.
     *
     * @param topic the fixture topic
     * @param tag   the required stanza tag, or {@code null} to accept any
     * @param attrs the required attribute key/value pairs, or {@code null}
     *              to skip attribute filtering
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
     * The stanza tree is the recursive plain-JSON shape emitted by the MCP stanza-logger script:
     * binary leaves are {@code {"kind": "binary", "base64": "..."}} objects and child arrays
     * are arrays of the same shape.
     *
     * @param event the event object from {@link #loadEvents(String)}
     * @return the reconstructed {@link Stanza}
     * @throws NullPointerException     if {@code event} is {@code null}
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
     * Reconstructs a {@link Stanza} from a captured plain-JSON {@code {tag, attrs, content}}
     * sub-object held directly, rather than from the outer event wrapper.
     *
     * @param tree the {@code {tag, attrs, content}} object
     * @return the reconstructed stanza
     * @throws IllegalArgumentException if {@code tree} has no tag
     */
    public static Stanza buildNodeFromTree(JSONObject tree) {
        return buildNode(tree);
    }

    /**
     * Returns the expected-output JSON document paired with the given fixture topic, loaded
     * from {@code <topic>.expected.json} alongside the JSONL capture.
     *
     * @param topic the fixture topic
     * @return the parsed expected document
     * @throws NullPointerException if {@code topic} is {@code null}
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
     * {@code result.value} field and re-parsing it as JSON. The {@code web_live_debug_eval_to_file}
     * MCP capture wraps the evaluation outcome as
     * {@snippet :
     *     // {"schema": ..., "expression": ..., "result": {"resultType": "string", "value": "<json-string>"}}
     * }
     * The oracle invocations stringify their result before returning so the live runtime can
     * deliver it through CDP without structured-clone hazards; this helper undoes that
     * stringification.
     *
     * @param topic the fixture topic
     * @return the parsed inner result document
     * @throws IllegalStateException if the fixture is malformed or the
     *                               result is not a {@code string} payload
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
     * Returns whether the given fixture topic exists on the classpath, letting tests skip
     * cleanly when a corpus has not yet been captured.
     *
     * @param topic the fixture topic
     * @return {@code true} when {@code <topic>.jsonl} is on the classpath
     */
    public static boolean isAvailable(String topic) {
        return MessageFixtures.class.getResourceAsStream("/" + FIXTURE_ROOT + "/" + topic + ".jsonl") != null;
    }

    /**
     * Returns the parsed expected document for the given topic if it exists, as a soft variant
     * of {@link #loadExpected(String)} that lets the caller fall back to a default rather than
     * abort on a missing oracle.
     *
     * @param topic the fixture topic
     * @return the document, or {@link Optional#empty()} when no expected
     *         file accompanies the fixture
     */
    public static Optional<JSONObject> findExpected(String topic) {
        var resource = FIXTURE_ROOT + "/" + topic + ".expected.json";
        try (var stream = MessageFixtures.class.getResourceAsStream("/" + resource)) {
            if (stream == null) return Optional.empty();
            return Optional.of(JSON.parseObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resource, e);
        }
    }

    /**
     * Creates an in-memory temporary store seeded with the given self-PN and self-LID and a
     * stub signed-device-identity, for use as the {@link LinkedWhatsAppStore} dependency of any
     * message-package class. The store is preconfigured with offline-resume state COMPLETE so
     * tests that block on offline-delivery-end do not stall on the 5-minute latch, and with a
     * stub signed device identity so PKMSG-bearing fanouts ship a {@code <device-identity>}
     * child stanza; the signature bytes are dummies because tests only assert presence and shape.
     *
     * @param selfPn  the local user's PN-form bare JID
     * @param selfLid the local user's LID-form bare JID, or {@code null} for
     *                a pre-LID-migration store
     * @return the configured temporary store
     * @throws NullPointerException if {@code selfPn} is {@code null}
     * @throws UncheckedIOException if the underlying factory cannot create
     *                              the store
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
            store.signalStore().setSignedDeviceIdentity(new ADVSignedDeviceIdentityBuilder()
                    .details(new byte[]{0})
                    .accountSignatureKey(new byte[32])
                    .accountSignature(new byte[64])
                    .deviceSignature(new byte[64])
                    .build());
            return store;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create temporary store", e);
        }
    }

    /**
     * Opens the named classpath resource.
     *
     * @param resourcePath the resource path under
     *                     {@code src/test/resources/}
     * @return an input stream over the resource bytes
     * @throws IOException if the resource is missing
     */
    private static InputStream open(String resourcePath) throws IOException {
        var stream = MessageFixtures.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("fixture not found on classpath: " + resourcePath);
        }
        return stream;
    }

    /**
     * Flattens a captured attribute value into the string form Cobalt's
     * {@link StanzaBuilder#attribute(String, String)} expects. The MCP stanza logger captures
     * WAP's internal Jid wrappers in two shapes:
     * <ul>
     *   <li>Server JID:
     *       {@code {"$1": {"type": &lt;int&gt;, "user": &lt;string|null&gt;, "server": &lt;string&gt;}}},
     *       used for outer {@code <message to="...">} and other contexts
     *       where the JID carries an explicit server label.</li>
     *   <li>Device JID:
     *       {@code {"$1": {"type": &lt;int&gt;, "user": &lt;string&gt;, "device": &lt;int&gt;, "domainType": &lt;int&gt;}}},
     *       used inside {@code <participants><to jid="...">} where WAP packs
     *       the device index in-band. {@code domainType=0} maps to
     *       {@code @s.whatsapp.net}, {@code domainType=1} maps to
     *       {@code @lid}. A non-zero {@code device} prepends
     *       {@code :<device>} to the user (for example
     *       {@code 83116928594056:1@lid}); device 0 omits the suffix.</li>
     * </ul>
     * Cobalt's {@link Stanza} carries those same JIDs as bare strings of the
     * form {@code user@server} or {@code user:device@server}. Any other shape
     * is delegated to {@link String#valueOf(Object)}.
     *
     * @param value the raw captured attribute value
     * @return the string-form value
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
     * Decodes a binary leaf object into raw bytes.
     *
     * @param binary the {@code {"kind": "binary", "base64": "..."}} object
     * @return the decoded bytes
     * @throws IllegalArgumentException if the object is not a binary leaf
     */
    private static byte[] decodeBinary(JSONObject binary) {
        var base64 = binary.getString("base64");
        if (base64 == null) {
            throw new IllegalArgumentException("binary leaf missing 'base64' field: " + binary);
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Builds a {@link Stanza} from a plain-JSON tree.
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
     * Applies a {@code content} value (binary leaf, child array, string, or
     * {@code null}) onto the builder.
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
