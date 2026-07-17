package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decoded reply to the usync MEX query.
 *
 * <p>Consumed after dispatching {@link UsyncMexRequest}. Each {@link Item} corresponds to a
 * {@code xwa2_fetch_wa_users} entry; which sub-objects are populated depends on which
 * {@code include_*} flags the request enabled.
 *
 * @see UsyncMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexUsync")
public final class UsyncMexResponse implements MexStanza.Response.Json {
    /**
     * The decoded {@code xwa2_fetch_wa_users} array, one entry per user.
     */
    private final List<Item> items;

    /**
     * Wraps a pre-parsed list of per-user records.
     *
     * @param items the per-user records produced by {@link #of(byte[])}
     */
    private UsyncMexResponse(List<Item> items) {
        this.items = items;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>The argument is the IQ stanza received in reply to a stanza dispatched with
     * {@link UsyncMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or
     *         malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsync", exports = "mexUsyncQuery",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<UsyncMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(UsyncMexResponse::of);
    }

    /**
     * Returns the decoded {@code xwa2_fetch_wa_users} entries.
     *
     * <p>The list reflects the order the relay returned, which mirrors the order of the queried
     * users; callers key the reply back to the request batch by {@link Item#jid()}.
     *
     * @return the per-user records; may be empty, never {@code null}
     */
    public List<Item> items() {
        return items;
    }

    /**
     * A decoded {@code xwa2_fetch_wa_users} entry for one user.
     *
     * <p>The {@link #countryCode()}, {@link #usernameInfo()}, and {@link #aboutStatusInfo()}
     * sub-objects are only populated when the corresponding {@code include_*} flag was set on the
     * {@link UsyncMexRequest}.
     */
    public static final class Item {
        /**
         * The {@code jid} field carrying the user identifier, possibly {@code null}.
         */
        private final String jid;

        /**
         * The {@code country_code} field, possibly {@code null}.
         */
        private final String countryCode;

        /**
         * The decoded {@code username_info} sub-object, possibly {@code null}.
         */
        private final UsernameInfo usernameInfo;

        /**
         * The decoded {@code about_status_info} sub-object, possibly {@code null}.
         */
        private final AboutStatusInfo aboutStatusInfo;

        /**
         * The {@code id} field carrying the relay's row identifier, possibly {@code null}.
         */
        private final String id;

        /**
         * Wraps the decoded fields of a user record.
         *
         * @param jid the {@code jid} field
         * @param countryCode the {@code country_code} field
         * @param usernameInfo the decoded {@code username_info} sub-object
         * @param aboutStatusInfo the decoded {@code about_status_info} sub-object
         * @param id the {@code id} row identifier
         */
        private Item(String jid, String countryCode, UsernameInfo usernameInfo, AboutStatusInfo aboutStatusInfo, String id) {
            this.jid = jid;
            this.countryCode = countryCode;
            this.usernameInfo = usernameInfo;
            this.aboutStatusInfo = aboutStatusInfo;
            this.id = id;
        }

        /**
         * Returns the user identifier.
         *
         * @return the JID wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
         *         omitted the field
         */
        public Optional<String> jid() {
            return Optional.ofNullable(jid);
        }

        /**
         * Returns the phone country code, when requested.
         *
         * <p>Populated only when {@link UsyncMexRequest} was dispatched with
         * {@code includeCountryCode=true}.
         *
         * @return the country code wrapped in an {@link Optional}, or {@link Optional#empty()} when
         *         absent
         */
        public Optional<String> countryCode() {
            return Optional.ofNullable(countryCode);
        }

        /**
         * Returns the decoded username record, when requested.
         *
         * <p>Populated only when {@link UsyncMexRequest} was dispatched with
         * {@code includeUsername=true}.
         *
         * @return the record wrapped in an {@link Optional}, or {@link Optional#empty()} when absent
         */
        public Optional<UsernameInfo> usernameInfo() {
            return Optional.ofNullable(usernameInfo);
        }

        /**
         * Returns the decoded about-status record, when requested.
         *
         * <p>Populated only when {@link UsyncMexRequest} was dispatched with
         * {@code includeAboutStatus=true}.
         *
         * @return the record wrapped in an {@link Optional}, or {@link Optional#empty()} when absent
         */
        public Optional<AboutStatusInfo> aboutStatusInfo() {
            return Optional.ofNullable(aboutStatusInfo);
        }

        /**
         * Returns the relay-assigned row identifier.
         *
         * @return the identifier wrapped in an {@link Optional}, or {@link Optional#empty()} when
         *         the relay omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Decoded {@code username_info} sub-object of a usync entry.
         *
         * <p>Carries the per-user view of the username state along with a {@link #status()} field
         * reporting whether the per-user fetch succeeded.
         */
        public static final class UsernameInfo {
            /**
             * The {@code username} field carrying the assigned identifier, possibly {@code null}.
             */
            private final String username;

            /**
             * The {@code state} field carrying the registration state token, possibly {@code null}.
             */
            private final String state;

            /**
             * The {@code timestamp} field, in epoch seconds, possibly {@code null}.
             */
            private final Long timestamp;

            /**
             * The {@code pin} field carrying the recovery PIN hash, possibly {@code null}.
             */
            private final String pin;

            /**
             * The {@code status} field carrying the per-user fetch outcome, possibly {@code null}.
             */
            private final String status;

            /**
             * Wraps the decoded fields of one username record.
             *
             * @param username the {@code username} field
             * @param state the {@code state} field
             * @param timestamp the {@code timestamp} field, in epoch seconds
             * @param pin the {@code pin} field
             * @param status the {@code status} field
             */
            private UsernameInfo(String username, String state, Long timestamp, String pin, String status) {
                this.username = username;
                this.state = state;
                this.timestamp = timestamp;
                this.pin = pin;
                this.status = status;
            }

            /**
             * Returns the assigned username.
             *
             * @return the username wrapped in an {@link Optional}, or {@link Optional#empty()} when
             *         the relay omitted the field
             */
            public Optional<String> username() {
                return Optional.ofNullable(username);
            }

            /**
             * Returns the registration state token.
             *
             * @return the state wrapped in an {@link Optional}, or {@link Optional#empty()} when the
             *         relay omitted the field
             */
            public Optional<String> state() {
                return Optional.ofNullable(state);
            }

            /**
             * Returns the timestamp the username was registered at.
             *
             * @return the instant wrapped in an {@link Optional}, or {@link Optional#empty()} when
             *         the relay omitted the field
             */
            public Optional<Instant> timestamp() {
                return Optional.ofNullable(timestamp).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the recovery PIN hash bound to the username.
             *
             * @return the hash wrapped in an {@link Optional}, or {@link Optional#empty()} when the
             *         relay omitted the field
             */
            public Optional<String> pin() {
                return Optional.ofNullable(pin);
            }

            /**
             * Returns the per-user fetch outcome.
             *
             * @return the status wrapped in an {@link Optional}, or {@link Optional#empty()} when
             *         the relay omitted the field
             */
            public Optional<String> status() {
                return Optional.ofNullable(status);
            }

            /**
             * Decodes a single username sub-object from a {@link JSONObject}.
             *
             * <p>Used by {@link Item#of(JSONObject)} while projecting the {@code username_info}
             * sub-object.
             *
             * @param obj the JSON object to decode, possibly {@code null}
             * @return the decoded record, or {@link Optional#empty()} when {@code obj} is
             *         {@code null}
             */
            static Optional<UsernameInfo> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var username = obj.getString("username");
                var state = obj.getString("state");
                var timestamp = obj.getLong("timestamp");
                var pin = obj.getString("pin");
                var status = obj.getString("status");
                return Optional.of(new UsernameInfo(username, state, timestamp, pin, status));
            }

            /**
             * Decodes a list of username sub-objects from a {@link JSONArray}.
             *
             * <p>Kept for parity with the other {@code ofArray} helpers; the response decoder does
             * not invoke it because the wire schema carries {@code username_info} as a single
             * sub-object, not an array.
             *
             * @param arr the JSON array to decode, possibly {@code null}
             * @return the decoded records in source order; empty when {@code arr} is {@code null}
             */
            static List<UsernameInfo> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<UsernameInfo>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Decoded {@code about_status_info} sub-object of a usync entry.
         *
         * <p>Carries the about-status text together with its last-update timestamp and the per-user
         * fetch outcome reported by {@link #status()}.
         */
        public static final class AboutStatusInfo {
            /**
             * The {@code text} field carrying the about-status body, possibly {@code null}.
             */
            private final String text;

            /**
             * The {@code timestamp} field, in epoch seconds, possibly {@code null}.
             */
            private final Long timestamp;

            /**
             * The {@code status} field carrying the per-user fetch outcome, possibly {@code null}.
             */
            private final String status;

            /**
             * Wraps the decoded fields of one about-status record.
             *
             * @param text the {@code text} field
             * @param timestamp the {@code timestamp} field, in epoch seconds
             * @param status the {@code status} field
             */
            private AboutStatusInfo(String text, Long timestamp, String status) {
                this.text = text;
                this.timestamp = timestamp;
                this.status = status;
            }

            /**
             * Returns the about-status body.
             *
             * @return the text wrapped in an {@link Optional}, or {@link Optional#empty()} when the
             *         relay omitted the field
             */
            public Optional<String> text() {
                return Optional.ofNullable(text);
            }

            /**
             * Returns the timestamp the about-status was last updated at.
             *
             * @return the instant wrapped in an {@link Optional}, or {@link Optional#empty()} when
             *         the relay omitted the field
             */
            public Optional<Instant> timestamp() {
                return Optional.ofNullable(timestamp).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the per-user fetch outcome for the about-status.
             *
             * @return the status wrapped in an {@link Optional}, or {@link Optional#empty()} when
             *         the relay omitted the field
             */
            public Optional<String> status() {
                return Optional.ofNullable(status);
            }

            /**
             * Decodes a single about-status sub-object from a {@link JSONObject}.
             *
             * <p>Used by {@link Item#of(JSONObject)} while projecting the {@code about_status_info}
             * sub-object.
             *
             * @param obj the JSON object to decode, possibly {@code null}
             * @return the decoded record, or {@link Optional#empty()} when {@code obj} is
             *         {@code null}
             */
            static Optional<AboutStatusInfo> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var text = obj.getString("text");
                var timestamp = obj.getLong("timestamp");
                var status = obj.getString("status");
                return Optional.of(new AboutStatusInfo(text, timestamp, status));
            }

            /**
             * Decodes a list of about-status sub-objects from a {@link JSONArray}.
             *
             * <p>Kept for parity with the other {@code ofArray} helpers; the response decoder does
             * not invoke it because the wire schema carries {@code about_status_info} as a single
             * sub-object, not an array.
             *
             * @param arr the JSON array to decode, possibly {@code null}
             * @return the decoded records in source order; empty when {@code arr} is {@code null}
             */
            static List<AboutStatusInfo> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<AboutStatusInfo>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Decodes a single user record from a {@link JSONObject}.
         *
         * <p>Used by {@link #ofArray(JSONArray)} while walking the {@code xwa2_fetch_wa_users} array.
         *
         * @param obj the JSON object to decode, possibly {@code null}
         * @return the decoded record, or {@link Optional#empty()} when {@code obj} is {@code null}
         */
        static Optional<Item> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var jid = obj.getString("jid");
            var countryCode = obj.getString("country_code");
            var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
            var aboutStatusInfo = AboutStatusInfo.of(obj.getJSONObject("about_status_info")).orElse(null);
            var id = obj.getString("id");
            return Optional.of(new Item(jid, countryCode, usernameInfo, aboutStatusInfo, id));
        }

        /**
         * Decodes the {@code xwa2_fetch_wa_users} array of the MEX payload.
         *
         * <p>Used by the package-level decoder to project the array nested under {@code data} of the
         * {@code <result>} payload.
         *
         * @param arr the JSON array to decode, possibly {@code null}
         * @return the decoded records in source order; empty when {@code arr} is {@code null}
         */
        static List<Item> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Item>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Decodes the {@code <result>} payload bytes into a {@link UsyncMexResponse}.
     *
     * @implNote This implementation projects {@code data.xwa2_fetch_wa_users}; a missing
     * {@code data} envelope yields {@link Optional#empty()}, while a missing or empty array yields a
     * response with an empty {@link #items()} list.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse or
     *         lacks the {@code data} envelope
     */
    private static Optional<UsyncMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_fetch_wa_users");
        var items = Item.ofArray(rootArr);

        return Optional.of(new UsyncMexResponse(items));
    }
}
