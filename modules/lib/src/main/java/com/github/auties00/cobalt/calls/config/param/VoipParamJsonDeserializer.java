package com.github.auties00.cobalt.calls.config.param;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.lang.System.Logger.Level;

/**
 * Parses an uncompressed {@code <voip_settings>} JSON document into a {@link VoipParams} set.
 *
 * <p>The {@code <voip_settings uncompressed="1">} element carries plaintext JSON, not a protobuf or a
 * compressed blob. The document is a tree of nested objects keyed by the section names ({@code aec},
 * {@code bwe}, {@code encode}, and the rest) plus flat scalar fields and a
 * {@code voip_settings_version} object.
 *
 * <p>The set is keyed by wire path. This deserializer flattens the tree to {@code section.key} leaves,
 * resolves each leaf to its {@link VoipParamKey} through {@link VoipParamKey#ofWirePath(String)}, and
 * stores the raw leaf value under that key; a leaf whose wire path is not modelled is stored under an
 * {@linkplain VoipParamKey#unknown(String) unknown} key for the same path, so no parsed value is
 * dropped. The typed {@link VoipParams} accessors coerce the raw value on read.
 *
 * @implNote This implementation keys each leaf by the document's own area sectioned wire path rather
 * than by the engine struct path. The {@code rc_dyn} and {@code vid_rc_dyn} dynamic rate control rule
 * arrays are stored as opaque leaf values; compiling them into executable rules is the job of the
 * BWE/rate control reader that owns the rule table format, not of this deserializer.
 */
public final class VoipParamJsonDeserializer {
    /**
     * The logger for {@link VoipParamJsonDeserializer}.
     */
    private static final System.Logger LOGGER = Log.get(VoipParamJsonDeserializer.class);

    /**
     * Constructs a voip param JSON deserializer.
     *
     * <p>The deserializer is stateless; one instance can parse any number of documents.
     */
    public VoipParamJsonDeserializer() {

    }

    /**
     * Parses the given JSON text into a voip param set.
     *
     * <p>The text must be the uncompressed JSON body of a {@code <voip_settings>} element, a single
     * JSON object at its root. Every leaf is stored under the {@link VoipParamKey} its
     * {@code section.key} wire path resolves to, or under an {@linkplain VoipParamKey#unknown(String)
     * unknown} key for that path when the wire path is not modelled.
     *
     * @param json the uncompressed JSON body of a {@code <voip_settings>} element
     * @return the parsed voip param set
     * @throws NullPointerException     if {@code json} is {@code null}
     * @throws IllegalArgumentException if {@code json} is not a single JSON object
     */
    public VoipParams parse(String json) {
        if (json == null) {
            throw new NullPointerException("json must not be null");
        }
        JSONObject root;
        try {
            root = JSON.parseObject(json);
        } catch (JSONException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "voip_settings body is not valid JSON", exception);
            }
            throw new IllegalArgumentException("voip_settings body is not valid JSON", exception);
        }
        if (root == null) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "voip_settings body is not a JSON object");
            }
            throw new IllegalArgumentException("voip_settings body is not a JSON object");
        }
        var params = new VoipParams();
        flatten(root, "", params);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed voip params json, paramCount={0}", params.size());
        }
        return params;
    }

    /**
     * Walks the document and stores every leaf under the key its flattened wire path resolves to.
     *
     * <p>Nested objects are descended with their key appended to the running prefix joined by
     * {@code .}; the {@code voip_settings_version} object is descended like any other so its fields are
     * retained. Each non object leaf (a scalar or an array, such as the {@code rc_dyn} rule array) is
     * stored verbatim under {@link VoipParamKey#ofWirePath(String)} for its path, falling back to an
     * {@linkplain VoipParamKey#unknown(String) unknown} key when the path is not modelled, so the parsed
     * document is preserved in full.
     *
     * @param node   the current JSON object being walked
     * @param prefix the running dotted prefix for keys at this depth
     * @param params the set to store leaves into
     */
    private void flatten(JSONObject node, String prefix, VoipParams params) {
        for (var entry : node.entrySet()) {
            var path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            var value = entry.getValue();
            if (value instanceof JSONObject child) {
                flatten(child, path, params);
            } else {
                var key = VoipParamKey.ofWirePath(path).orElseGet(() -> {
                    if (Log.TRACE) {
                        LOGGER.log(Level.TRACE, "voip param wire path not modelled, path={0}", path);
                    }
                    return VoipParamKey.unknown(path);
                });
                params.put(key, value);
            }
        }
    }
}
