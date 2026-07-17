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
 * Parses the MEX response of the join-newsletter mutation built by
 * {@link JoinNewsletterMexRequest}.
 *
 * <p>Carries the newsletter id echoed under {@code xwa2_newsletter_join_v2} together with a
 * {@link State} marker. A {@link State} of type {@code DELETED} or {@code NON_EXISTING} signals
 * that the relay refused the join because the channel was removed, and {@code SUSPENDED} signals a
 * server-side suspension. Both fields are surfaced without throwing so callers decide their own
 * recovery policy.
 */
@WhatsAppWebModule(moduleName = "WAWebMexJoinNewsletterJob")
public final class JoinNewsletterMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string echoed under {@code id}.
     */
    private final String id;

    /**
     * Holds the lifecycle state marker echoed under {@code state}.
     */
    private final State state;

    /**
     * Constructs a response wrapping the echoed newsletter id and state.
     *
     * <p>Invoked only by the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param id    the newsletter Jid string echoed by the relay
     * @param state the lifecycle state marker
     */
    private JoinNewsletterMexResponse(String id, State state) {
        this.id = id;
        this.state = state;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_join_v2} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<JoinNewsletterMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(JoinNewsletterMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * <p>Empty when the GraphQL envelope omits {@code id}; otherwise carries the same Jid string
     * sent in {@link JoinNewsletterMexRequest}.
     *
     * @return the echoed newsletter id, or empty when omitted
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the lifecycle state marker the relay attached to the mutation result.
     *
     * <p>Empty when the GraphQL envelope omits {@code state}. A populated {@link State} of type
     * {@code DELETED} or {@code NON_EXISTING} signals that the relay refused the join because the
     * channel was removed, and {@code SUSPENDED} signals a server-side suspension.
     *
     * @return the parsed {@link State}, or empty when omitted
     */
    public Optional<State> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Models the lifecycle {@code state} marker echoed on the join-mutation result.
     *
     * <p>Carries the relay-defined state-type string verbatim; recovery is left to the caller. A
     * type of {@code DELETED} or {@code NON_EXISTING} corresponds to a removed channel and
     * {@code SUSPENDED} to a server-side suspension.
     */
    public static final class State {
        /**
         * Holds the relay-defined state-type string.
         */
        private final String type;

        /**
         * Constructs a parsed {@code state} value.
         *
         * <p>Invoked only by {@link #of(JSONObject)}.
         *
         * @param type the state-type string
         */
        private State(String type) {
            this.type = type;
        }

        /**
         * Returns the state-type string.
         *
         * <p>Empty when the GraphQL envelope omits {@code type}; otherwise carries the relay-defined
         * state-type identifier, for example {@code "ACTIVE"}, {@code "DELETED"},
         * {@code "NON_EXISTING"}, or {@code "SUSPENDED"}.
         *
         * @return the {@code type} value, or empty when omitted
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Parses a {@code state} fragment from the given JSON object.
         *
         * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null} so an absent fragment
         * cleanly back-propagates to {@link #state()}.
         *
         * @param obj the JSON object to parse
         * @return the parsed value, or empty when {@code obj} is {@code null}
         */
        static Optional<State> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var type = obj.getString("type");
            return Optional.of(new State(type));
        }

        /**
         * Parses every {@code state} fragment in the given JSON array.
         *
         * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed values, empty when {@code arr} is {@code null}
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
     * <p>Invoked only by the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_join_v2} root
     */
    private static Optional<JoinNewsletterMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_join_v2");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = State.of(root.getJSONObject("state")).orElse(null);

        return Optional.of(new JoinNewsletterMexResponse(id, state));
    }
}
