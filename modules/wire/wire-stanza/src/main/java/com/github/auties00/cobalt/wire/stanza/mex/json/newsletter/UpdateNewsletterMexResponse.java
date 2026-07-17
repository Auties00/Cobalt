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
 * Parses the MEX response of the update-newsletter mutation built by
 * {@link UpdateNewsletterMexRequest}.
 *
 * <p>Surfaces the relay's {@code data.xwa2_newsletter_update} payload as the newsletter Jid, a
 * {@link State} lifecycle marker, and a full {@link ThreadMetadata} block carrying the updated name,
 * description, picture, preview thumbnail, invite URL, vanity handle, verification badge, creation
 * timestamp, and reaction-codes setting. Callers feed the {@link ThreadMetadata} into their local
 * newsletter cache to apply the relay-confirmed edits.
 */
@WhatsAppWebModule(moduleName = "WAWebMexUpdateNewsletterJob")
public final class UpdateNewsletterMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newsletter Jid string echoed under {@code id}.
     */
    private final String id;

    /**
     * Holds the lifecycle state marker echoed under {@code state}.
     */
    private final State state;

    /**
     * Holds the post-edit display-metadata block echoed under {@code thread_metadata}.
     */
    private final ThreadMetadata threadMetadata;

    /**
     * Constructs a response wrapping the parsed top-level fields.
     *
     * <p>Invoked only by the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param id             the newsletter Jid string echoed by the relay
     * @param state          the lifecycle state marker
     * @param threadMetadata the post-edit display-metadata block
     */
    private UpdateNewsletterMexResponse(String id, State state, ThreadMetadata threadMetadata) {
        this.id = id;
        this.state = state;
        this.threadMetadata = threadMetadata;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_update} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<UpdateNewsletterMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(UpdateNewsletterMexResponse::of);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * <p>Empty when the GraphQL envelope omits {@code id}; otherwise carries the same Jid string
     * sent in {@link UpdateNewsletterMexRequest}.
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
     * Returns the post-edit display-metadata block.
     *
     * <p>Empty when the GraphQL envelope omits {@code thread_metadata}; callers feed the populated
     * block into their newsletter cache to apply the relay-confirmed edits.
     *
     * @return the parsed {@link ThreadMetadata}, or empty when omitted
     */
    public Optional<ThreadMetadata> threadMetadata() {
        return Optional.ofNullable(threadMetadata);
    }

    /**
     * Models the lifecycle {@code state} marker on an update-mutation result.
     *
     * <p>Carries the relay-defined state-type string the server attached after applying the edit;
     * callers use it to detect lifecycle transitions outside the normal edit flow.
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
     * Models the {@code thread_metadata} block on the post-edit mutation result.
     *
     * <p>Carries the full display surface for the local cache: localised {@link Name} and
     * {@link Description} blocks, the avatar {@link Picture}, the {@link Preview} thumbnail, the
     * canonical invite URL, the vanity handle, the verification badge, the creation timestamp, and
     * the per-newsletter {@link Settings} container.
     */
    public static final class ThreadMetadata {
        /**
         * Holds the localised display-name block.
         */
        private final Name name;

        /**
         * Holds the localised description block.
         */
        private final Description description;

        /**
         * Holds the avatar-image block.
         */
        private final Picture picture;

        /**
         * Holds the preview-thumbnail block.
         */
        private final Preview preview;

        /**
         * Holds the canonical invite URL string.
         */
        private final String invite;

        /**
         * Holds the vanity-handle string.
         */
        private final String handle;

        /**
         * Holds the verification-badge string.
         */
        private final String verification;

        /**
         * Holds the newsletter-creation timestamp in epoch seconds.
         */
        private final Long creationTime;

        /**
         * Holds the per-newsletter settings container.
         */
        private final Settings settings;

        /**
         * Constructs a parsed {@code thread_metadata} value.
         *
         * <p>Invoked only by {@link #of(JSONObject)}.
         *
         * @param name         the localised display-name block
         * @param description  the localised description block
         * @param picture      the avatar-image block
         * @param preview      the preview-thumbnail block
         * @param invite       the canonical invite URL
         * @param handle       the vanity-handle string
         * @param verification the verification-badge string
         * @param creationTime the newsletter-creation epoch seconds
         * @param settings     the per-newsletter settings container
         */
        private ThreadMetadata(Name name, Description description, Picture picture, Preview preview, String invite, String handle, String verification, Long creationTime, Settings settings) {
            this.name = name;
            this.description = description;
            this.picture = picture;
            this.preview = preview;
            this.invite = invite;
            this.handle = handle;
            this.verification = verification;
            this.creationTime = creationTime;
            this.settings = settings;
        }

        /**
         * Returns the localised display-name block.
         *
         * <p>Empty when the GraphQL envelope omits {@code name}.
         *
         * @return the parsed {@link Name}, or empty when omitted
         */
        public Optional<Name> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the localised description block.
         *
         * <p>Empty when the GraphQL envelope omits {@code description}.
         *
         * @return the parsed {@link Description}, or empty when omitted
         */
        public Optional<Description> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the avatar-image block.
         *
         * <p>Empty when the GraphQL envelope omits {@code picture}.
         *
         * @return the parsed {@link Picture}, or empty when omitted
         */
        public Optional<Picture> picture() {
            return Optional.ofNullable(picture);
        }

        /**
         * Returns the preview-thumbnail block.
         *
         * <p>Empty when the GraphQL envelope omits {@code preview}.
         *
         * @return the parsed {@link Preview}, or empty when omitted
         */
        public Optional<Preview> preview() {
            return Optional.ofNullable(preview);
        }

        /**
         * Returns the canonical invite URL.
         *
         * <p>Empty when the GraphQL envelope omits {@code invite}.
         *
         * @return the {@code invite} value, or empty when omitted
         */
        public Optional<String> invite() {
            return Optional.ofNullable(invite);
        }

        /**
         * Returns the vanity-handle string.
         *
         * <p>Empty when the GraphQL envelope omits {@code handle}.
         *
         * @return the {@code handle} value, or empty when omitted
         */
        public Optional<String> handle() {
            return Optional.ofNullable(handle);
        }

        /**
         * Returns the verification-badge string.
         *
         * <p>Empty when the GraphQL envelope omits {@code verification}; otherwise carries the
         * relay-defined badge string, for example {@code "VERIFIED"} or {@code "UNVERIFIED"}.
         *
         * @return the {@code verification} value, or empty when omitted
         */
        public Optional<String> verification() {
            return Optional.ofNullable(verification);
        }

        /**
         * Returns the moment the newsletter was created.
         *
         * <p>Empty when the GraphQL envelope omits {@code creation_time}; the wire-level epoch-second
         * integer is remapped to an {@link Instant}.
         *
         * @return the {@code creation_time} as an {@link Instant}, or empty when omitted
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the per-newsletter settings container.
         *
         * <p>Empty when the GraphQL envelope omits {@code settings}; in practice the relay only
         * echoes the {@code reaction_codes} sub-key updated by the mutation.
         *
         * @return the parsed {@link Settings}, or empty when omitted
         */
        public Optional<Settings> settings() {
            return Optional.ofNullable(settings);
        }

        /**
         * Models the localised {@code name} block on the post-edit metadata.
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
             * <p>Invoked only by {@link #of(JSONObject)}.
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
             * <p>Empty when the GraphQL envelope omits {@code id}.
             *
             * @return the {@code id} value, or empty when omitted
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the display text.
             *
             * <p>Empty when the GraphQL envelope omits {@code text}.
             *
             * @return the {@code text} value, or empty when omitted
             */
            public Optional<String> text() {
                return Optional.ofNullable(text);
            }

            /**
             * Returns the last-update timestamp.
             *
             * <p>Empty when the GraphQL envelope omits {@code update_time}; the wire-level
             * epoch-second integer is remapped to an {@link Instant}.
             *
             * @return the {@code update_time} as an {@link Instant}, or empty when omitted
             */
            public Optional<Instant> updateTime() {
                return Optional.ofNullable(updateTime).map(Instant::ofEpochSecond);
            }

            /**
             * Parses a {@code name} fragment from the given JSON object.
             *
             * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
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
             * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
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
         * Models the localised {@code description} block on the post-edit metadata.
         *
         * <p>Mirrors {@link Name}'s shape: a localisation identifier, the display text, and the
         * last-update timestamp.
         */
        public static final class Description {
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
             * Constructs a parsed {@code description} value.
             *
             * <p>Invoked only by {@link #of(JSONObject)}.
             *
             * @param id         the localisation identifier
             * @param text       the display text
             * @param updateTime the last-update epoch seconds
             */
            private Description(String id, String text, Long updateTime) {
                this.id = id;
                this.text = text;
                this.updateTime = updateTime;
            }

            /**
             * Returns the localisation identifier.
             *
             * <p>Empty when the GraphQL envelope omits {@code id}.
             *
             * @return the {@code id} value, or empty when omitted
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the display text.
             *
             * <p>Empty when the GraphQL envelope omits {@code text}.
             *
             * @return the {@code text} value, or empty when omitted
             */
            public Optional<String> text() {
                return Optional.ofNullable(text);
            }

            /**
             * Returns the last-update timestamp.
             *
             * <p>Empty when the GraphQL envelope omits {@code update_time}; the wire-level
             * epoch-second integer is remapped to an {@link Instant}.
             *
             * @return the {@code update_time} as an {@link Instant}, or empty when omitted
             */
            public Optional<Instant> updateTime() {
                return Optional.ofNullable(updateTime).map(Instant::ofEpochSecond);
            }

            /**
             * Parses a {@code description} fragment from the given JSON object.
             *
             * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
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
             * Parses every {@code description} fragment in the given JSON array.
             *
             * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
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
         * Models the {@code picture} avatar block on the post-edit metadata.
         *
         * <p>Carries the relay-issued media handle, the media type, and the direct download path the
         * client uses to fetch the full avatar bytes.
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
             * <p>Invoked only by {@link #of(JSONObject)}.
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
             * <p>Empty when the GraphQL envelope omits {@code id}.
             *
             * @return the {@code id} value, or empty when omitted
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the media-type marker.
             *
             * <p>Empty when the GraphQL envelope omits {@code type}.
             *
             * @return the {@code type} value, or empty when omitted
             */
            public Optional<String> type() {
                return Optional.ofNullable(type);
            }

            /**
             * Returns the direct download path.
             *
             * <p>Empty when the GraphQL envelope omits {@code direct_path}; otherwise carries the
             * server-relative path the client uses to fetch the avatar bytes.
             *
             * @return the {@code direct_path} value, or empty when omitted
             */
            public Optional<String> directPath() {
                return Optional.ofNullable(directPath);
            }

            /**
             * Parses a {@code picture} fragment from the given JSON object.
             *
             * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
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
             * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
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
         * Models the {@code preview} thumbnail block on the post-edit metadata.
         *
         * <p>Mirrors {@link Picture}'s shape but carries the smaller preview variant the UI renders
         * inline.
         */
        public static final class Preview {
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
             * Constructs a parsed {@code preview} value.
             *
             * <p>Invoked only by {@link #of(JSONObject)}.
             *
             * @param id         the media handle
             * @param type       the media type marker
             * @param directPath the direct download path
             */
            private Preview(String id, String type, String directPath) {
                this.id = id;
                this.type = type;
                this.directPath = directPath;
            }

            /**
             * Returns the media handle.
             *
             * <p>Empty when the GraphQL envelope omits {@code id}.
             *
             * @return the {@code id} value, or empty when omitted
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the media-type marker.
             *
             * <p>Empty when the GraphQL envelope omits {@code type}.
             *
             * @return the {@code type} value, or empty when omitted
             */
            public Optional<String> type() {
                return Optional.ofNullable(type);
            }

            /**
             * Returns the direct download path.
             *
             * <p>Empty when the GraphQL envelope omits {@code direct_path}; otherwise carries the
             * server-relative path the client uses to fetch the preview-thumbnail bytes.
             *
             * @return the {@code direct_path} value, or empty when omitted
             */
            public Optional<String> directPath() {
                return Optional.ofNullable(directPath);
            }

            /**
             * Parses a {@code preview} fragment from the given JSON object.
             *
             * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
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
             * Parses every {@code preview} fragment in the given JSON array.
             *
             * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
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
         * Models the {@code settings} container on the post-edit metadata.
         *
         * <p>The relay only echoes the {@link ReactionCodes} sub-key, which carries the
         * comma-separated reaction emoji whitelist configured for the newsletter.
         */
        public static final class Settings {
            /**
             * Holds the reaction-codes sub-key carrying the emoji whitelist.
             */
            private final ReactionCodes reactionCodes;

            /**
             * Constructs a parsed {@code settings} value.
             *
             * <p>Invoked only by {@link #of(JSONObject)}.
             *
             * @param reactionCodes the reaction-codes sub-key
             */
            private Settings(ReactionCodes reactionCodes) {
                this.reactionCodes = reactionCodes;
            }

            /**
             * Returns the reaction-codes sub-key.
             *
             * <p>Empty when the GraphQL envelope omits {@code reaction_codes}.
             *
             * @return the parsed {@link ReactionCodes}, or empty when omitted
             */
            public Optional<ReactionCodes> reactionCodes() {
                return Optional.ofNullable(reactionCodes);
            }

            /**
             * Models the {@code reaction_codes} sub-key on the newsletter settings.
             *
             * <p>Carries the relay-echoed value the request shipped; the format is a comma-separated
             * emoji whitelist or one of the relay-defined preset identifiers.
             */
            public static final class ReactionCodes {
                /**
                 * Holds the relay-echoed reaction-codes value.
                 */
                private final String value;

                /**
                 * Constructs a parsed {@code reaction_codes} value.
                 *
                 * <p>Invoked only by {@link #of(JSONObject)}.
                 *
                 * @param value the relay-echoed reaction-codes value
                 */
                private ReactionCodes(String value) {
                    this.value = value;
                }

                /**
                 * Returns the relay-echoed reaction-codes value.
                 *
                 * <p>Empty when the GraphQL envelope omits {@code value}.
                 *
                 * @return the {@code value} value, or empty when omitted
                 */
                public Optional<String> value() {
                    return Optional.ofNullable(value);
                }

                /**
                 * Parses a {@code reaction_codes} fragment from the given JSON object.
                 *
                 * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed value, or empty when {@code obj} is {@code null}
                 */
                static Optional<ReactionCodes> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var value = obj.getString("value");
                    return Optional.of(new ReactionCodes(value));
                }

                /**
                 * Parses every {@code reaction_codes} fragment in the given JSON array.
                 *
                 * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed values, empty when {@code arr} is {@code null}
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
             * Parses a {@code settings} fragment from the given JSON object.
             *
             * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
             *
             * @param obj the JSON object to parse
             * @return the parsed value, or empty when {@code obj} is {@code null}
             */
            static Optional<Settings> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var reactionCodes = ReactionCodes.of(obj.getJSONObject("reaction_codes")).orElse(null);
                return Optional.of(new Settings(reactionCodes));
            }

            /**
             * Parses every {@code settings} fragment in the given JSON array.
             *
             * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed values, empty when {@code arr} is {@code null}
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
         * Parses a {@code thread_metadata} fragment from the given JSON object.
         *
         * <p>Returns {@link Optional#empty()} when {@code obj} is {@code null}.
         *
         * @param obj the JSON object to parse
         * @return the parsed value, or empty when {@code obj} is {@code null}
         */
        static Optional<ThreadMetadata> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var name = Name.of(obj.getJSONObject("name")).orElse(null);
            var description = Description.of(obj.getJSONObject("description")).orElse(null);
            var picture = Picture.of(obj.getJSONObject("picture")).orElse(null);
            var preview = Preview.of(obj.getJSONObject("preview")).orElse(null);
            var invite = obj.getString("invite");
            var handle = obj.getString("handle");
            var verification = obj.getString("verification");
            var creationTime = obj.getLong("creation_time");
            var settings = Settings.of(obj.getJSONObject("settings")).orElse(null);
            return Optional.of(new ThreadMetadata(name, description, picture, preview, invite, handle, verification, creationTime, settings));
        }

        /**
         * Parses every {@code thread_metadata} fragment in the given JSON array.
         *
         * <p>Returns {@link List#of()} when {@code arr} is {@code null}.
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
     * Parses the response from the raw UTF-8 JSON payload of the {@code <result>} child.
     *
     * <p>Invoked only by the public {@link #of(Stanza)} overload.
     *
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_update} root
     */
    private static Optional<UpdateNewsletterMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_update");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = State.of(root.getJSONObject("state")).orElse(null);
        var threadMetadata = ThreadMetadata.of(root.getJSONObject("thread_metadata")).orElse(null);

        return Optional.of(new UpdateNewsletterMexResponse(id, state, threadMetadata));
    }
}
