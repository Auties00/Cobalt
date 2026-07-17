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
 * Parses the MEX response of the fetch-newsletter-followers query built by
 * {@link FetchNewsletterFollowersMexRequest}.
 *
 * <p>Exposes the follower roster echoed under {@code xwa2_newsletter_followers}. The
 * {@link Followers} sub-object wraps the Relay-style {@code edges} array where each
 * {@link Followers.Edges} carries one follower's profile, role and follow time.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterFollowersJob")
public final class FetchNewsletterFollowersMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the follower-edges container.
     */
    private final Followers followers;

    /**
     * Constructs a response wrapping the parsed followers container.
     *
     * @param followers the follower-edges container
     */
    private FetchNewsletterFollowersMexResponse(Followers followers) {
        this.followers = followers;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletter_followers} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterFollowersMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterFollowersMexResponse::of);
    }

    /**
     * Returns the follower-edges container.
     *
     * @return the parsed {@link Followers}, or empty when the relay omitted the field
     */
    public Optional<Followers> followers() {
        return Optional.ofNullable(followers);
    }

    /**
     * Wraps the {@code followers} sub-object.
     *
     * <p>Holds the Relay-style {@code edges} array; an empty list with no edges indicates the
     * newsletter has no followers visible to the caller.
     */
    public static final class Followers {
        /**
         * Holds the Relay-style edges of the follower connection.
         */
        private final List<Edges> edges;

        /**
         * Constructs a followers wrapper from the parsed sub-fields.
         *
         * @param edges the follower edges
         */
        private Followers(List<Edges> edges) {
            this.edges = edges;
        }

        /**
         * Returns the follower edges.
         *
         * @return the parsed edges, empty when the relay returned none
         */
        public List<Edges> edges() {
            return edges;
        }

        /**
         * Wraps one entry of the {@code edges} array.
         *
         * <p>Carries one follower's profile {@link Node}, the per-follower {@code follow_time}
         * epoch-second, and the {@code role} label ({@code OWNER}, {@code ADMIN} or
         * {@code SUBSCRIBER}); WhatsApp Web sorts admins and owners ahead of subscribers in the UI.
         */
        public static final class Edges {
            /**
             * Holds the follower profile sub-object.
             */
            private final Node node;

            /**
             * Holds the follow epoch-second.
             */
            private final Long followTime;

            /**
             * Holds the follower role label.
             */
            private final String role;

            /**
             * Holds the admin profile projection echoed for admin and owner followers.
             */
            private final AdminProfile adminProfile;

            /**
             * Constructs an edge wrapper from the parsed sub-fields.
             *
             * @param node         the follower profile sub-object
             * @param followTime   the follow epoch-second
             * @param role         the follower role label
             * @param adminProfile the admin profile projection
             */
            private Edges(Node node, Long followTime, String role, AdminProfile adminProfile) {
                this.node = node;
                this.followTime = followTime;
                this.role = role;
                this.adminProfile = adminProfile;
            }

            /**
             * Returns the follower profile sub-object.
             *
             * @return the parsed {@link Node}, or empty when the relay omitted the field
             */
            public Optional<Node> node() {
                return Optional.ofNullable(node);
            }

            /**
             * Returns the follow instant.
             *
             * @return the follow instant, or empty when the relay omitted the field
             */
            public Optional<Instant> followTime() {
                return Optional.ofNullable(followTime).map(Instant::ofEpochSecond);
            }

            /**
             * Returns the follower role label.
             *
             * @return the role label, or empty when the relay omitted the field
             */
            public Optional<String> role() {
                return Optional.ofNullable(role);
            }

            /**
             * Returns the admin profile projection.
             *
             * <p>WhatsApp Web populates this only for followers whose {@link #role()} is an admin or
             * owner; subscriber edges omit the sub-object.
             *
             * @return the parsed {@link AdminProfile}, or empty when the relay omitted the field
             */
            public Optional<AdminProfile> adminProfile() {
                return Optional.ofNullable(adminProfile);
            }

            /**
             * Wraps the follower profile {@code stanza} sub-object.
             *
             * <p>Carries the follower's Jid string {@code id}, a display name, an optional phone
             * number string {@code pn}, and an optional username sub-object populated only when the
             * newsletter username-PN-privacy gate is on.
             */
            public static final class Node {
                /**
                 * Holds the follower Jid string.
                 */
                private final String id;

                /**
                 * Holds the follower display name.
                 */
                private final String displayName;

                /**
                 * Holds the follower phone-number string.
                 */
                private final String pn;

                /**
                 * Holds the follower username sub-object.
                 */
                private final UsernameInfo usernameInfo;

                /**
                 * Constructs a stanza wrapper from the parsed sub-fields.
                 *
                 * @param id           the follower Jid string
                 * @param displayName  the follower display name
                 * @param pn           the follower phone-number string
                 * @param usernameInfo the follower username sub-object
                 */
                private Node(String id, String displayName, String pn, UsernameInfo usernameInfo) {
                    this.id = id;
                    this.displayName = displayName;
                    this.pn = pn;
                    this.usernameInfo = usernameInfo;
                }

                /**
                 * Returns the follower Jid string.
                 *
                 * @return the Jid string, or empty when the relay omitted the field
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the follower display name.
                 *
                 * @return the display name, or empty when the relay omitted the field
                 */
                public Optional<String> displayName() {
                    return Optional.ofNullable(displayName);
                }

                /**
                 * Returns the follower phone-number string.
                 *
                 * @return the phone number, or empty when the relay omitted the field
                 */
                public Optional<String> pn() {
                    return Optional.ofNullable(pn);
                }

                /**
                 * Returns the follower username sub-object.
                 *
                 * @return the parsed {@link UsernameInfo}, or empty when the relay omitted the field
                 */
                public Optional<UsernameInfo> usernameInfo() {
                    return Optional.ofNullable(usernameInfo);
                }

                /**
                 * Wraps the {@code username_info} sub-object.
                 *
                 * <p>Populated only when the newsletter username-PN-privacy gate is on; otherwise
                 * the relay omits the sub-object.
                 */
                public static final class UsernameInfo {
                    /**
                     * Holds the follower username string.
                     */
                    private final String username;

                    /**
                     * Constructs a username-info wrapper from the parsed sub-fields.
                     *
                     * @param username the follower username string
                     */
                    private UsernameInfo(String username) {
                        this.username = username;
                    }

                    /**
                     * Returns the follower username string.
                     *
                     * @return the username, or empty when the relay omitted the field
                     */
                    public Optional<String> username() {
                        return Optional.ofNullable(username);
                    }

                    /**
                     * Parses a {@link UsernameInfo} from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
                     */
                    static Optional<UsernameInfo> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var username = obj.getString("username");
                        return Optional.of(new UsernameInfo(username));
                    }

                    /**
                     * Parses a list of {@link UsernameInfo} entries from the given JSON array.
                     *
                     * @param arr the JSON array to parse
                     * @return the parsed list, empty when {@code arr} is {@code null}
                     */
                    static List<UsernameInfo> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<UsernameInfo>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Parses a {@link Node} from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
                 */
                static Optional<Node> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var displayName = obj.getString("display_name");
                    var pn = obj.getString("pn");
                    var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
                    return Optional.of(new Node(id, displayName, pn, usernameInfo));
                }

                /**
                 * Parses a list of {@link Node} entries from the given JSON array.
                 *
                 * @param arr the JSON array to parse
                 * @return the parsed list, empty when {@code arr} is {@code null}
                 */
                static List<Node> ofArray(JSONArray arr) {
                    if (arr == null) {
                        return List.of();
                    }

                    var result = new ArrayList<Node>(arr.size());
                    for (var i = 0; i < arr.size(); i++) {
                        of(arr.getJSONObject(i)).ifPresent(result::add);
                    }
                    return result;
                }
            }

            /**
             * Wraps the {@code admin_profile} sub-object echoed for admin and owner followers.
             *
             * <p>Carries the admin's Jid string {@code id}, display {@code name}, and a
             * {@link Picture} reference used to render the admin profile chip.
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
                     * @param obj the JSON object to parse
                     * @return the parsed entry, or empty when {@code obj} is {@code null}
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
                 * @param obj the JSON object to parse
                 * @return the parsed entry, or empty when {@code obj} is {@code null}
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
             * Parses an {@link Edges} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<Edges> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var node = Node.of(obj.getJSONObject("node")).orElse(null);
                var followTime = obj.getLong("follow_time");
                var role = obj.getString("role");
                var adminProfile = AdminProfile.of(obj.getJSONObject("admin_profile")).orElse(null);
                return Optional.of(new Edges(node, followTime, role, adminProfile));
            }

            /**
             * Parses a list of {@link Edges} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<Edges> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Edges>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Followers} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Followers> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var edges = Edges.ofArray(obj.getJSONArray("edges"));
            return Optional.of(new Followers(edges));
        }

        /**
         * Parses a list of {@link Followers} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Followers> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Followers>(arr.size());
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
     *         {@code data.xwa2_newsletter_followers} root
     */
    private static Optional<FetchNewsletterFollowersMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletter_followers");
        if (root == null) {
            return Optional.empty();
        }

        var followers = Followers.of(root.getJSONObject("followers")).orElse(null);

        return Optional.of(new FetchNewsletterFollowersMexResponse(followers));
    }
}
