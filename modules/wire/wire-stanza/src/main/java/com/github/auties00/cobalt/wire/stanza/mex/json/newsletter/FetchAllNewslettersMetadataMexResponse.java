package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-all-newsletters-metadata query built by
 * {@link FetchAllNewslettersMetadataMexRequest}.
 *
 * <p>Exposes one {@link Item} per newsletter the local user follows, drawn from the
 * {@code data.xwa2_newsletter_subscribed} array. {@link #partition()} splits the items into active
 * and deleted buckets so callers can hydrate the active list and evict cached entries for the
 * deleted ones in a single pass.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchAllNewslettersMetadataJob")
public final class FetchAllNewslettersMetadataMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the parsed list of subscribed newsletters, ordered as returned by the relay.
     */
    private final List<Item> items;

    /**
     * Constructs a response wrapping the parsed item list.
     *
     * <p>Reserved for the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param items the parsed list of subscribed newsletters
     */
    private FetchAllNewslettersMetadataMexResponse(List<Item> items) {
        this.items = items;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_subscribed} array.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchAllNewslettersMetadataMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchAllNewslettersMetadataMexResponse::of);
    }

    /**
     * Returns the parsed list of subscribed newsletters.
     *
     * <p>The list is ordered as returned by the relay and may include items whose
     * {@code state.type} is {@code "DELETED"}; prefer {@link #partition()} to separate active from
     * deleted entries in a single pass.
     *
     * @return the parsed item list, empty when the {@code xwa2_newsletter_subscribed} array was
     *         missing or empty
     */
    public List<Item> items() {
        return items;
    }

    /**
     * Splits the items in this response into active newsletters and the subset the relay reported as
     * deleted.
     *
     * <p>Items whose {@code state.type} equals {@code "DELETED"} are routed into the deleted bucket
     * and the rest into the active bucket; {@code null} entries are skipped. Callers map the active
     * items into their domain objects and use the deleted bucket to evict cached newsletters.
     *
     * @implNote This implementation short-circuits the empty case with two {@link List#of()}
     * sentinels, and otherwise materialises the buckets as unmodifiable copies so callers cannot
     * mutate the shared response state.
     *
     * @return a {@link Partitioned} carrying the active and deleted item lists; both lists are
     *         unmodifiable
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllNewslettersMetadataJob",
            exports = "handleMexGetAllNewsletters", adaptation = WhatsAppAdaptation.ADAPTED)
    public Partitioned partition() {
        if (items.isEmpty()) {
            return new Partitioned(List.of(), List.of());
        }
        var active = new ArrayList<Item>(items.size());
        var deleted = new ArrayList<Item>();
        for (var item : items) {
            if (item == null) {
                continue;
            }
            var stateType = item.state()
                    .flatMap(Item.State::type)
                    .orElse(null);
            if ("DELETED".equals(stateType)) {
                deleted.add(item);
            } else {
                active.add(item);
            }
        }
        return new Partitioned(List.copyOf(active), List.copyOf(deleted));
    }

    /**
     * Carries the result of {@link FetchAllNewslettersMetadataMexResponse#partition()}: the active
     * newsletters surfaced to the UI and the subset the relay reported as deleted.
     *
     * <p>Both lists are returned in server-defined order; consumers typically hydrate the active
     * list and evict cached newsletter entries for the deleted ones.
     *
     * @param newsletters        the items whose {@code state.type} is not {@code "DELETED"}
     * @param deletedNewsletters the items whose {@code state.type} is {@code "DELETED"}
     */
    public record Partitioned(List<Item> newsletters, List<Item> deletedNewsletters) {
    }

    /**
     * Wraps a single subscribed-newsletter entry of the {@code xwa2_newsletter_subscribed} array.
     *
     * <p>Carries the newsletter id, the lifecycle {@link State} (used by {@link #partition()} to
     * detect deleted entries), the {@link ThreadMetadata} hydrated profile fields and the
     * {@link ViewerMetadata} per-viewer state; {@link StatusMetadata} is populated only when the
     * request set {@code fetch_status_metadata=true}.
     */
    public static final class Item {
        /**
         * Holds the newsletter Jid string.
         */
        private final String id;

        /**
         * Holds the lifecycle state object.
         */
        private final State state;

        /**
         * Holds the hydrated thread metadata.
         */
        private final ThreadMetadata threadMetadata;

        /**
         * Holds the viewer-side metadata.
         */
        private final ViewerMetadata viewerMetadata;

        /**
         * Holds the optional status metadata fragment.
         */
        private final StatusMetadata statusMetadata;

        /**
         * Constructs an item wrapping the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param id             the newsletter Jid string
         * @param state          the lifecycle state object
         * @param threadMetadata the hydrated thread metadata
         * @param viewerMetadata the viewer-side metadata
         * @param statusMetadata the optional status metadata fragment
         */
        private Item(String id, State state, ThreadMetadata threadMetadata, ViewerMetadata viewerMetadata, StatusMetadata statusMetadata) {
            this.id = id;
            this.state = state;
            this.threadMetadata = threadMetadata;
            this.viewerMetadata = viewerMetadata;
            this.statusMetadata = statusMetadata;
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
         * Returns the lifecycle state object.
         *
         * <p>Used by {@link #partition()} to detect items the relay reports as deleted.
         *
         * @return the parsed {@link State}, or empty when the relay omitted the field
         */
        public Optional<State> state() {
            return Optional.ofNullable(state);
        }

        /**
         * Returns the hydrated thread metadata.
         *
         * @return the parsed {@link ThreadMetadata}, or empty when the relay omitted the field
         */
        public Optional<ThreadMetadata> threadMetadata() {
            return Optional.ofNullable(threadMetadata);
        }

        /**
         * Returns the viewer-side metadata.
         *
         * @return the parsed {@link ViewerMetadata}, or empty when the relay omitted the field
         */
        public Optional<ViewerMetadata> viewerMetadata() {
            return Optional.ofNullable(viewerMetadata);
        }

        /**
         * Returns the optional status metadata fragment.
         *
         * <p>Populated only when the request set {@code fetch_status_metadata=true}; otherwise the
         * relay omits the fragment entirely.
         *
         * @return the parsed {@link StatusMetadata}, or empty when the relay omitted the field
         */
        public Optional<StatusMetadata> statusMetadata() {
            return Optional.ofNullable(statusMetadata);
        }

        /**
         * Wraps the {@code state} sub-object embedded in an {@code xwa2_newsletter_subscribed} item.
         *
         * <p>A {@code type} value of {@code "DELETED"} signals the relay has removed the newsletter;
         * consumers route such items into the eviction bucket via {@link #partition()}.
         */
        public static final class State {
            /**
             * Holds the textual state identifier.
             */
            private final String type;

            /**
             * Constructs a state wrapping the textual type.
             *
             * <p>Reserved for the static parser.
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
             * Parses a {@link State} from the given JSON object.
             *
             * <p>Used by {@link Item#of(JSONObject)} to hydrate the {@code state} entry.
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
         * Wraps the {@code thread_metadata} sub-object embedded in an
         * {@code xwa2_newsletter_subscribed} item.
         *
         * <p>Carries the hydrated profile (creation timestamp, name, picture, preview, description,
         * invite, handle, verification), the per-channel reaction-code {@link Settings} and the
         * optional {@link WamoSub} subscription identifier.
         */
        public static final class ThreadMetadata {
            /**
             * Holds the unix-second creation timestamp.
             */
            private final Long creationTime;

            /**
             * Holds the display-name sub-object.
             */
            private final Name name;

            /**
             * Holds the full-resolution picture sub-object.
             */
            private final Picture picture;

            /**
             * Holds the preview-resolution picture sub-object.
             */
            private final Preview preview;

            /**
             * Holds the description sub-object.
             */
            private final Description description;

            /**
             * Holds the shareable invite identifier.
             */
            private final String invite;

            /**
             * Holds the reserved handle (vanity URL slug).
             */
            private final String handle;

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
             * @param creationTime the unix-second creation timestamp
             * @param name         the display-name sub-object
             * @param picture      the picture sub-object
             * @param preview      the preview sub-object
             * @param description  the description sub-object
             * @param invite       the shareable invite identifier
             * @param handle       the reserved handle
             * @param verification the verification tier label
             * @param settings     the per-channel settings sub-object
             * @param wamoSub      the optional subscription sub-object
             */
            private ThreadMetadata(Long creationTime, Name name, Picture picture, Preview preview, Description description, String invite, String handle, String verification, Settings settings, WamoSub wamoSub) {
                this.creationTime = creationTime;
                this.name = name;
                this.picture = picture;
                this.preview = preview;
                this.description = description;
                this.invite = invite;
                this.handle = handle;
                this.verification = verification;
                this.settings = settings;
                this.wamoSub = wamoSub;
            }

            /**
             * Returns the creation timestamp.
             *
             * <p>The relay carries this value as a unix-second integer.
             *
             * @return the parsed {@link Instant}, or empty when the relay omitted the field
             */
            public Optional<Instant> creationTime() {
                return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the display-name sub-object.
             *
             * @return the parsed {@link Name}, or empty when the relay omitted the field
             */
            public Optional<Name> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the full-resolution picture sub-object.
             *
             * @return the parsed {@link Picture}, or empty when the relay omitted the field
             */
            public Optional<Picture> picture() {
                return Optional.ofNullable(picture);
            }

            /**
             * Returns the preview-resolution picture sub-object.
             *
             * @return the parsed {@link Preview}, or empty when the relay omitted the field
             */
            public Optional<Preview> preview() {
                return Optional.ofNullable(preview);
            }

            /**
             * Returns the description sub-object.
             *
             * @return the parsed {@link Description}, or empty when the relay omitted the field
             */
            public Optional<Description> description() {
                return Optional.ofNullable(description);
            }

            /**
             * Returns the shareable invite identifier.
             *
             * @return the invite token, or empty when the relay omitted the field
             */
            public Optional<String> invite() {
                return Optional.ofNullable(invite);
            }

            /**
             * Returns the reserved handle (vanity URL slug).
             *
             * @return the handle, or empty when the relay omitted the field
             */
            public Optional<String> handle() {
                return Optional.ofNullable(handle);
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
             * actually carries a paid subscription plan.
             *
             * @return the parsed {@link WamoSub}, or empty when the relay omitted the field
             */
            public Optional<WamoSub> wamoSub() {
                return Optional.ofNullable(wamoSub);
            }

            /**
             * Wraps the {@code name} sub-object embedded in {@code thread_metadata}.
             *
             * <p>Models a renameable scalar as (server-side id, text, update timestamp) so consumers
             * can detect stale local copies during sync reconciliation.
             */
            public static final class Name {
                /**
                 * Holds the server-side identifier of this name revision.
                 */
                private final String id;

                /**
                 * Holds the textual display name.
                 */
                private final String text;

                /**
                 * Holds the unix-second timestamp of the last name update.
                 */
                private final Long updateTime;

                /**
                 * Constructs a name wrapper from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param id         the server-side revision identifier
                 * @param text       the textual display name
                 * @param updateTime the unix-second update timestamp
                 */
                private Name(String id, String text, Long updateTime) {
                    this.id = id;
                    this.text = text;
                    this.updateTime = updateTime;
                }

                /**
                 * Returns the server-side identifier of this name revision.
                 *
                 * @return the revision id, or empty when the relay omitted the field
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the textual display name.
                 *
                 * @return the display name, or empty when the relay omitted the field
                 */
                public Optional<String> text() {
                    return Optional.ofNullable(text);
                }

                /**
                 * Returns the timestamp of the last name update.
                 *
                 * @return the parsed {@link Instant}, or empty when the relay omitted the field
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
             * Wraps the {@code picture} sub-object embedded in {@code thread_metadata}.
             *
             * <p>Carries (id, type, direct_path) so the client can download the full-resolution
             * newsletter avatar from the media CDN.
             */
            public static final class Picture {
                /**
                 * Holds the opaque server-side identifier of the picture revision.
                 */
                private final String id;

                /**
                 * Holds the picture type discriminator.
                 */
                private final String type;

                /**
                 * Holds the CDN direct-path used to fetch the picture bytes.
                 */
                private final String directPath;

                /**
                 * Constructs a picture wrapper from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param id         the server-side revision identifier
                 * @param type       the picture type discriminator
                 * @param directPath the CDN direct-path
                 */
                private Picture(String id, String type, String directPath) {
                    this.id = id;
                    this.type = type;
                    this.directPath = directPath;
                }

                /**
                 * Returns the server-side identifier of this picture revision.
                 *
                 * @return the revision id, or empty when the relay omitted the field
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
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
                 * Returns the CDN direct-path used to fetch the picture bytes.
                 *
                 * @return the direct-path, or empty when the relay omitted the field
                 */
                public Optional<String> directPath() {
                    return Optional.ofNullable(directPath);
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
                    var type = obj.getString("type");
                    var directPath = obj.getString("direct_path");
                    return Optional.of(new Picture(id, type, directPath));
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
             * Wraps the {@code preview} sub-object embedded in {@code thread_metadata}.
             *
             * <p>Mirrors {@link Picture} but points at the preview-resolution avatar used in
             * chat-list rendering paths.
             */
            public static final class Preview {
                /**
                 * Holds the opaque server-side identifier of the preview revision.
                 */
                private final String id;

                /**
                 * Holds the preview type discriminator.
                 */
                private final String type;

                /**
                 * Holds the CDN direct-path used to fetch the preview bytes.
                 */
                private final String directPath;

                /**
                 * Constructs a preview wrapper from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param id         the server-side revision identifier
                 * @param type       the preview type discriminator
                 * @param directPath the CDN direct-path
                 */
                private Preview(String id, String type, String directPath) {
                    this.id = id;
                    this.type = type;
                    this.directPath = directPath;
                }

                /**
                 * Returns the server-side identifier of this preview revision.
                 *
                 * @return the revision id, or empty when the relay omitted the field
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the preview type discriminator.
                 *
                 * @return the preview type, or empty when the relay omitted the field
                 */
                public Optional<String> type() {
                    return Optional.ofNullable(type);
                }

                /**
                 * Returns the CDN direct-path used to fetch the preview bytes.
                 *
                 * @return the direct-path, or empty when the relay omitted the field
                 */
                public Optional<String> directPath() {
                    return Optional.ofNullable(directPath);
                }

                /**
                 * Parses a {@link Preview} from the given JSON object.
                 *
                 * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                 * {@code preview} entry.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed {@link Preview}, or empty when {@code obj} is {@code null}
                 */
                static Optional<Preview> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var type = obj.getString("type");
                    var directPath = obj.getString("direct_path");
                    return Optional.of(new Preview(id, type, directPath));
                }

                /**
                 * Parses a list of {@link Preview} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<Preview> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<Preview>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code description} sub-object embedded in {@code thread_metadata}.
             *
             * <p>Models the (id, text, update_time) tuple so consumers can detect stale local copies
             * during sync reconciliation.
             */
            public static final class Description {
                /**
                 * Holds the server-side identifier of this description revision.
                 */
                private final String id;

                /**
                 * Holds the textual description.
                 */
                private final String text;

                /**
                 * Holds the unix-second timestamp of the last description update.
                 */
                private final Long updateTime;

                /**
                 * Constructs a description wrapper from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param id         the server-side revision identifier
                 * @param text       the textual description
                 * @param updateTime the unix-second update timestamp
                 */
                private Description(String id, String text, Long updateTime) {
                    this.id = id;
                    this.text = text;
                    this.updateTime = updateTime;
                }

                /**
                 * Returns the server-side identifier of this description revision.
                 *
                 * @return the revision id, or empty when the relay omitted the field
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the textual description.
                 *
                 * @return the description, or empty when the relay omitted the field
                 */
                public Optional<String> text() {
                    return Optional.ofNullable(text);
                }

                /**
                 * Returns the timestamp of the last description update.
                 *
                 * @return the parsed {@link Instant}, or empty when the relay omitted the field
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
                 * @return the parsed {@link Description}, or empty when {@code obj} is {@code null}
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
             * Wraps the {@code settings} sub-object embedded in {@code thread_metadata}.
             *
             * <p>Holds the per-channel reaction-code policy that gates which emojis followers may use
             * as message reactions.
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
                 * Wraps the {@code reaction_codes} sub-object embedded in {@code settings}.
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
             * Wraps the {@code wamo_sub} sub-object embedded in {@code thread_metadata}.
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
                 * <p>Used by {@link ThreadMetadata#of(JSONObject)} to hydrate the nested
                 * {@code wamo_sub} entry.
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
             * <p>Used by {@link Item#of(JSONObject)} to hydrate the {@code thread_metadata} entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link ThreadMetadata}, or empty when {@code obj} is {@code null}
             */
            static Optional<ThreadMetadata> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var creationTime = obj.getLong("creation_time");
                var name = Name.of(obj.getJSONObject("name")).orElse(null);
                var picture = Picture.of(obj.getJSONObject("picture")).orElse(null);
                var preview = Preview.of(obj.getJSONObject("preview")).orElse(null);
                var description = Description.of(obj.getJSONObject("description")).orElse(null);
                var invite = obj.getString("invite");
                var handle = obj.getString("handle");
                var verification = obj.getString("verification");
                var settings = Settings.of(obj.getJSONObject("settings")).orElse(null);
                var wamoSub = WamoSub.of(obj.getJSONObject("wamo_sub")).orElse(null);
                return Optional.of(new ThreadMetadata(creationTime, name, picture, preview, description, invite, handle, verification, settings, wamoSub));
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
         * Wraps the {@code viewer_metadata} sub-object embedded in an
         * {@code xwa2_newsletter_subscribed} item.
         *
         * <p>Records per-viewer state: opaque settings entries, the local user's role on the
         * channel, and the subscription status label.
         */
        public static final class ViewerMetadata {
            /**
             * Holds the viewer-side settings list (type/value pairs).
             */
            private final List<Settings> settings;

            /**
             * Holds the viewer's role on the newsletter.
             */
            private final String role;

            /**
             * Holds the subscription status label.
             */
            private final String wamoSubStatus;

            /**
             * Constructs a viewer-metadata wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param settings      the viewer-side settings list
             * @param role          the viewer's role on the newsletter
             * @param wamoSubStatus the subscription status label
             */
            private ViewerMetadata(List<Settings> settings, String role, String wamoSubStatus) {
                this.settings = settings;
                this.role = role;
                this.wamoSubStatus = wamoSubStatus;
            }

            /**
             * Returns the viewer-side settings.
             *
             * @return the settings list, empty when the relay omitted the field
             */
            public List<Settings> settings() {
                return settings;
            }

            /**
             * Returns the viewer's role on the newsletter.
             *
             * @return the role, or empty when the relay omitted the field
             */
            public Optional<String> role() {
                return Optional.ofNullable(role);
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
             * Wraps an entry of the viewer-side {@code settings} list.
             *
             * <p>Each entry is a (type, value) pair; the type discriminates which UI preference the
             * value applies to.
             */
            public static final class Settings {
                /**
                 * Holds the discriminator identifying which viewer preference this entry represents.
                 */
                private final String type;

                /**
                 * Holds the textual value associated with {@link #type}.
                 */
                private final String value;

                /**
                 * Constructs a settings entry from the parsed sub-fields.
                 *
                 * <p>Reserved for the static parser.
                 *
                 * @param type  the discriminator
                 * @param value the textual value
                 */
                private Settings(String type, String value) {
                    this.type = type;
                    this.value = value;
                }

                /**
                 * Returns the discriminator identifying which viewer preference this entry
                 * represents.
                 *
                 * @return the discriminator, or empty when the relay omitted the field
                 */
                public Optional<String> type() {
                    return Optional.ofNullable(type);
                }

                /**
                 * Returns the textual value associated with {@link #type()}.
                 *
                 * @return the value, or empty when the relay omitted the field
                 */
                public Optional<String> value() {
                    return Optional.ofNullable(value);
                }

                /**
                 * Parses a {@link Settings} from the given JSON object.
                 *
                 * <p>Used by {@link Settings#ofArray(JSONArray)} when hydrating the {@code settings}
                 * array under {@code viewer_metadata}.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed {@link Settings}, or empty when {@code obj} is {@code null}
                 */
                static Optional<Settings> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var type = obj.getString("type");
                    var value = obj.getString("value");
                    return Optional.of(new Settings(type, value));
                }

                /**
                 * Parses a list of {@link Settings} entries from the given JSON array.
                 *
                 * <p>Drives hydration of the {@code settings} array under {@code viewer_metadata}.
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
             * Parses a {@link ViewerMetadata} from the given JSON object.
             *
             * <p>Used by {@link Item#of(JSONObject)} to hydrate the {@code viewer_metadata} entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link ViewerMetadata}, or empty when {@code obj} is {@code null}
             */
            static Optional<ViewerMetadata> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var settings = Settings.ofArray(obj.getJSONArray("settings"));
                var role = obj.getString("role");
                var wamoSubStatus = obj.getString("wamo_sub_status");
                return Optional.of(new ViewerMetadata(settings, role, wamoSubStatus));
            }

            /**
             * Parses a list of {@link ViewerMetadata} entries from the given JSON array.
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
         * Wraps the {@code status_metadata} sub-object embedded in an
         * {@code xwa2_newsletter_subscribed} item.
         *
         * <p>Exposes the {@code last_status_server_id} and {@code last_status_sent_time} scalars
         * described by the {@code XWA2NewsletterStatusMetadata} concrete type. Populated only when
         * the request set {@code fetch_status_metadata=true}.
         */
        public static final class StatusMetadata {
            /**
             * Holds the server-side identifier of the most recent newsletter status post.
             */
            private final String lastStatusServerId;

            /**
             * Holds the unix-second timestamp of the most recent newsletter status post.
             */
            private final Long lastStatusSentTime;

            /**
             * Constructs a status-metadata wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param lastStatusServerId the server-side identifier of the last status post
             * @param lastStatusSentTime the unix-second timestamp of the last status post
             */
            private StatusMetadata(String lastStatusServerId, Long lastStatusSentTime) {
                this.lastStatusServerId = lastStatusServerId;
                this.lastStatusSentTime = lastStatusSentTime;
            }

            /**
             * Returns the server-side identifier of the most recent newsletter status post.
             *
             * @return the status server id, or empty when the relay omitted the field
             */
            public Optional<String> lastStatusServerId() {
                return Optional.ofNullable(lastStatusServerId);
            }

            /**
             * Returns the timestamp of the most recent newsletter status post.
             *
             * @return the parsed {@link Instant}, or empty when the relay omitted the field
             */
            public Optional<Instant> lastStatusSentTime() {
                return Optional.ofNullable(lastStatusSentTime).map(Instant::ofEpochSecond);
            }

            /**
             * Parses a {@link StatusMetadata} from the given JSON object.
             *
             * <p>Used by {@link Item#of(JSONObject)} to hydrate the optional {@code status_metadata}
             * entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link StatusMetadata}, or empty when {@code obj} is {@code null}
             */
            static Optional<StatusMetadata> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var lastStatusServerId = obj.getString("last_status_server_id");
                var lastStatusSentTime = obj.getLong("last_status_sent_time");
                return Optional.of(new StatusMetadata(lastStatusServerId, lastStatusSentTime));
            }
        }

        /**
         * Parses an {@link Item} from the given JSON object.
         *
         * <p>Used by {@link Item#ofArray(JSONArray)} to hydrate each entry of the
         * {@code xwa2_newsletter_subscribed} array.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link Item}, or empty when {@code obj} is {@code null}
         */
        static Optional<Item> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var state = State.of(obj.getJSONObject("state")).orElse(null);
            var threadMetadata = ThreadMetadata.of(obj.getJSONObject("thread_metadata")).orElse(null);
            var viewerMetadata = ViewerMetadata.of(obj.getJSONObject("viewer_metadata")).orElse(null);
            var statusMetadata = StatusMetadata.of(obj.getJSONObject("status_metadata")).orElse(null);
            return Optional.of(new Item(id, state, threadMetadata, viewerMetadata, statusMetadata));
        }

        /**
         * Parses a list of {@link Item} entries from the given JSON array.
         *
         * <p>Drives hydration of the {@code xwa2_newsletter_subscribed} array; entries that fail to
         * parse are silently dropped.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
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
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Reserved for the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception. Where WhatsApp Web raises a
     * server error when the relay returns a null {@code xwa2_newsletter_subscribed} array, that
     * condition surfaces here as an empty {@link Item} list rather than an exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected {@code data} root
     */
    private static Optional<FetchAllNewslettersMetadataMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var rootArr = data.getJSONArray("xwa2_newsletter_subscribed");
        var items = Item.ofArray(rootArr);

        return Optional.of(new FetchAllNewslettersMetadataMexResponse(items));
    }
}
