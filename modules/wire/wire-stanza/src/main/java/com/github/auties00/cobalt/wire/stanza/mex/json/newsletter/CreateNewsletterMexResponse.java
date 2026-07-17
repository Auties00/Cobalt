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
import java.util.OptionalLong;

/**
 * Parses the MEX response of the create-newsletter mutation built by
 * {@link CreateNewsletterMexRequest}.
 *
 * <p>Exposes the fully-hydrated newsletter metadata returned under {@code xwa2_newsletter_create}:
 * the freshly-assigned id, the initial state object, the thread metadata (name, description,
 * picture, preview, invite handle, verification, subscriber count, creation time) and the viewer
 * metadata that records the local user's role (owner) and per-channel settings.
 */
@WhatsAppWebModule(moduleName = "WAWebMexCreateNewsletterJob")
public final class CreateNewsletterMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the newly assigned newsletter Jid string.
     */
    private final String id;

    /**
     * Holds the initial newsletter state object.
     */
    private final State state;

    /**
     * Holds the hydrated thread metadata (name, description, picture, preview, invite handle,
     * verification, subscriber count and creation time).
     */
    private final ThreadMetadata threadMetadata;

    /**
     * Holds the viewer-side metadata (per-channel settings and viewer role).
     */
    private final ViewerMetadata viewerMetadata;

    /**
     * Constructs a response wrapping the parsed top-level fields.
     *
     * @param id             the newly assigned newsletter Jid string
     * @param state          the initial newsletter state object
     * @param threadMetadata the hydrated thread metadata
     * @param viewerMetadata the viewer-side metadata
     */
    private CreateNewsletterMexResponse(String id, State state, ThreadMetadata threadMetadata, ViewerMetadata viewerMetadata) {
        this.id = id;
        this.state = state;
        this.threadMetadata = threadMetadata;
        this.viewerMetadata = viewerMetadata;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_create} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<CreateNewsletterMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(CreateNewsletterMexResponse::of);
    }

    /**
     * Returns the newly assigned newsletter Jid string.
     *
     * @return the newsletter id, or empty when the relay omitted the field
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the initial newsletter state object.
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
     * Wraps the {@code state} object embedded in the create-newsletter response.
     *
     * <p>Exposes only the {@code type} scalar; immediately after creation the relay reports a
     * non-deleted state.
     */
    public static final class State {
        /**
         * Holds the textual state identifier.
         */
        private final String type;

        /**
         * Constructs a state wrapping the textual type.
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
         * <p>Drives hydration of the top-level {@code state} entry from
         * {@link CreateNewsletterMexResponse#of(byte[])}.
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
         * <p>Provided for symmetry with the other nested array parsers; the create-newsletter
         * envelope does not carry a {@code state} array at the top level.
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
     * Wraps the {@code thread_metadata} sub-object embedded in the create-newsletter response.
     *
     * <p>Carries the hydrated newsletter profile: display name, description, picture and preview
     * hashes, invite handle, verification tier, subscriber count and creation time. Each nested
     * object follows the standard MEX shape (string identifier, payload, update timestamp).
     */
    public static final class ThreadMetadata {
        /**
         * Holds the display-name sub-object.
         */
        private final Name name;

        /**
         * Holds the description sub-object.
         */
        private final Description description;

        /**
         * Holds the full-resolution picture sub-object.
         */
        private final Picture picture;

        /**
         * Holds the preview-resolution picture sub-object.
         */
        private final Preview preview;

        /**
         * Holds the shareable invite identifier.
         */
        private final String invite;

        /**
         * Holds the reserved handle (vanity URL slug) of the newsletter.
         */
        private final String handle;

        /**
         * Holds the verification tier label.
         */
        private final String verification;

        /**
         * Holds the follower count at the moment of creation.
         */
        private final Long subscribersCount;

        /**
         * Holds the unix-second creation timestamp.
         */
        private final Long creationTime;

        /**
         * Constructs a thread-metadata wrapper from the parsed sub-fields.
         *
         * @param name             the display-name sub-object
         * @param description      the description sub-object
         * @param picture          the picture sub-object
         * @param preview          the preview sub-object
         * @param invite           the shareable invite identifier
         * @param handle           the reserved handle
         * @param verification     the verification tier label
         * @param subscribersCount the follower count at creation time
         * @param creationTime     the unix-second creation timestamp
         */
        private ThreadMetadata(Name name, Description description, Picture picture, Preview preview, String invite, String handle, String verification, Long subscribersCount, Long creationTime) {
            this.name = name;
            this.description = description;
            this.picture = picture;
            this.preview = preview;
            this.invite = invite;
            this.handle = handle;
            this.verification = verification;
            this.subscribersCount = subscribersCount;
            this.creationTime = creationTime;
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
         * Returns the description sub-object.
         *
         * @return the parsed {@link Description}, or empty when the relay omitted the field
         */
        public Optional<Description> description() {
            return Optional.ofNullable(description);
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
         * Returns the follower count at the moment of creation.
         *
         * @return the follower count, or empty when the relay omitted the field
         */
        public OptionalLong subscribersCount() {
            return subscribersCount != null ? OptionalLong.of(subscribersCount) : OptionalLong.empty();
        }

        /**
         * Returns the creation timestamp.
         *
         * <p>Carried by the relay as a unix-second integer and surfaced as an {@link Instant}.
         *
         * @return the parsed {@link Instant}, or empty when the relay omitted the field
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Wraps the {@code name} sub-object embedded in {@code thread_metadata}.
         *
         * <p>The MEX schema models a renameable scalar as a tuple of (server-side id, text, update
         * timestamp) so consumers can detect stale local copies during sync reconciliation.
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
             * <p>Drives hydration of the nested {@code name} entry from
             * {@link ThreadMetadata#of(JSONObject)}.
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
             * <p>Provided for symmetry; the create-newsletter envelope does not carry a
             * {@code name} array.
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
         * Wraps the {@code description} sub-object embedded in {@code thread_metadata}.
         *
         * <p>Models the same (id, text, update_time) tuple as {@link Name}, so consumers can detect
         * stale local copies during sync reconciliation.
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
             * <p>Drives hydration of the nested {@code description} entry from
             * {@link ThreadMetadata#of(JSONObject)}.
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
             * <p>Provided for symmetry; the create-newsletter envelope does not carry a
             * {@code description} array.
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
         * Wraps the {@code picture} sub-object embedded in {@code thread_metadata}.
         *
         * <p>Carries the (id, type, direct_path) triple that lets the client download the
         * full-resolution newsletter avatar from the media CDN.
         */
        public static final class Picture {
            /**
             * Holds the opaque server-side identifier of the picture revision.
             */
            private final String id;

            /**
             * Holds the picture MIME-style type discriminator.
             */
            private final String type;

            /**
             * Holds the CDN direct-path used to fetch the picture bytes.
             */
            private final String directPath;

            /**
             * Constructs a picture wrapper from the parsed sub-fields.
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
             * <p>Drives hydration of the nested {@code picture} entry from
             * {@link ThreadMetadata#of(JSONObject)}.
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
             * <p>Provided for symmetry; the create-newsletter envelope does not carry a
             * {@code picture} array.
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
         * <p>Mirrors {@link Picture} but points at the preview-resolution avatar used in chat-list
         * rendering paths.
         */
        public static final class Preview {
            /**
             * Holds the opaque server-side identifier of the preview revision.
             */
            private final String id;

            /**
             * Holds the preview MIME-style type discriminator.
             */
            private final String type;

            /**
             * Holds the CDN direct-path used to fetch the preview bytes.
             */
            private final String directPath;

            /**
             * Constructs a preview wrapper from the parsed sub-fields.
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
             * <p>Drives hydration of the nested {@code preview} entry from
             * {@link ThreadMetadata#of(JSONObject)}.
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
             * <p>Provided for symmetry; the create-newsletter envelope does not carry a
             * {@code preview} array.
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
         * Parses a {@link ThreadMetadata} from the given JSON object.
         *
         * <p>Drives hydration of the {@code thread_metadata} entry from
         * {@link CreateNewsletterMexResponse#of(byte[])}.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link ThreadMetadata}, or empty when {@code obj} is {@code null}
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
            var subscribersCount = obj.getLong("subscribers_count");
            var creationTime = obj.getLong("creation_time");
            return Optional.of(new ThreadMetadata(name, description, picture, preview, invite, handle, verification, subscribersCount, creationTime));
        }

        /**
         * Parses a list of {@link ThreadMetadata} entries from the given JSON array.
         *
         * <p>Provided for symmetry; the create-newsletter envelope does not carry a
         * {@code thread_metadata} array.
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
     * Wraps the {@code viewer_metadata} sub-object embedded in the create-newsletter response.
     *
     * <p>Records per-viewer state that the local user holds against the newly-created newsletter;
     * immediately after creation the role is always owner.
     */
    public static final class ViewerMetadata {
        /**
         * Holds the per-channel viewer-side settings (type/value pairs).
         */
        private final List<Settings> settings;

        /**
         * Holds the viewer's role on the newsletter (for example owner).
         */
        private final String role;

        /**
         * Constructs a viewer-metadata wrapper from the parsed sub-fields.
         *
         * @param settings the viewer-side settings list
         * @param role     the viewer's role on the newsletter
         */
        private ViewerMetadata(List<Settings> settings, String role) {
            this.settings = settings;
            this.role = role;
        }

        /**
         * Returns the per-channel viewer-side settings.
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
             * @param type  the discriminator
             * @param value the textual value
             */
            private Settings(String type, String value) {
                this.type = type;
                this.value = value;
            }

            /**
             * Returns the discriminator identifying which viewer preference this entry represents.
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
             * <p>Drives per-entry hydration from {@link Settings#ofArray(JSONArray)}.
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
         * <p>Drives hydration of the {@code viewer_metadata} entry from
         * {@link CreateNewsletterMexResponse#of(byte[])}.
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
            return Optional.of(new ViewerMetadata(settings, role));
        }

        /**
         * Parses a list of {@link ViewerMetadata} entries from the given JSON array.
         *
         * <p>Provided for symmetry; the create-newsletter envelope does not carry a
         * {@code viewer_metadata} array.
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
     * @implNote This implementation guards every nested object lookup so a malformed envelope
     * produces {@link Optional#empty()} rather than a parser exception.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return the parsed response, or empty when the envelope lacks the expected
     *         {@code data.xwa2_newsletter_create} root
     */
    private static Optional<CreateNewsletterMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_create");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var state = State.of(root.getJSONObject("state")).orElse(null);
        var threadMetadata = ThreadMetadata.of(root.getJSONObject("thread_metadata")).orElse(null);
        var viewerMetadata = ViewerMetadata.of(root.getJSONObject("viewer_metadata")).orElse(null);

        return Optional.of(new CreateNewsletterMexResponse(id, state, threadMetadata, viewerMetadata));
    }
}
