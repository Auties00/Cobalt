package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the MEX response of the fetch-newsletter-message-reaction-sender-list query built by
 * {@link FetchNewsletterMessageReactionSenderListMexRequest}.
 *
 * <p>Exposes the per-emoji reaction sender groups echoed under
 * {@code xwa2_newsletters_reaction_sender_list}. Each {@link Reactions} carries one reaction code
 * and the Relay-style edges of senders that used it.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterMessageReactionSenderListJob")
public final class FetchNewsletterMessageReactionSenderListMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the per-reaction sender groups.
     */
    private final List<Reactions> reactions;

    /**
     * Constructs a response wrapping the parsed reaction groups.
     *
     * @param reactions the per-reaction sender groups
     */
    private FetchNewsletterMessageReactionSenderListMexResponse(List<Reactions> reactions) {
        this.reactions = reactions;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser. The returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.xwa2_newsletters_reaction_sender_list} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result
     *         payload
     */
    public static Optional<FetchNewsletterMessageReactionSenderListMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterMessageReactionSenderListMexResponse::of);
    }

    /**
     * Returns the per-reaction sender groups.
     *
     * @return the parsed groups, empty when the relay returned none
     */
    public List<Reactions> reactions() {
        return reactions;
    }

    /**
     * Wraps one reaction-emoji group.
     *
     * <p>Carries the {@code reaction_code} emoji and the {@link SenderList} of users that reacted
     * with it.
     */
    public static final class Reactions {
        /**
         * Holds the reaction code emoji.
         */
        private final String reactionCode;

        /**
         * Holds the senders that used the reaction.
         */
        private final SenderList senderList;

        /**
         * Constructs a reaction-group wrapper from the parsed sub-fields.
         *
         * @param reactionCode the reaction code emoji
         * @param senderList   the senders that used the reaction
         */
        private Reactions(String reactionCode, SenderList senderList) {
            this.reactionCode = reactionCode;
            this.senderList = senderList;
        }

        /**
         * Returns the reaction code emoji.
         *
         * @return the reaction code, or empty when the relay omitted the field
         */
        public Optional<String> reactionCode() {
            return Optional.ofNullable(reactionCode);
        }

        /**
         * Returns the senders that used the reaction.
         *
         * @return the parsed {@link SenderList}, or empty when the relay omitted the field
         */
        public Optional<SenderList> senderList() {
            return Optional.ofNullable(senderList);
        }

        /**
         * Wraps the {@code sender_list} sub-object.
         *
         * <p>Holds the Relay-style {@code edges} array of reactors.
         */
        public static final class SenderList {
            /**
             * Holds the Relay-style edges.
             */
            private final List<Edges> edges;

            /**
             * Constructs a sender-list wrapper from the parsed sub-fields.
             *
             * @param edges the Relay-style edges
             */
            private SenderList(List<Edges> edges) {
                this.edges = edges;
            }

            /**
             * Returns the Relay-style edges.
             *
             * @return the parsed edges, empty when the relay returned none
             */
            public List<Edges> edges() {
                return edges;
            }

            /**
             * Wraps one entry of the {@code edges} array.
             *
             * <p>Carries one sender's profile {@link Node}.
             */
            public static final class Edges {
                /**
                 * Holds the reactor profile sub-object.
                 */
                private final Node node;

                /**
                 * Constructs an edge wrapper from the parsed sub-fields.
                 *
                 * @param node the reactor profile sub-object
                 */
                private Edges(Node node) {
                    this.node = node;
                }

                /**
                 * Returns the reactor profile sub-object.
                 *
                 * @return the parsed {@link Node}, or empty when the relay omitted the field
                 */
                public Optional<Node> node() {
                    return Optional.ofNullable(node);
                }

                /**
                 * Wraps the reactor profile {@code stanza} sub-object.
                 *
                 * <p>Carries the reactor's Jid string {@code id} and the direct-path of the
                 * reactor's profile picture; WhatsApp Web resolves the Jid into a wid for downstream
                 * consumers.
                 */
                public static final class Node {
                    /**
                     * Holds the reactor Jid string.
                     */
                    private final String id;

                    /**
                     * Holds the direct-path of the reactor profile picture.
                     */
                    private final String profilePicDirectPath;

                    /**
                     * Constructs a stanza wrapper from the parsed sub-fields.
                     *
                     * @param id                   the reactor Jid string
                     * @param profilePicDirectPath the direct-path of the reactor profile picture
                     */
                    private Node(String id, String profilePicDirectPath) {
                        this.id = id;
                        this.profilePicDirectPath = profilePicDirectPath;
                    }

                    /**
                     * Returns the reactor Jid string.
                     *
                     * @return the Jid string, or empty when the relay omitted the field
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Returns the direct-path of the reactor profile picture.
                     *
                     * @return the direct path, or empty when the relay omitted the field
                     */
                    public Optional<String> profilePicDirectPath() {
                        return Optional.ofNullable(profilePicDirectPath);
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
                        var profilePicDirectPath = obj.getString("profile_pic_direct_path");
                        return Optional.of(new Node(id, profilePicDirectPath));
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
                    return Optional.of(new Edges(node));
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
             * Parses a {@link SenderList} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<SenderList> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var edges = Edges.ofArray(obj.getJSONArray("edges"));
                return Optional.of(new SenderList(edges));
            }

            /**
             * Parses a list of {@link SenderList} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<SenderList> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<SenderList>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Reactions} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Reactions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var reactionCode = obj.getString("reaction_code");
            var senderList = SenderList.of(obj.getJSONObject("sender_list")).orElse(null);
            return Optional.of(new Reactions(reactionCode, senderList));
        }

        /**
         * Parses a list of {@link Reactions} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Reactions> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Reactions>(arr.size());
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
     *         {@code data.xwa2_newsletters_reaction_sender_list} root
     */
    private static Optional<FetchNewsletterMessageReactionSenderListMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_newsletters_reaction_sender_list");
        if (root == null) {
            return Optional.empty();
        }

        var reactions = Reactions.ofArray(root.getJSONArray("reactions"));

        return Optional.of(new FetchNewsletterMessageReactionSenderListMexResponse(reactions));
    }
}
