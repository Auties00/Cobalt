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
 * Parses the MEX response of the fetch-newsletter-admin-info query built by
 * {@link FetchNewsletterAdminInfoMexRequest}.
 *
 * <p>Exposes the contents echoed under {@code xwa2_newsletter_admin}: the admin headcount scalar
 * {@code admin_count}, the newsletter id, the calling admin's {@link AdminProfile} projection
 * (display name and picture), and the newsletter's {@link AdminSettings} flags.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterAdminInfoJob")
public final class FetchNewsletterAdminInfoMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the admin headcount echoed under {@code xwa2_newsletter_admin.admin_count}.
     */
    private final Long adminCount;

    /**
     * Holds the calling admin's profile projection echoed under
     * {@code xwa2_newsletter_admin.admin_profile}.
     */
    private final AdminProfile adminProfile;

    /**
     * Holds the newsletter admin settings echoed under
     * {@code xwa2_newsletter_admin.admin_settings}.
     */
    private final AdminSettings adminSettings;

    /**
     * Holds the newsletter Jid string echoed under {@code xwa2_newsletter_admin.id}.
     */
    private final String id;

    /**
     * Constructs a response wrapping the parsed fields.
     *
     * <p>Reserved for the static parser; external callers obtain instances via {@link #of(Stanza)}.
     *
     * @param adminCount    the admin headcount
     * @param adminProfile  the calling admin's profile projection
     * @param adminSettings the newsletter admin settings
     * @param id            the newsletter Jid echoed by the relay
     */
    private FetchNewsletterAdminInfoMexResponse(Long adminCount, AdminProfile adminProfile, AdminSettings adminSettings, String id) {
        this.adminCount = adminCount;
        this.adminProfile = adminProfile;
        this.adminSettings = adminSettings;
        this.id = id;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_admin} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterAdminInfoMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterAdminInfoMexResponse::of);
    }

    /**
     * Returns the admin headcount.
     *
     * <p>WhatsApp Web falls back to a default admin count when this scalar is omitted; callers must
     * apply their own fallback if they need a numeric value.
     *
     * @return the admin headcount, or empty when the relay omitted the field
     */
    public OptionalLong adminCount() {
        return adminCount != null ? OptionalLong.of(adminCount) : OptionalLong.empty();
    }

    /**
     * Returns the calling admin's profile projection.
     *
     * @return the parsed {@link AdminProfile}, or empty when the relay omitted the field
     */
    public Optional<AdminProfile> adminProfile() {
        return Optional.ofNullable(adminProfile);
    }

    /**
     * Returns the newsletter admin settings.
     *
     * @return the parsed {@link AdminSettings}, or empty when the relay omitted the field
     */
    public Optional<AdminSettings> adminSettings() {
        return Optional.ofNullable(adminSettings);
    }

    /**
     * Returns the newsletter Jid string echoed by the relay.
     *
     * @return the echoed newsletter id, or empty when the relay omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Wraps the {@code admin_profile} sub-object echoed for the calling admin.
     *
     * <p>Carries the admin's Jid string {@code id}, display {@code name}, and a {@link Picture}
     * reference used to render the admin profile chip.
     */
    public static final class AdminProfile {
        /**
         * Holds the admin Jid string.
         */
        private final String id;

        /**
         * Holds the admin display name.
         */
        private final String name;

        /**
         * Holds the admin picture reference projection.
         */
        private final Picture picture;

        /**
         * Constructs an admin-profile wrapper from the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param id      the admin Jid string
         * @param name    the admin display name
         * @param picture the admin picture reference projection
         */
        private AdminProfile(String id, String name, Picture picture) {
            this.id = id;
            this.name = name;
            this.picture = picture;
        }

        /**
         * Returns the admin Jid string.
         *
         * @return the admin id, or empty when the relay omitted the field
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the admin display name.
         *
         * @return the display name, or empty when the relay omitted the field
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the admin picture reference projection.
         *
         * @return the parsed {@link Picture}, or empty when the relay omitted the field
         */
        public Optional<Picture> picture() {
            return Optional.ofNullable(picture);
        }

        /**
         * Wraps the {@code picture} reference sub-object.
         *
         * <p>Carries the file id and the relay direct-path used to fetch the picture bytes.
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
             * Constructs a picture wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param id         the file identifier
             * @param directPath the relay direct-path
             */
            private Picture(String id, String directPath) {
                this.id = id;
                this.directPath = directPath;
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
             * Parses a {@link Picture} from the given JSON object.
             *
             * <p>Used by {@link AdminProfile#of(JSONObject)} to hydrate the nested {@code picture}
             * entry.
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
                return Optional.of(new Picture(id, directPath));
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
         * Parses an {@link AdminProfile} from the given JSON object.
         *
         * <p>Used by {@link FetchNewsletterAdminInfoMexResponse#of(byte[])} to hydrate the nested
         * {@code admin_profile} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link AdminProfile}, or empty when {@code obj} is {@code null}
         */
        static Optional<AdminProfile> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var name = obj.getString("name");
            var picture = Picture.of(obj.getJSONObject("picture")).orElse(null);
            return Optional.of(new AdminProfile(id, name, picture));
        }

        /**
         * Parses a list of {@link AdminProfile} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<AdminProfile> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<AdminProfile>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Wraps the {@code admin_settings} sub-object echoed for the newsletter.
     *
     * <p>Carries the {@code admin_profiles_enabled} flag governing whether admin profile chips are
     * surfaced to followers.
     */
    public static final class AdminSettings {
        /**
         * Holds whether admin profiles are enabled for this newsletter.
         */
        private final Boolean adminProfilesEnabled;

        /**
         * Constructs an admin-settings wrapper from the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param adminProfilesEnabled whether admin profiles are enabled
         */
        private AdminSettings(Boolean adminProfilesEnabled) {
            this.adminProfilesEnabled = adminProfilesEnabled;
        }

        /**
         * Returns whether admin profiles are enabled for this newsletter.
         *
         * @return {@code true} when the relay reported admin profiles enabled, {@code false} when
         *         it did not or omitted the field
         */
        public boolean adminProfilesEnabled() {
            return adminProfilesEnabled != null && adminProfilesEnabled;
        }

        /**
         * Parses an {@link AdminSettings} from the given JSON object.
         *
         * <p>Used by {@link FetchNewsletterAdminInfoMexResponse#of(byte[])} to hydrate the nested
         * {@code admin_settings} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link AdminSettings}, or empty when {@code obj} is {@code null}
         */
        static Optional<AdminSettings> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var adminProfilesEnabled = obj.getBoolean("admin_profiles_enabled");
            return Optional.of(new AdminSettings(adminProfilesEnabled));
        }

        /**
         * Parses a list of {@link AdminSettings} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<AdminSettings> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<AdminSettings>(arr.size());
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
     *         {@code data.xwa2_newsletter_admin} root
     */
    private static Optional<FetchNewsletterAdminInfoMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_admin");
        if (root == null) {
            return Optional.empty();
        }

        var adminCount = root.getLong("admin_count");
        var adminProfile = AdminProfile.of(root.getJSONObject("admin_profile")).orElse(null);
        var adminSettings = AdminSettings.of(root.getJSONObject("admin_settings")).orElse(null);
        var id = root.getString("id");

        return Optional.of(new FetchNewsletterAdminInfoMexResponse(adminCount, adminProfile, adminSettings, id));
    }
}
