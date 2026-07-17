package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decodes the reply to the text-status batch query.
 *
 * <p>Each {@link Item} carries one user's text status, with its identifier, body, emoji decoration,
 * last-update timestamp, and ephemeral duration. The list is keyed back to the request batch by
 * {@link Item#jid()}. An empty {@link #items()} indicates the relay returned no rows for the queried
 * batch. Consume this type after dispatching {@link FetchTextStatusListMexRequest}.
 *
 * @see FetchTextStatusListMexRequest
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchTextStatusListJob")
public final class FetchTextStatusListMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the decoded {@code xwa2_text_status_list} array, one entry per user.
     */
    private final List<Item> items;

    /**
     * Wraps a pre-parsed list of per-user records.
     *
     * @param items the per-user records produced by {@link #of(byte[])}
     */
    private FetchTextStatusListMexResponse(List<Item> items) {
        this.items = items;
    }

    /**
     * Decodes the {@code <result>} child of an inbound MEX IQ.
     *
     * <p>Pass the IQ stanza received in reply to a stanza dispatched with
     * {@link FetchTextStatusListMexRequest#toStanza()}.
     *
     * @param stanza the IQ reply stanza
     * @return the decoded reply, or {@link Optional#empty()} when the payload is missing or malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchTextStatusListJob", exports = "mexGetTextStatusList",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchTextStatusListMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchTextStatusListMexResponse::of);
    }

    /**
     * Returns the decoded {@code xwa2_text_status_list} entries.
     *
     * <p>The list mirrors the order the relay returned, keyed back to the request batch by
     * {@link Item#jid()}.
     *
     * @return the per-user records; may be empty, never {@code null}
     */
    public List<Item> items() {
        return items;
    }

    /**
     * Wraps a decoded text-status record for one user.
     *
     * <p>{@link #lastUpdateTime()} and {@link #ephemeralDurationSec()} together drive the expiry
     * countdown rendered by the Status tab UI.
     */
    public static final class Item {
        /**
         * Holds the {@code jid} field identifying the status author.
         */
        private final String jid;

        /**
         * Holds the {@code text} field carrying the status body.
         */
        private final String text;

        /**
         * Holds the {@code last_update_time} field, in epoch seconds.
         */
        private final Long lastUpdateTime;

        /**
         * Holds the {@code ephemeral_duration_sec} field, in seconds.
         */
        private final Long ephemeralDurationSec;

        /**
         * Holds the decoded {@code emoji} sub-object, possibly {@code null}.
         */
        private final Emoji emoji;

        /**
         * Wraps the decoded fields of a single text-status row.
         *
         * @param jid the {@code jid} field
         * @param text the {@code text} field
         * @param lastUpdateTime the {@code last_update_time} field, in epoch seconds
         * @param ephemeralDurationSec the {@code ephemeral_duration_sec} field, in seconds
         * @param emoji the decoded {@code emoji} sub-object, or {@code null} when the relay omitted it
         */
        private Item(String jid, String text, Long lastUpdateTime, Long ephemeralDurationSec, Emoji emoji) {
            this.jid = jid;
            this.text = text;
            this.lastUpdateTime = lastUpdateTime;
            this.ephemeralDurationSec = ephemeralDurationSec;
            this.emoji = emoji;
        }

        /**
         * Returns the status author identifier.
         *
         * @return the author JID wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         relay omitted the field
         */
        public Optional<String> jid() {
            return Optional.ofNullable(jid);
        }

        /**
         * Returns the status text body.
         *
         * @return the body wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
         *         omitted the field
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns the timestamp the status was last updated at.
         *
         * <p>Used together with {@link #ephemeralDurationSec()} to compute the expiry instant
         * rendered in the Status tab.
         *
         * @return the timestamp wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         relay omitted the field
         */
        public Optional<Instant> lastUpdateTime() {
            return Optional.ofNullable(lastUpdateTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the duration after which the status expires.
         *
         * <p>A status whose {@link #lastUpdateTime()} plus this duration is in the past has already
         * expired and is elided from the UI.
         *
         * @return the duration wrapped in an {@link Optional}, or {@link Optional#empty()} when the
         *         relay omitted the field
         */
        public Optional<Duration> ephemeralDurationSec() {
            return Optional.ofNullable(ephemeralDurationSec).map(Duration::ofSeconds);
        }

        /**
         * Returns the decoded emoji decoration accompanying the status.
         *
         * @return the emoji wrapped in an {@link Optional}, or {@link Optional#empty()} when the relay
         *         omitted the field
         */
        public Optional<Emoji> emoji() {
            return Optional.ofNullable(emoji);
        }

        /**
         * Wraps the {@code emoji} sub-object of a text-status row.
         */
        public static final class Emoji {
            /**
             * Holds the {@code content} field carrying the emoji codepoints.
             */
            private final String content;

            /**
             * Wraps the decoded emoji content string.
             *
             * @param content the {@code content} field, possibly {@code null}
             */
            private Emoji(String content) {
                this.content = content;
            }

            /**
             * Returns the emoji codepoints.
             *
             * @return the content wrapped in an {@link Optional}, or {@link Optional#empty()} when the
             *         relay omitted the field
             */
            public Optional<String> content() {
                return Optional.ofNullable(content);
            }

            /**
             * Decodes a single emoji entry from a {@link JSONObject}.
             *
             * <p>Invoked by {@link Item#of(JSONObject)} when projecting the {@code emoji} sub-object.
             *
             * @param obj the JSON object to decode, possibly {@code null}
             * @return the decoded emoji, or {@link Optional#empty()} when {@code obj} is {@code null}
             */
            static Optional<Emoji> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var content = obj.getString("content");
                return Optional.of(new Emoji(content));
            }

            /**
             * Decodes a list of emoji entries from a {@link JSONArray}.
             *
             * <p>Provided for parity with the other {@code ofArray} helpers in this file; the response
             * decoder does not invoke it because the wire schema carries {@code emoji} as a single
             * sub-object, not an array.
             *
             * @param arr the JSON array to decode, possibly {@code null}
             * @return the decoded emojis in source order; empty when {@code arr} is {@code null}
             */
            static List<Emoji> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Emoji>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Decodes a single text-status row from a {@link JSONObject}.
         *
         * <p>Invoked by {@link #ofArray(JSONArray)} while walking the {@code xwa2_text_status_list}
         * array.
         *
         * @param obj the JSON object to decode, possibly {@code null}
         * @return the decoded row, or {@link Optional#empty()} when {@code obj} is {@code null}
         */
        static Optional<Item> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var jid = obj.getString("jid");
            var text = obj.getString("text");
            var lastUpdateTime = obj.getLong("last_update_time");
            var ephemeralDurationSec = obj.getLong("ephemeral_duration_sec");
            var emoji = Emoji.of(obj.getJSONObject("emoji")).orElse(null);
            return Optional.of(new Item(jid, text, lastUpdateTime, ephemeralDurationSec, emoji));
        }

        /**
         * Decodes the {@code xwa2_text_status_list} array of the MEX payload.
         *
         * <p>Invoked by {@link FetchTextStatusListMexResponse#of(byte[])} to project the array nested
         * under {@code data} of the {@code <result>} payload.
         *
         * @param arr the JSON array to decode, possibly {@code null}
         * @return the decoded rows in source order; empty when {@code arr} is {@code null}
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
     * Decodes the {@code <result>} payload bytes into a {@link FetchTextStatusListMexResponse}.
     *
     * <p>The payload is projected from {@code data.xwa2_text_status_list}. A missing {@code data}
     * envelope yields {@link Optional#empty()}; a present-but-empty array yields a response with an
     * empty {@link #items()} list.
     *
     * @param json the raw {@code <result>} payload bytes
     * @return the decoded reply, or {@link Optional#empty()} when the payload does not parse as a
     *         JSON object or lacks the {@code data} envelope
     */
    private static Optional<FetchTextStatusListMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_text_status_list");
        var items = Item.ofArray(rootArr);

        return Optional.of(new FetchTextStatusListMexResponse(items));
    }
}
