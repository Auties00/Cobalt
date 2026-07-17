package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the delete-newsletter mutation built by
 * {@link DeleteNewsletterMexRequest}.
 *
 * <p>Exposes the deleted newsletter id and the post-deletion {@code state} object echoed under
 * {@code xwa2_newsletter_delete_v2}. A {@code null} root for this mutation indicates a server-side
 * failure; callers should treat an empty {@link Optional} from {@link #of(Stanza)} as that failure
 * signal.
 */
@WhatsAppWebModule(moduleName = "WAWebMexDeleteNewsletterJob")
public final class DeleteNewsletterMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the Jid string of the deleted newsletter, as echoed under
     * {@code xwa2_newsletter_delete_v2.id}.
     */
    private final String id;

    /**
     * Holds the post-deletion {@code state} object describing the terminal newsletter status.
     */
    private final State state;

    /**
     * Constructs a response wrapping the deleted newsletter id and its post-deletion state.
     *
     * @param id    the newsletter Jid echoed by the relay
     * @param state the post-deletion state object
     */
    private DeleteNewsletterMexResponse(String id, State state) {
        this.id = id;
        this.state = state;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_delete_v2} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<DeleteNewsletterMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(DeleteNewsletterMexResponse::of);
    }

    /**
     * Returns the Jid string of the deleted newsletter.
     *
     * @return the echoed newsletter id, or empty when the relay omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the post-deletion state object.
     *
     * @return the parsed {@link State}, or empty when the relay omitted it
     */
    public Optional<State> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Wraps the {@code state} sub-object embedded in the delete-newsletter response.
     *
     * <p>Carries the terminal newsletter status; the {@code type} value typically reads as the
     * {@code GONE} sentinel used by the WhatsApp backend to mark removed channels.
     */
    public static final class State {
        /**
         * Holds the textual state identifier.
         */
        private final String type;

        /**
         * Constructs a state wrapping the textual type.
         *
         * @param type the raw state identifier returned by the relay
         */
        private State(String type) {
            this.type = type;
        }

        /**
         * Returns the textual state identifier.
         *
         * @return the state type, or empty when the relay omitted the field
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Parses a single {@link State} object from the given JSON map.
         *
         * <p>Drives hydration of the {@code state} entry from
         * {@link DeleteNewsletterMexResponse#of(byte[])}.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link State}, or empty when {@code obj} is {@code null}
         */
        static Optional<State> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var type = obj.getString("type");
            return Optional.of(new State(type));
        }

        /**
         * Parses a list of {@link State} objects from the given JSON array.
         *
         * <p>Provided for symmetry; the delete-newsletter envelope does not carry a {@code state}
         * array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<State> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<State>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception; a {@code null}
     * {@code xwa2_newsletter_delete_v2} root, which the source treats as a server error, surfaces
     * here as empty.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_delete_v2} root
     */
    private static Optional<DeleteNewsletterMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_delete_v2");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = State.of(root.getJSONObject("state")).orElse(null);

        return Optional.of(new DeleteNewsletterMexResponse(id, state));
    }
}
