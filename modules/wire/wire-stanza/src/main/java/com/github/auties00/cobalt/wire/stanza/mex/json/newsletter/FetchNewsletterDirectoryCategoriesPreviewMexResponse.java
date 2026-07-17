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
import java.util.OptionalLong;

/**
 * Parses the MEX response of the fetch-newsletter-directory-categories-preview query built by
 * {@link FetchNewsletterDirectoryCategoriesPreviewMexRequest}.
 *
 * <p>Exposes the per-category newsletter previews echoed under
 * {@code xwa2_newsletters_directory_category_preview}; each {@link Result} is one category bucket
 * carrying a few featured newsletters with their thread metadata projection used to render the
 * directory category-preview carousel.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterDirectoryCategoriesPreviewJob")
public final class FetchNewsletterDirectoryCategoriesPreviewMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the per-category newsletter preview buckets returned by the relay.
     */
    private final List<Result> result;

    /**
     * Constructs a response wrapping the parsed category-preview buckets.
     *
     * <p>Reserved for the static parser.
     *
     * @param result the parsed category-preview buckets
     */
    private FetchNewsletterDirectoryCategoriesPreviewMexResponse(List<Result> result) {
        this.result = result;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletters_directory_category_preview} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterDirectoryCategoriesPreviewMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterDirectoryCategoriesPreviewMexResponse::of);
    }

    /**
     * Returns the per-category preview buckets.
     *
     * @return the parsed buckets, empty when the relay returned none
     */
    public List<Result> result() {
        return result;
    }

    /**
     * Wraps one {@code result} entry: a single directory category and its featured newsletter
     * previews.
     *
     * <p>Carries the category enum string, the human-readable category title, and the list of
     * newsletter previews surfaced under the category in the directory's category-preview carousel.
     */
    public static final class Result {
        /**
         * Holds the category enum-string identifier.
         */
        private final String category;

        /**
         * Holds the localised category display title.
         */
        private final String categoryTitle;

        /**
         * Holds the featured newsletters in this category.
         */
        private final List<Newsletters> newsletters;

        /**
         * Constructs a category-preview wrapper from the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param category      the category enum-string identifier
         * @param categoryTitle the localised category display title
         * @param newsletters   the featured newsletters in this category
         */
        private Result(String category, String categoryTitle, List<Newsletters> newsletters) {
            this.category = category;
            this.categoryTitle = categoryTitle;
            this.newsletters = newsletters;
        }

        /**
         * Returns the category enum-string identifier.
         *
         * @return the category id, or empty when the relay omitted the field
         */
        public Optional<String> category() {
            return Optional.ofNullable(category);
        }

        /**
         * Returns the localised category display title.
         *
         * @return the category title, or empty when the relay omitted the field
         */
        public Optional<String> categoryTitle() {
            return Optional.ofNullable(categoryTitle);
        }

        /**
         * Returns the featured newsletters in this category.
         *
         * @return the parsed newsletters, empty when the relay returned none
         */
        public List<Newsletters> newsletters() {
            return newsletters;
        }

        /**
         * Wraps one featured-newsletter entry inside a category bucket.
         *
         * <p>Carries only the newsletter Jid plus a thread-metadata projection sufficient to render
         * the carousel tile (name, picture, handle, subscriber count, verification tier).
         */
        public static final class Newsletters {
            /**
             * Holds the newsletter Jid string.
             */
            private final String id;

            /**
             * Holds the dehydrated thread metadata projection used for tile rendering.
             */
            private final ThreadMetadata threadMetadata;

            /**
             * Constructs a newsletter-tile wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param id             the newsletter Jid string
             * @param threadMetadata the dehydrated thread metadata projection
             */
            private Newsletters(String id, ThreadMetadata threadMetadata) {
                this.id = id;
                this.threadMetadata = threadMetadata;
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
             * Wraps the dehydrated {@code thread_metadata} sub-object used for directory carousel
             * tiles.
             *
             * <p>Carries the subset of newsletter metadata needed to render the carousel tile:
             * creation time, invite token, public handle, subscriber count, name, description,
             * picture, and verification tier label.
             */
            public static final class ThreadMetadata {
                /**
                 * Holds the newsletter creation epoch-second.
                 */
                private final Long creationTime;

                /**
                 * Holds the newsletter public invite token.
                 */
                private final String invite;

                /**
                 * Holds the newsletter public handle.
                 */
                private final String handle;

                /**
                 * Holds the follower count.
                 */
                private final Long subscribersCount;

                /**
                 * Holds the localised name projection.
                 */
                private final Name name;

                /**
                 * Holds the localised description projection.
                 */
                private final Description description;

                /**
                 * Holds the picture reference projection.
                 */
                private final Picture picture;

                /**
                 * Holds the verification tier label.
                 */
                private final String verification;

                /**
                 * Holds the optional status-metadata projection, present only when the request set
                 * {@code fetch_status_metadata}.
                 */
                private final StatusMetadata statusMetadata;

                /**
                 * Constructs a thread-metadata wrapper from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param creationTime     the newsletter creation epoch-second
                 * @param invite           the public invite token
                 * @param handle           the public handle
                 * @param subscribersCount the follower count
                 * @param name             the localised name projection
                 * @param description      the localised description projection
                 * @param picture          the picture reference projection
                 * @param verification     the verification tier label
                 * @param statusMetadata   the optional status-metadata projection
                 */
                private ThreadMetadata(Long creationTime, String invite, String handle, Long subscribersCount, Name name, Description description, Picture picture, String verification, StatusMetadata statusMetadata) {
                    this.creationTime = creationTime;
                    this.invite = invite;
                    this.handle = handle;
                    this.subscribersCount = subscribersCount;
                    this.name = name;
                    this.description = description;
                    this.picture = picture;
                    this.verification = verification;
                    this.statusMetadata = statusMetadata;
                }

                /**
                 * Returns the newsletter creation instant.
                 *
                 * @return the creation instant, or empty when the relay omitted the field
                 */
                public Optional<Instant> creationTime() {
                    return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
                }

                /**
                 * Returns the newsletter public invite token.
                 *
                 * @return the invite token, or empty when the relay omitted the field
                 */
                public Optional<String> invite() {
                    return Optional.ofNullable(invite);
                }

                /**
                 * Returns the newsletter public handle.
                 *
                 * @return the public handle, or empty when the relay omitted the field
                 */
                public Optional<String> handle() {
                    return Optional.ofNullable(handle);
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
                 * Returns the localised name projection.
                 *
                 * @return the parsed {@link Name}, or empty when the relay omitted the field
                 */
                public Optional<Name> name() {
                    return Optional.ofNullable(name);
                }

                /**
                 * Returns the localised description projection.
                 *
                 * @return the parsed {@link Description}, or empty when the relay omitted the field
                 */
                public Optional<Description> description() {
                    return Optional.ofNullable(description);
                }

                /**
                 * Returns the picture reference projection.
                 *
                 * @return the parsed {@link Picture}, or empty when the relay omitted the field
                 */
                public Optional<Picture> picture() {
                    return Optional.ofNullable(picture);
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
                 * Returns the optional status-metadata projection.
                 *
                 * @return the parsed {@link StatusMetadata}, or empty when the relay omitted the
                 *         field (for example when the request did not set
                 *         {@code fetch_status_metadata})
                 */
                public Optional<StatusMetadata> statusMetadata() {
                    return Optional.ofNullable(statusMetadata);
                }

                /**
                 * Wraps the {@code name} versioned-text sub-object.
                 *
                 * <p>Carries the server-assigned revision id, the current text, and the epoch-second
                 * the name was last updated.
                 */
                public static final class Name {
                    /**
                     * Holds the revision identifier.
                     */
                    private final String id;

                    /**
                     * Holds the current text.
                     */
                    private final String text;

                    /**
                     * Holds the epoch-second of the last update.
                     */
                    private final Long updateTime;

                    /**
                     * Constructs a name wrapper from the parsed sub-fields.
                     *
                     * <p>Reserved for the static parser.
                     *
                     * @param id         the revision identifier
                     * @param text       the current text
                     * @param updateTime the epoch-second of the last update
                     */
                    private Name(String id, String text, Long updateTime) {
                        this.id = id;
                        this.text = text;
                        this.updateTime = updateTime;
                    }

                    /**
                     * Returns the revision identifier.
                     *
                     * @return the revision id, or empty when the relay omitted the field
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Returns the current text.
                     *
                     * @return the text, or empty when the relay omitted the field
                     */
                    public Optional<String> text() {
                        return Optional.ofNullable(text);
                    }

                    /**
                     * Returns the last-update instant.
                     *
                     * @return the update instant, or empty when the relay omitted the field
                     */
                    public Optional<Instant> updateTime() {
                        return Optional.ofNullable(updateTime).map(Instant::ofEpochSecond);
                    }

                    /**
                     * Parses a {@link Name} from the given JSON object.
                     *
                     * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                     * {@code name} entry.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed {@link Name}, or empty when {@code obj} is {@code null}
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
                     * Parses a list of {@link Name} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
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
                 * Wraps the {@code description} versioned-text sub-object.
                 *
                 * <p>Carries the same shape as {@link Name}: revision id, current text, and
                 * last-update epoch-second.
                 */
                public static final class Description {
                    /**
                     * Holds the revision identifier.
                     */
                    private final String id;

                    /**
                     * Holds the current text.
                     */
                    private final String text;

                    /**
                     * Holds the epoch-second of the last update.
                     */
                    private final Long updateTime;

                    /**
                     * Constructs a description wrapper from the parsed sub-fields.
                     *
                     * <p>Reserved for the static parser.
                     *
                     * @param id         the revision identifier
                     * @param text       the current text
                     * @param updateTime the epoch-second of the last update
                     */
                    private Description(String id, String text, Long updateTime) {
                        this.id = id;
                        this.text = text;
                        this.updateTime = updateTime;
                    }

                    /**
                     * Returns the revision identifier.
                     *
                     * @return the revision id, or empty when the relay omitted the field
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Returns the current text.
                     *
                     * @return the text, or empty when the relay omitted the field
                     */
                    public Optional<String> text() {
                        return Optional.ofNullable(text);
                    }

                    /**
                     * Returns the last-update instant.
                     *
                     * @return the update instant, or empty when the relay omitted the field
                     */
                    public Optional<Instant> updateTime() {
                        return Optional.ofNullable(updateTime).map(Instant::ofEpochSecond);
                    }

                    /**
                     * Parses a {@link Description} from the given JSON object.
                     *
                     * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                     * {@code description} entry.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed {@link Description}, or empty when {@code obj} is
                     *         {@code null}
                     */
                    static Optional<Description> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var id = obj.getString("id");
                        var text = obj.getString("text");
                        var updateTime = obj.getLong("update_time");
                        return Optional.of(new Description(id, text, updateTime));
                    }

                    /**
                     * Parses a list of {@link Description} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<Description> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<Description>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Wraps the {@code picture} reference sub-object.
                 *
                 * <p>Carries the file id, the direct-path used to fetch the picture bytes, and the
                 * picture type discriminator.
                 */
                public static final class Picture {
                    /**
                     * Holds the file identifier.
                     */
                    private final String id;

                    /**
                     * Holds the relay direct-path for the picture bytes.
                     */
                    private final String directPath;

                    /**
                     * Holds the picture type discriminator.
                     */
                    private final String type;

                    /**
                     * Constructs a picture wrapper from the parsed sub-fields.
                     *
                     * <p>Reserved for the static parser.
                     *
                     * @param id         the file identifier
                     * @param directPath the relay direct-path
                     * @param type       the picture type discriminator
                     */
                    private Picture(String id, String directPath, String type) {
                        this.id = id;
                        this.directPath = directPath;
                        this.type = type;
                    }

                    /**
                     * Returns the file identifier.
                     *
                     * @return the file id, or empty when the relay omitted the field
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Returns the relay direct-path.
                     *
                     * @return the direct path, or empty when the relay omitted the field
                     */
                    public Optional<String> directPath() {
                        return Optional.ofNullable(directPath);
                    }

                    /**
                     * Returns the picture type discriminator.
                     *
                     * @return the picture type, or empty when the relay omitted the field
                     */
                    public Optional<String> type() {
                        return Optional.ofNullable(type);
                    }

                    /**
                     * Parses a {@link Picture} from the given JSON object.
                     *
                     * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                     * {@code picture} entry.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed {@link Picture}, or empty when {@code obj} is {@code null}
                     */
                    static Optional<Picture> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var id = obj.getString("id");
                        var directPath = obj.getString("direct_path");
                        var type = obj.getString("type");
                        return Optional.of(new Picture(id, directPath, type));
                    }

                    /**
                     * Parses a list of {@link Picture} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
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
                 * Wraps the optional {@code status_metadata} sub-object.
                 *
                 * <p>Gated server-side by the {@code fetch_status_metadata} GraphQL variable; carries
                 * the server-assigned id of the newsletter's most recent status update and the
                 * epoch-second it was sent. Absent when the request did not opt in.
                 */
                public static final class StatusMetadata {
                    /**
                     * Holds the server-assigned id of the last status update.
                     */
                    private final String lastStatusServerId;

                    /**
                     * Holds the epoch-second the last status update was sent.
                     */
                    private final Long lastStatusSentTime;

                    /**
                     * Constructs a status-metadata wrapper from the parsed sub-fields.
                     *
                     * <p>Reserved for the static parser.
                     *
                     * @param lastStatusServerId the server-assigned id of the last status update
                     * @param lastStatusSentTime the epoch-second the last status update was sent
                     */
                    private StatusMetadata(String lastStatusServerId, Long lastStatusSentTime) {
                        this.lastStatusServerId = lastStatusServerId;
                        this.lastStatusSentTime = lastStatusSentTime;
                    }

                    /**
                     * Returns the server-assigned id of the last status update.
                     *
                     * @return the last-status server id, or empty when the relay omitted the field
                     */
                    public Optional<String> lastStatusServerId() {
                        return Optional.ofNullable(lastStatusServerId);
                    }

                    /**
                     * Returns the instant the last status update was sent.
                     *
                     * @return the last-status sent instant, or empty when the relay omitted the field
                     */
                    public Optional<Instant> lastStatusSentTime() {
                        return Optional.ofNullable(lastStatusSentTime).map(Instant::ofEpochSecond);
                    }

                    /**
                     * Parses a {@link StatusMetadata} from the given JSON object.
                     *
                     * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                     * {@code status_metadata} entry.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed {@link StatusMetadata}, or empty when {@code obj} is
                     *         {@code null}
                     */
                    static Optional<StatusMetadata> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var lastStatusServerId = obj.getString("last_status_server_id");
                        var lastStatusSentTime = obj.getLong("last_status_sent_time");
                        return Optional.of(new StatusMetadata(lastStatusServerId, lastStatusSentTime));
                    }

                    /**
                     * Parses a list of {@link StatusMetadata} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
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
                 * Parses a {@link ThreadMetadata} from the given JSON object.
                 *
                 * <p>Used by {@link Newsletters#of(JSONObject)} to hydrate the nested
                 * {@code thread_metadata} entry.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed {@link ThreadMetadata}, or empty when {@code obj} is
                 *         {@code null}
                 */
                static Optional<ThreadMetadata> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var creationTime = obj.getLong("creation_time");
                    var invite = obj.getString("invite");
                    var handle = obj.getString("handle");
                    var subscribersCount = obj.getLong("subscribers_count");
                    var name = Name.of(obj.getJSONObject("name")).orElse(null);
                    var description = Description.of(obj.getJSONObject("description")).orElse(null);
                    var picture = Picture.of(obj.getJSONObject("picture")).orElse(null);
                    var verification = obj.getString("verification");
                    var statusMetadata = StatusMetadata.of(obj.getJSONObject("status_metadata")).orElse(null);
                    return Optional.of(new ThreadMetadata(creationTime, invite, handle, subscribersCount, name, description, picture, verification, statusMetadata));
                }

                /**
                 * Parses a list of {@link ThreadMetadata} entries from the given JSON array.
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
             * Parses a {@link Newsletters} from the given JSON object.
             *
             * <p>Used by {@link Result#of(JSONObject)} to hydrate one entry of the
             * {@code newsletters} array.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link Newsletters}, or empty when {@code obj} is {@code null}
             */
            static Optional<Newsletters> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var id = obj.getString("id");
                var threadMetadata = ThreadMetadata.of(obj.getJSONObject("thread_metadata")).orElse(null);
                return Optional.of(new Newsletters(id, threadMetadata));
            }

            /**
             * Parses a list of {@link Newsletters} entries from the given JSON array.
             *
             * <p>Used by {@link Result#of(JSONObject)} to hydrate the {@code newsletters} array of
             * one category bucket.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<Newsletters> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Newsletters>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Result} from the given JSON object.
         *
         * <p>Used by {@link FetchNewsletterDirectoryCategoriesPreviewMexResponse#of(byte[])} to
         * hydrate one entry of the {@code result} array.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Result}, or empty when {@code obj} is {@code null}
         */
        static Optional<Result> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var category = obj.getString("category");
            var categoryTitle = obj.getString("category_title");
            var newsletters = Newsletters.ofArray(obj.getJSONArray("newsletters"));
            return Optional.of(new Result(category, categoryTitle, newsletters));
        }

        /**
         * Parses a list of {@link Result} entries from the given JSON array.
         *
         * <p>Used by {@link FetchNewsletterDirectoryCategoriesPreviewMexResponse#of(byte[])} to
         * hydrate the top-level {@code result} array.
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
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletters_directory_category_preview} root
     */
    private static Optional<FetchNewsletterDirectoryCategoriesPreviewMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletters_directory_category_preview");
        if (root == null) {
            return Optional.empty();
        }

        var result = Result.ofArray(root.getJSONArray("result"));

        return Optional.of(new FetchNewsletterDirectoryCategoriesPreviewMexResponse(result));
    }
}
