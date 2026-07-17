package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-insights query built by
 * {@link FetchNewsletterInsightsMexRequest}.
 *
 * <p>Exposes the admin insights payload echoed under {@code xwa2_newsletter_admin_insights}. Each
 * {@link Result} carries one metric id and the per-time-bucket values that drive the admin
 * dashboard charts, plus a top-level {@code last_update_time} and {@code metrics_status} freshness
 * indicator.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterInsightsJob")
public final class FetchNewsletterInsightsMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string echoed back by the server.
     */
    private final String newsletterId;

    /**
     * Holds the optional admin insights state sub-object.
     */
    private final State state;

    /**
     * Holds the epoch-second of the last insights aggregation pass.
     */
    private final Long lastUpdateTime;

    /**
     * Holds the insights freshness status label.
     */
    private final String metricsStatus;

    /**
     * Holds the per-metric value series.
     */
    private final List<Result> result;

    /**
     * Constructs a response wrapping the parsed insights payload.
     *
     * @param newsletterId   the newsletter Jid string
     * @param state          the optional admin insights state sub-object
     * @param lastUpdateTime the epoch-second of the last aggregation
     * @param metricsStatus  the insights freshness status label
     * @param result         the per-metric value series
     */
    private FetchNewsletterInsightsMexResponse(String newsletterId, State state, Long lastUpdateTime, String metricsStatus, List<Result> result) {
        this.newsletterId = newsletterId;
        this.state = state;
        this.lastUpdateTime = lastUpdateTime;
        this.metricsStatus = metricsStatus;
        this.result = result;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin_insights} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterInsightsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterInsightsMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed back by the server.
     *
     * @return the Jid string, or empty when the relay omitted the field
     */
    public Optional<String> newsletterId() {
        return Optional.ofNullable(newsletterId);
    }

    /**
     * Returns the admin insights state sub-object.
     *
     * @return the parsed {@link State}, or empty when the relay omitted the field
     */
    public Optional<State> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns the last-aggregation instant.
     *
     * <p>When the relay omits this field WhatsApp Web falls back to the current wall-clock; Cobalt
     * callers receive {@link Optional#empty()} and can apply the same fallback or render the chart
     * as stale.
     *
     * @return the instant, or empty when the relay omitted the field
     */
    public Optional<Instant> lastUpdateTime() {
        return Optional.ofNullable(lastUpdateTime).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the insights freshness status label.
     *
     * <p>WhatsApp Web maps the label onto the per-card status badges shown in the dashboard.
     *
     * @return the status label, or empty when the relay omitted the field
     */
    public Optional<String> metricsStatus() {
        return Optional.ofNullable(metricsStatus);
    }

    /**
     * Returns the per-metric value series.
     *
     * @return the parsed series, empty when the relay returned none
     */
    public List<Result> result() {
        return result;
    }

    /**
     * Wraps the {@code state} sub-object.
     *
     * <p>Carries a single {@code type} label describing the overall insights state.
     */
    public static final class State {
        /**
         * Holds the state type label.
         */
        private final String type;

        /**
         * Constructs a state wrapper from the parsed sub-fields.
         *
         * @param type the state type label
         */
        private State(String type) {
            this.type = type;
        }

        /**
         * Returns the state type label.
         *
         * @return the type label, or empty when the relay omitted the field
         */
        public Optional<String> type() {
            return Optional.ofNullable(type);
        }

        /**
         * Parses a {@link State} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<State> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var type = obj.getString("type");
            return Optional.of(new State(type));
        }

        /**
         * Parses a list of {@link State} entries from the given JSON array.
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
     * Wraps one entry of the {@code result} array: one metric id and its per-bucket values.
     *
     * <p>The {@code id} is the metric identifier the request asked for; the {@link Values} list
     * carries one point per time bucket, often broken down by {@code country} and {@code role}.
     */
    public static final class Result {
        /**
         * Holds the metric identifier.
         */
        private final String id;

        /**
         * Holds the per-bucket metric values.
         */
        private final List<Values> values;

        /**
         * Constructs a result wrapper from the parsed sub-fields.
         *
         * @param id     the metric identifier
         * @param values the per-bucket metric values
         */
        private Result(String id, List<Values> values) {
            this.id = id;
            this.values = values;
        }

        /**
         * Returns the metric identifier.
         *
         * @return the metric id, or empty when the relay omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the per-bucket metric values.
         *
         * @return the parsed values, empty when the relay returned none
         */
        public List<Values> values() {
            return values;
        }

        /**
         * Wraps one bucket inside a metric's {@code values} array.
         *
         * <p>Each bucket carries a numeric {@code value} held as a string for 64-bit precision, an
         * optional country breakdown, an optional role breakdown, and the bucket timestamp.
         */
        public static final class Values {
            /**
             * Holds the numeric value as a string.
             */
            private final String value;

            /**
             * Holds the country-breakdown label.
             */
            private final String country;

            /**
             * Holds the role-breakdown label.
             */
            private final String role;

            /**
             * Holds the bucket epoch-second.
             */
            private final Long timestamp;

            /**
             * Constructs a values wrapper from the parsed sub-fields.
             *
             * @param value     the numeric value as a string
             * @param country   the country-breakdown label
             * @param role      the role-breakdown label
             * @param timestamp the bucket epoch-second
             */
            private Values(String value, String country, String role, Long timestamp) {
                this.value = value;
                this.country = country;
                this.role = role;
                this.timestamp = timestamp;
            }

            /**
             * Returns the numeric value as a string.
             *
             * @return the value, or empty when the relay omitted the field
             */
            public Optional<String> value() {
                return Optional.ofNullable(value);
            }

            /**
             * Returns the country-breakdown label.
             *
             * @return the country label, or empty when the relay omitted the field
             */
            public Optional<String> country() {
                return Optional.ofNullable(country);
            }

            /**
             * Returns the role-breakdown label.
             *
             * @return the role label, or empty when the relay omitted the field
             */
            public Optional<String> role() {
                return Optional.ofNullable(role);
            }

            /**
             * Returns the bucket instant.
             *
             * @return the instant, or empty when the relay omitted the field
             */
            public Optional<Instant> timestamp() {
                return Optional.ofNullable(timestamp).map(Instant::ofEpochSecond);
            }

            /**
             * Parses a {@link Values} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<Values> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var value = obj.getString("value");
                var country = obj.getString("country");
                var role = obj.getString("role");
                var timestamp = obj.getLong("timestamp");
                return Optional.of(new Values(value, country, role, timestamp));
            }

            /**
             * Parses a list of {@link Values} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<Values> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Values>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Result} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Result> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var values = Values.ofArray(obj.getJSONArray("values"));
            return Optional.of(new Result(id, values));
        }

        /**
         * Parses a list of {@link Result} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Result> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Result>(arr.size());
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
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_admin_insights} root
     */
    private static Optional<FetchNewsletterInsightsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin_insights");
        if (root == null) {
            return Optional.empty();
        }

        var newsletterId = root.getString("newsletter_id");
        var state = State.of(root.getJSONObject("state")).orElse(null);
        var lastUpdateTime = root.getLong("last_update_time");
        var metricsStatus = root.getString("metrics_status");
        var result = Result.ofArray(root.getJSONArray("result"));

        return Optional.of(new FetchNewsletterInsightsMexResponse(newsletterId, state, lastUpdateTime, metricsStatus, result));
    }
}
