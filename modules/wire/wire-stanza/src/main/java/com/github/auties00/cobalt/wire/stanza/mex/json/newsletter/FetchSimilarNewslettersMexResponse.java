package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-similar-newsletters query built by
 * {@link FetchSimilarNewslettersMexRequest}.
 *
 * <p>Surfaces the relay's {@code data.xwa2_newsletters_similar.result} array as one {@link Result}
 * per similar newsletter; each result carries the newsletter Jid, a small
 * {@link Result.ThreadMetadata} block (name, thumbnail picture, verification badge) and a
 * {@link Result.State} lifecycle marker.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchSimilarNewslettersJob")
public final class FetchSimilarNewslettersMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the parsed list of similar newsletters, ordered as returned by the relay.
     */
    private final List<Result> result;

    /**
     * Constructs a response wrapping the parsed similar-newsletter list.
     *
     * @param result the parsed similar-newsletter list
     */
    private FetchSimilarNewslettersMexResponse(List<Result> result) {
        this.result = result;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletters_similar} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    public static Optional<FetchSimilarNewslettersMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchSimilarNewslettersMexResponse::of);
    }

    /**
     * Returns the parsed list of similar newsletters.
     *
     * <p>The list is ordered as returned by the relay and may be empty when no similar channels are
     * available for the seed newsletter.
     *
     * @return the parsed list, empty when the {@code result} array was missing or empty
     */
    public List<Result> result() {
        return result;
    }

    /**
     * Wraps a single similar-newsletter entry returned under
     * {@code xwa2_newsletters_similar.result}.
     *
     * <p>Carries the newsletter Jid, a slim {@link ThreadMetadata} block (name, thumbnail picture,
     * verification badge) and a lifecycle {@link State} marker; deliberately narrower than the
     * recommended-newsletter result because the similar-channels carousel UI only renders the title
     * and avatar.
     */
    public static final class Result {
        /**
         * Holds the newsletter Jid string echoed under {@code id}.
         */
        private final String id;

        /**
         * Holds the slim display-metadata block echoed under {@code thread_metadata}.
         */
        private final ThreadMetadata threadMetadata;

        /**
         * Holds the optional status-metadata projection echoed under {@code status_metadata},
         * present only when the request set {@code fetch_status_metadata}.
         */
        private final StatusMetadata statusMetadata;

        /**
         * Holds the lifecycle state marker echoed under {@code state}.
         */
        private final State state;

        /**
         * Constructs a parsed {@code result} entry.
         *
         * @param id             the newsletter Jid string
         * @param threadMetadata the slim display-metadata block
         * @param statusMetadata the optional status-metadata projection
         * @param state          the lifecycle state marker
         */
        private Result(String id, ThreadMetadata threadMetadata, StatusMetadata statusMetadata, State state) {
            this.id = id;
            this.threadMetadata = threadMetadata;
            this.statusMetadata = statusMetadata;
            this.state = state;
        }

        /**
         * Returns the newsletter Jid string.
         *
         * @return the {@code id} value, or empty when omitted
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the slim display-metadata block.
         *
         * @return the parsed {@link ThreadMetadata}, or empty when omitted
         */
        public Optional<ThreadMetadata> threadMetadata() {
            return Optional.ofNullable(threadMetadata);
        }

        /**
         * Returns the optional status-metadata projection.
         *
         * @return the parsed {@link StatusMetadata}, or empty when omitted (for example when the
         *         request did not set {@code fetch_status_metadata})
         */
        public Optional<StatusMetadata> statusMetadata() {
            return Optional.ofNullable(statusMetadata);
        }

        /**
         * Returns the lifecycle state marker.
         *
         * <p>Carries the relay-defined state-type string (for example {@code "ACTIVE"},
         * {@code "DELETED"}) when present.
         *
         * @return the parsed {@link State}, or empty when omitted
         */
        public Optional<State> state() {
            return Optional.ofNullable(state);
        }

        /**
         * Wraps the slim {@code thread_metadata} block on a similar-newsletter entry.
         *
         * <p>Carries the display fields the similar-channels carousel needs: the localised
         * {@link Name}, the thumbnail {@link Picture}, and the verification badge string.
         */
        public static final class ThreadMetadata {
            /**
             * Holds the localised display-name block.
             */
            private final Name name;

            /**
             * Holds the thumbnail-image block.
             */
            private final Picture picture;

            /**
             * Holds the verification-badge string.
             */
            private final String verification;

            /**
             * Constructs a parsed {@code thread_metadata} value.
             *
             * @param name         the localised display-name block
             * @param picture      the thumbnail-image block
             * @param verification the verification-badge string
             */
            private ThreadMetadata(Name name, Picture picture, String verification) {
                this.name = name;
                this.picture = picture;
                this.verification = verification;
            }

            /**
             * Returns the localised display-name block.
             *
             * @return the parsed {@link Name}, or empty when omitted
             */
            public Optional<Name> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the thumbnail-image block.
             *
             * @return the parsed {@link Picture}, or empty when omitted
             */
            public Optional<Picture> picture() {
                return Optional.ofNullable(picture);
            }

            /**
             * Returns the verification-badge string.
             *
             * <p>Carries the relay-defined badge string (for example {@code "VERIFIED"},
             * {@code "UNVERIFIED"}) when present.
             *
             * @return the {@code verification} value, or empty when omitted
             */
            public Optional<String> verification() {
                return Optional.ofNullable(verification);
            }

            /**
             * Wraps the localised {@code name} block on a similar-newsletter entry.
             *
             * <p>Carries the localisation identifier, the display text, and the last-update timestamp
             * the relay uses to drive cache invalidation.
             */
            public static final class Name {
                /**
                 * Holds the localisation identifier.
                 */
                private final String id;

                /**
                 * Holds the display text.
                 */
                private final String text;

                /**
                 * Holds the last-update timestamp in epoch seconds.
                 */
                private final Long updateTime;

                /**
                 * Constructs a parsed {@code name} value.
                 *
                 * @param id         the localisation identifier
                 * @param text       the display text
                 * @param updateTime the last-update epoch seconds
                 */
                private Name(String id, String text, Long updateTime) {
                    this.id = id;
                    this.text = text;
                    this.updateTime = updateTime;
                }

                /**
                 * Returns the localisation identifier.
                 *
                 * @return the {@code id} value, or empty when omitted
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the display text.
                 *
                 * @return the {@code text} value, or empty when omitted
                 */
                public Optional<String> text() {
                    return Optional.ofNullable(text);
                }

                /**
                 * Returns the last-update timestamp.
                 *
                 * <p>The underlying value is the wire-level epoch-second integer remapped to an
                 * {@link Instant}.
                 *
                 * @return the {@code update_time} as an {@link Instant}, or empty when omitted
                 */
                public Optional<Instant> updateTime() {
                    return Optional.ofNullable(updateTime).map(Instant::ofEpochSecond);
                }

                /**
                 * Parses a {@code name} fragment from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed value, or empty when {@code obj} is {@code null}
                 */
                static Optional<Name> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var text = obj.getString("text");
                    var updateTime = obj.getLong("update_time");
                    return Optional.of(new Name(id, text, updateTime));
                }

                /**
                 * Parses every {@code name} fragment in the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed values, empty when {@code arr} is {@code null}
                 */
                static List<Name> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<Name>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code picture} thumbnail block on a similar-newsletter entry.
             *
             * <p>Carries the relay-issued media handle, the media type, and the direct download path
             * the client uses to fetch the thumbnail bytes.
             */
            public static final class Picture {
                /**
                 * Holds the media handle.
                 */
                private final String id;

                /**
                 * Holds the media type marker.
                 */
                private final String type;

                /**
                 * Holds the direct download path.
                 */
                private final String directPath;

                /**
                 * Constructs a parsed {@code picture} value.
                 *
                 * @param id         the media handle
                 * @param type       the media type marker
                 * @param directPath the direct download path
                 */
                private Picture(String id, String type, String directPath) {
                    this.id = id;
                    this.type = type;
                    this.directPath = directPath;
                }

                /**
                 * Returns the media handle.
                 *
                 * @return the {@code id} value, or empty when omitted
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the media-type marker.
                 *
                 * @return the {@code type} value, or empty when omitted
                 */
                public Optional<String> type() {
                    return Optional.ofNullable(type);
                }

                /**
                 * Returns the direct download path.
                 *
                 * <p>Carries the server-relative path the client uses to fetch the thumbnail bytes
                 * when present.
                 *
                 * @return the {@code direct_path} value, or empty when omitted
                 */
                public Optional<String> directPath() {
                    return Optional.ofNullable(directPath);
                }

                /**
                 * Parses a {@code picture} fragment from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed value, or empty when {@code obj} is {@code null}
                 */
                static Optional<Picture> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var type = obj.getString("type");
                    var directPath = obj.getString("direct_path");
                    return Optional.of(new Picture(id, type, directPath));
                }

                /**
                 * Parses every {@code picture} fragment in the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed values, empty when {@code arr} is {@code null}
                 */
                static List<Picture> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<Picture>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Parses a {@code thread_metadata} fragment from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
             */
            static Optional<ThreadMetadata> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var name = Name.of(obj.getJSONObject("name")).orElse(null);
                var picture = Picture.of(obj.getJSONObject("picture")).orElse(null);
                var verification = obj.getString("verification");
                return Optional.of(new ThreadMetadata(name, picture, verification));
            }

            /**
             * Parses every {@code thread_metadata} fragment in the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
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
         * Wraps the lifecycle {@code state} marker on a similar-newsletter entry.
         *
         * <p>Carries the relay-defined {@code type} string (for example {@code "ACTIVE"},
         * {@code "DELETED"}, {@code "SUSPENDED"}) that gates whether the carousel UI should surface
         * the entry.
         */
        public static final class State {
            /**
             * Holds the relay-defined state-type string.
             */
            private final String type;

            /**
             * Constructs a parsed {@code state} value.
             *
             * @param type the state-type string
             */
            private State(String type) {
                this.type = type;
            }

            /**
             * Returns the state-type string.
             *
             * @return the {@code type} value, or empty when omitted
             */
            public Optional<String> type() {
                return Optional.ofNullable(type);
            }

            /**
             * Parses a {@code state} fragment from the given JSON object.
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
         * Wraps the optional {@code status_metadata} block on a similar-newsletter entry.
         *
         * <p>Gated server-side by the {@code fetch_status_metadata} GraphQL variable; carries the
         * server-assigned id of the newsletter's most recent status update. Absent when the request
         * did not opt in.
         *
         * @implNote This implementation models only {@code last_status_server_id} because the
         * similar-newsletters selection set omits the {@code last_status_sent_time} field the
         * directory-list and search selection sets carry.
         */
        public static final class StatusMetadata {
            /**
             * Holds the server-assigned id of the last status update.
             */
            private final String lastStatusServerId;

            /**
             * Constructs a parsed {@code status_metadata} value.
             *
             * @param lastStatusServerId the server-assigned id of the last status update
             */
            private StatusMetadata(String lastStatusServerId) {
                this.lastStatusServerId = lastStatusServerId;
            }

            /**
             * Returns the server-assigned id of the last status update.
             *
             * @return the {@code last_status_server_id} value, or empty when omitted
             */
            public Optional<String> lastStatusServerId() {
                return Optional.ofNullable(lastStatusServerId);
            }

            /**
             * Parses a {@code status_metadata} fragment from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
             */
            static Optional<StatusMetadata> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var lastStatusServerId = obj.getString("last_status_server_id");
                return Optional.of(new StatusMetadata(lastStatusServerId));
            }

            /**
             * Parses every {@code status_metadata} fragment in the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
             */
            static List<StatusMetadata> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<StatusMetadata>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a single {@code result} entry from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed value, or empty when {@code obj} is {@code null}
         */
        static Optional<Result> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var threadMetadata = ThreadMetadata.of(obj.getJSONObject("thread_metadata")).orElse(null);
            var statusMetadata = StatusMetadata.of(obj.getJSONObject("status_metadata")).orElse(null);
            var state = State.of(obj.getJSONObject("state")).orElse(null);
            return Optional.of(new Result(id, threadMetadata, statusMetadata, state));
        }

        /**
         * Parses every {@code result} entry in the given JSON array.
         *
         * <p>Materialises the {@code xwa2_newsletters_similar.result} array into a fresh
         * {@link List}; returns {@link List#of()} when {@code arr} is {@code null} so an absent or
         * empty server response surfaces as an empty list.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed values, empty when {@code arr} is {@code null}
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
     *         {@code data.xwa2_newsletters_similar} root
     */
    private static Optional<FetchSimilarNewslettersMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletters_similar");
        if (root == null) {
            return Optional.empty();
        }

        var result = Result.ofArray(root.getJSONArray("result"));

        return Optional.of(new FetchSimilarNewslettersMexResponse(result));
    }
}
