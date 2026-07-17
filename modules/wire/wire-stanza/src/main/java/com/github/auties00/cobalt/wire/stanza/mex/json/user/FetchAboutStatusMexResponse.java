package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decodes the reply to the about-status fetch query.
 *
 * <p>Each {@link Item} corresponds to one {@code xwa2_users_updates_since} entry. The head of
 * {@link Item#updates()} is the user's current about-status; older entries form the history surfaced
 * by the profile screen. An empty {@link #items()} indicates the relay returned no rows for any
 * requested user. Consume this type after dispatching {@link FetchAboutStatusMexRequest}.
 *
 * @see FetchAboutStatusMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchAboutStatusJob")
public final class FetchAboutStatusMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the decoded {@code xwa2_users_updates_since} array, one entry per user.
     */
    private final List<Item> items;

    /**
     * Wraps a pre-parsed list of per-user records.
     *
     * @param items the per-user records produced by {@link #of(byte[])}
     */
    private FetchAboutStatusMexResponse(List<Item> items) {
        this.items = items;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>The reply is empty when the IQ does not carry a {@code <result>} child or when its payload
     * bytes are absent or unreadable JSON. Pass the IQ stanza received in reply to a stanza dispatched
     * with {@link FetchAboutStatusMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAboutStatusJob", exports = "mexGetAbout",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchAboutStatusMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchAboutStatusMexResponse::of);
    }

    /**
     * Returns the decoded {@code xwa2_users_updates_since} entries.
     *
     * <p>The list reflects the order the relay returned, which mirrors the order of the queried
     * users. An empty list indicates the relay accepted the query but had no rows to return.
     *
     * @return the per-user records; may be empty, never {@code null}
     */
    public List<Item> items() {
        return items;
    }

    /**
     * Wraps the about-status update history for one user.
     *
     * <p>The head of {@link #updates()} is the user's current about-status; older entries form the
     * history surfaced by the profile screen.
     */
    public static final class Item {
        /**
         * Holds the decoded {@code updates} array carrying the history entries for this user,
         * head-first.
         */
        private final List<Updates> updates;

        /**
         * Wraps a pre-parsed list of update entries.
         *
         * @param updates the update entries returned by the relay for this user
         */
        private Item(List<Updates> updates) {
            this.updates = updates;
        }

        /**
         * Returns the decoded update entries for this user.
         *
         * <p>The head entry is the current about-status; subsequent entries are historical updates
         * ordered most-recent-first.
         *
         * @return the update entries; may be empty, never {@code null}
         */
        public List<Updates> updates() {
            return updates;
        }

        /**
         * Wraps a single about-status revision, exposing only its text payload.
         */
        public static final class Updates {
            /**
             * Holds the {@code text} field of this revision, possibly {@code null}.
             */
            private final String text;

            /**
             * Wraps the text payload of a decoded revision.
             *
             * @param text the {@code text} field of this revision
             */
            private Updates(String text) {
                this.text = text;
            }

            /**
             * Returns the about-status text recorded by this revision.
             *
             * @return the text wrapped in an {@link Optional}, or {@link Optional#empty()} when the
             *         relay omitted the field
             */
            public Optional<String> text() {
                return Optional.ofNullable(text);
            }

            /**
             * Decodes a single revision from a {@link JSONObject}.
             *
             * <p>Invoked by {@link #ofArray(JSONArray)} while walking the {@code updates} array of an
             * {@link Item}.
             *
             * @param obj the JSON object to decode, possibly {@code null}
             * @return the decoded revision, or {@link Optional#empty()} when {@code obj} is
             *         {@code null}
             */
            static Optional<Updates> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var text = obj.getString("text");
                return Optional.of(new Updates(text));
            }

            /**
             * Decodes the {@code updates} array of an {@link Item}.
             *
             * <p>Invoked by {@link Item#of(JSONObject)} to project the array nested inside each user
             * record.
             *
             * @param arr the JSON array to decode, possibly {@code null}
             * @return the decoded revisions in source order; empty when {@code arr} is {@code null}
             */
            static List<Updates> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Updates>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Decodes a single user record from a {@link JSONObject}.
         *
         * <p>Invoked by {@link #ofArray(JSONArray)} while walking the
         * {@code xwa2_users_updates_since} array.
         *
         * @param obj the JSON object to decode, possibly {@code null}
         * @return the decoded record, or {@link Optional#empty()} when {@code obj} is {@code null}
         */
        static Optional<Item> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var updates = Updates.ofArray(obj.getJSONArray("updates"));
            return Optional.of(new Item(updates));
        }

        /**
         * Decodes the {@code xwa2_users_updates_since} array of the MEX payload.
         *
         * <p>Invoked by {@link FetchAboutStatusMexResponse#of(byte[])} to project the array nested
         * under {@code data} of the {@code <result>} payload.
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
     * Decodes the {@code <result>} payload bytes into a {@link FetchAboutStatusMexResponse}.
     *
     * <p>The payload is projected from {@code data.xwa2_users_updates_since}. A missing {@code data}
     * envelope or root array yields {@link Optional#empty()}; a present-but-empty array yields a
     * response carrying an empty {@link #items()} list.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse as a
     *         JSON object or lacks the {@code data} envelope
     */
    private static Optional<FetchAboutStatusMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_users_updates_since");
        var items = Item.ofArray(rootArr);

        return Optional.of(new FetchAboutStatusMexResponse(items));
    }
}
