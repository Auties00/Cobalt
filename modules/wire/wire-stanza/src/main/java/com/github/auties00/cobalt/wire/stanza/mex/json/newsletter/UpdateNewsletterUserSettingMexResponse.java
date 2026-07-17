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
 * Parses the MEX response of the update-newsletter-user-setting mutation built by
 * {@link UpdateNewsletterUserSettingMexRequest}.
 *
 * <p>Hands back the newsletter Jid echoed under {@code xwa2_newsletter_update_user_setting}
 * together with a {@link State} lifecycle marker, which downstream code uses to apply the local
 * mute-state change to the chat or newsletter-metadata table.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateNewsletterUserSetting")
public final class UpdateNewsletterUserSettingMexResponse implements MexStanza.Response.Json {
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
     * <p>External callers obtain instances through {@link #of(Stanza)} rather than this constructor.
     *
     * @param id    the newsletter Jid string echoed by the relay
     * @param state the lifecycle state marker
     */
    private UpdateNewsletterUserSettingMexResponse(String id, State state) {
        this.id = id;
        this.state = state;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_update_user_setting} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<UpdateNewsletterUserSettingMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(UpdateNewsletterUserSettingMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * <p>Empty when the GraphQL envelope omits {@code id}; otherwise carries the same Jid string
     * sent in {@link UpdateNewsletterUserSettingMexRequest}.
     *
     * @return the echoed newsletter id, or empty when omitted
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the lifecycle state marker the relay attached to the mutation result.
     *
     * <p>Empty when the GraphQL envelope omits {@code state}; otherwise carries the relay-defined
     * state-type string, for example {@code "ACTIVE"} or {@code "DELETED"}.
     *
     * @return the parsed {@link State}, or empty when omitted
     */
    public Optional<State> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Models the lifecycle {@code state} marker echoed on the update-user-setting mutation result.
     *
     * <p>Carries the relay-defined state-type string that downstream code reads before applying the
     * local cache update.
     */
    public static final class State {
        /**
         * Holds the relay-defined state-type string.
         */
        private final String type;

        /**
         * Constructs a parsed {@code state} value.
         *
         * <p>Instances are produced by {@link #of(JSONObject)} rather than this constructor.
         *
         * @param type the state-type string
         */
        private State(String type) {
            this.type = type;
        }

        /**
         * Returns the state-type string.
         *
         * <p>Empty when the GraphQL envelope omits {@code type}.
         *
         * @return the {@code type} value, or empty when omitted
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Parses a {@code state} fragment from the given JSON object.
         *
         * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
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
         * <p>Returns {@link List#of()} when {@code arr} is {@code null}; each null or malformed
         * element is skipped rather than collected.
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
     * <p>Backs the public {@link #of(Stanza)} overload, which holds the raw JSON bytes so that callers
     * do not have to.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception, mirroring the defensive
     * null-checks in WhatsApp Web's caller.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_update_user_setting} root
     */
    private static Optional<UpdateNewsletterUserSettingMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_update_user_setting");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = State.of(root.getJSONObject("state")).orElse(null);

        return Optional.of(new UpdateNewsletterUserSettingMexResponse(id, state));
    }
}
