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
import java.util.OptionalLong;

/**
 * Parses the MEX response of the fetch-newsletter-dehydrated query built by
 * {@link FetchNewsletterDehydratedMexRequest}.
 *
 * <p>Exposes the lightweight newsletter projection echoed under {@code xwa2_newsletter}: the
 * newsletter id, the verification-plus-subcount portion of {@link ThreadMetadata} and the paid
 * subscription portion of {@link ViewerMetadata}. The shape is a strict subset of the full
 * {@code xwa2_newsletter} schema so the relay can serve it cheaply on chat-list rendering and
 * follow-suggestion paths.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDehydratedJob")
public final class FetchNewsletterDehydratedMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string.
     */
    private final String id;

    /**
     * Holds the dehydrated thread metadata projection.
     */
    private final ThreadMetadata threadMetadata;

    /**
     * Holds the dehydrated viewer-side metadata projection.
     */
    private final ViewerMetadata viewerMetadata;

    /**
     * Constructs a response wrapping the parsed top-level fields.
     *
     * <p>Reserved for the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param id             the newsletter Jid string
     * @param threadMetadata the dehydrated thread metadata
     * @param viewerMetadata the dehydrated viewer-side metadata
     */
    private FetchNewsletterDehydratedMexResponse(String id, ThreadMetadata threadMetadata, ViewerMetadata viewerMetadata) {
        this.id = id;
        this.threadMetadata = threadMetadata;
        this.viewerMetadata = viewerMetadata;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterDehydratedMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterDehydratedMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string.
     *
     * @return the newsletter id, or empty when the relay omitted the field
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the dehydrated thread metadata projection.
     *
     * @return the parsed {@link ThreadMetadata}, or empty when the relay omitted the field
     */
    public Optional<ThreadMetadata> threadMetadata() {
        return Optional.ofNullable(threadMetadata);
    }

    /**
     * Returns the dehydrated viewer-side metadata projection.
     *
     * @return the parsed {@link ViewerMetadata}, or empty when the relay omitted the field
     */
    public Optional<ViewerMetadata> viewerMetadata() {
        return Optional.ofNullable(viewerMetadata);
    }

    /**
     * Wraps the dehydrated {@code thread_metadata} sub-object.
     *
     * <p>Carries only the subset of thread metadata the dehydrated query pulls: subscriber count,
     * verification tier, per-channel {@link Settings} and the optional {@link WamoSub} subscription
     * identifier.
     */
    public static final class ThreadMetadata {
        /**
         * Holds the follower count.
         */
        private final Long subscribersCount;

        /**
         * Holds the verification tier label.
         */
        private final String verification;

        /**
         * Holds the per-channel settings sub-object.
         */
        private final Settings settings;

        /**
         * Holds the optional paid-subscription identifier sub-object.
         */
        private final WamoSub wamoSub;

        /**
         * Constructs a thread-metadata wrapper from the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param subscribersCount the follower count
         * @param verification     the verification tier label
         * @param settings         the per-channel settings sub-object
         * @param wamoSub          the optional subscription sub-object
         */
        private ThreadMetadata(Long subscribersCount, String verification, Settings settings, WamoSub wamoSub) {
            this.subscribersCount = subscribersCount;
            this.verification = verification;
            this.settings = settings;
            this.wamoSub = wamoSub;
        }

        /**
         * Returns the follower count.
         *
         * @return the follower count, or empty when the relay omitted the field
         */
        public OptionalLong subscribersCount() {
            return subscribersCount != null ? OptionalLong.of(subscribersCount) : OptionalLong.empty();
        }

        /**
         * Returns the verification tier label.
         *
         * @return the verification label, or empty when the relay omitted the field
         */
        public Optional<String> verification() {
            return Optional.ofNullable(verification);
        }

        /**
         * Returns the per-channel settings sub-object.
         *
         * @return the parsed {@link Settings}, or empty when the relay omitted the field
         */
        public Optional<Settings> settings() {
            return Optional.ofNullable(settings);
        }

        /**
         * Returns the optional paid-subscription sub-object.
         *
         * <p>Populated only when the request set {@code fetch_wamo_sub=true} and the newsletter
         * carries a paid subscription plan.
         *
         * @return the parsed {@link WamoSub}, or empty when the relay omitted the field
         */
        public Optional<WamoSub> wamoSub() {
            return Optional.ofNullable(wamoSub);
        }

        /**
         * Wraps the {@code settings} sub-object embedded in the dehydrated {@code thread_metadata}.
         *
         * <p>Holds the per-channel reaction-code policy that gates which emojis followers may use as
         * message reactions.
         */
        public static final class Settings {
            /**
             * Holds the reaction-code policy sub-object.
             */
            private final ReactionCodes reactionCodes;

            /**
             * Constructs a settings wrapper from the parsed sub-field.
             *
             * <p>Reserved for the static parser.
             *
             * @param reactionCodes the reaction-code policy sub-object
             */
            private Settings(ReactionCodes reactionCodes) {
                this.reactionCodes = reactionCodes;
            }

            /**
             * Returns the reaction-code policy sub-object.
             *
             * @return the parsed {@link ReactionCodes}, or empty when the relay omitted the field
             */
            public Optional<ReactionCodes> reactionCodes() {
                return Optional.ofNullable(reactionCodes);
            }

            /**
             * Wraps the {@code reaction_codes} sub-object.
             *
             * <p>Exposes the policy value verbatim; consumers map it to the corresponding
             * reaction-codes enum themselves.
             */
            public static final class ReactionCodes {
                /**
                 * Holds the textual policy value.
                 */
                private final String value;

                /**
                 * Constructs a reaction-codes wrapper from the parsed sub-field.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param value the textual policy value
                 */
                private ReactionCodes(String value) {
                    this.value = value;
                }

                /**
                 * Returns the textual policy value.
                 *
                 * @return the value, or empty when the relay omitted the field
                 */
                public Optional<String> value() {
                    return Optional.ofNullable(value);
                }

                /**
                 * Parses a {@link ReactionCodes} from the given JSON object.
                 *
                 * <p>Used by {@link Settings#of(JSONObject)} to hydrate the nested
                 * {@code reaction_codes} entry.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed {@link ReactionCodes}, or empty when {@code obj} is
                 *         {@code null}
                 */
                static Optional<ReactionCodes> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var value = obj.getString("value");
                    return Optional.of(new ReactionCodes(value));
                }

                /**
                 * Parses a list of {@link ReactionCodes} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<ReactionCodes> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<ReactionCodes>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses a {@link Settings} from the given JSON object.
             *
             * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
             * {@code settings} entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link Settings}, or empty when {@code obj} is {@code null}
             */
            static Optional<Settings> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var reactionCodes = ReactionCodes.of(obj.getJSONObject("reaction_codes")).orElse(null);
                return Optional.of(new Settings(reactionCodes));
            }

            /**
             * Parses a list of {@link Settings} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<Settings> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Settings>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Wraps the {@code wamo_sub} sub-object embedded in the dehydrated {@code thread_metadata}.
         *
         * <p>Carries the monetisation paid-subscription plan identifier; populated only when the
         * request set {@code fetch_wamo_sub=true}.
         */
        public static final class WamoSub {
            /**
             * Holds the subscription plan identifier.
             */
            private final String planId;

            /**
             * Constructs a subscription wrapper from the parsed sub-field.
             *
             * <p>Reserved for the static parser.
             *
             * @param planId the subscription plan identifier
             */
            private WamoSub(String planId) {
                this.planId = planId;
            }

            /**
             * Returns the subscription plan identifier.
             *
             * @return the plan id, or empty when the relay omitted the field
             */
            public Optional<String> planId() {
                return Optional.ofNullable(planId);
            }

            /**
             * Parses a {@link WamoSub} from the given JSON object.
             *
             * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested {@code wamo_sub}
             * entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link WamoSub}, or empty when {@code obj} is {@code null}
             */
            static Optional<WamoSub> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var planId = obj.getString("plan_id");
                return Optional.of(new WamoSub(planId));
            }

            /**
             * Parses a list of {@link WamoSub} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<WamoSub> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<WamoSub>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link ThreadMetadata} from the given JSON object.
         *
         * <p>Used by {@link FetchNewsletterDehydratedMexResponse#of(byte[])} to hydrate the
         * {@code thread_metadata} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ThreadMetadata}, or empty when {@code obj} is {@code null}
         */
        static Optional<ThreadMetadata> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var subscribersCount = obj.getLong("subscribers_count");
            var verification = obj.getString("verification");
            var settings = Settings.of(obj.getJSONObject("settings")).orElse(null);
            var wamoSub = WamoSub.of(obj.getJSONObject("wamo_sub")).orElse(null);
            return Optional.of(new ThreadMetadata(subscribersCount, verification, settings, wamoSub));
        }

        /**
         * Parses a list of {@link ThreadMetadata} entries from the given JSON array.
         *
         * <p>The dehydrated envelope does not carry a {@code thread_metadata} array; this overload
         * exists for symmetry with the other sub-object parsers.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<ThreadMetadata> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ThreadMetadata>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps the dehydrated {@code viewer_metadata} sub-object.
     *
     * <p>Carries only the subscription status label; the full {@code viewer_metadata} shape with
     * role and settings is not projected by the dehydrated query.
     */
    public static final class ViewerMetadata {
        /**
         * Holds the subscription status label.
         */
        private final String wamoSubStatus;

        /**
         * Constructs a viewer-metadata wrapper from the parsed sub-field.
         *
         * <p>Reserved for the static parser.
         *
         * @param wamoSubStatus the subscription status label
         */
        private ViewerMetadata(String wamoSubStatus) {
            this.wamoSubStatus = wamoSubStatus;
        }

        /**
         * Returns the subscription status label.
         *
         * @return the status label, or empty when the relay omitted the field
         */
        public Optional<String> wamoSubStatus() {
            return Optional.ofNullable(wamoSubStatus);
        }

        /**
         * Parses a {@link ViewerMetadata} from the given JSON object.
         *
         * <p>Used by {@link FetchNewsletterDehydratedMexResponse#of(byte[])} to hydrate the
         * {@code viewer_metadata} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ViewerMetadata}, or empty when {@code obj} is {@code null}
         */
        static Optional<ViewerMetadata> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var wamoSubStatus = obj.getString("wamo_sub_status");
            return Optional.of(new ViewerMetadata(wamoSubStatus));
        }

        /**
         * Parses a list of {@link ViewerMetadata} entries from the given JSON array.
         *
         * <p>The dehydrated envelope does not carry a {@code viewer_metadata} array; this overload
         * exists for symmetry with the other sub-object parsers.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<ViewerMetadata> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<ViewerMetadata>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter} root
     */
    private static Optional<FetchNewsletterDehydratedMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var threadMetadata = ThreadMetadata.of(root.getJSONObject("thread_metadata")).orElse(null);
        var viewerMetadata = ViewerMetadata.of(root.getJSONObject("viewer_metadata")).orElse(null);

        return Optional.of(new FetchNewsletterDehydratedMexResponse(id, threadMetadata, viewerMetadata));
    }
}
