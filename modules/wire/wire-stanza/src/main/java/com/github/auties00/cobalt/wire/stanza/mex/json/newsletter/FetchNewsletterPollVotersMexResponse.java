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

/**
 * Parses the MEX response of the fetch-newsletter-poll-voters query built by
 * {@link FetchNewsletterPollVotersMexRequest}.
 *
 * <p>Exposes the per-option voter lists echoed under {@code voter_list}; each {@link Votes} carries
 * one option (keyed by its base64-encoded {@code vote_hash}) and the Relay-style edges of voters
 * that selected it. WA Web converts the {@code vote_hash} to hex before keying its in-memory map.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterPollVotersJob")
public final class FetchNewsletterPollVotersMexResponse implements MexStanza.Response.Json {
    /**
     * Holds the per-option voter groups.
     */
    private final List<Votes> votes;

    /**
     * Constructs a response wrapping the parsed voter groups.
     *
     * @param votes the per-option voter groups
     */
    private FetchNewsletterPollVotersMexResponse(List<Votes> votes) {
        this.votes = votes;
    }

    /**
     * Parses the MEX response carried by the given IQ result stanza.
     *
     * <p>Drains the {@code <result>} child's byte content into the JSON parser; the returned
     * {@link Optional} is empty when the result child is missing or when the JSON envelope omits the
     * expected {@code data.voter_list} root.
     *
     * @param stanza the IQ result stanza received from the relay
     * @return the parsed response, or empty when the stanza does not carry a well-formed result payload
     */
    public static Optional<FetchNewsletterPollVotersMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchNewsletterPollVotersMexResponse::of);
    }

    /**
     * Returns the per-option voter groups.
     *
     * @return the parsed groups, empty when the relay returned none
     */
    public List<Votes> votes() {
        return votes;
    }

    /**
     * Wraps one entry of the {@code votes} array: one poll option and its voters.
     *
     * <p>The {@code vote_hash} is the base64-encoded server hash of the option text, so the same
     * option produces the same hash across all voters; the {@link VoterList} carries the Relay-style
     * edges of voters that picked the option.
     */
    public static final class Votes {
        /**
         * Holds the base64-encoded option hash.
         */
        private final String voteHash;

        /**
         * Holds the voters that picked the option.
         */
        private final VoterList voterList;

        /**
         * Constructs a votes wrapper from the parsed sub-fields.
         *
         * @param voteHash  the base64-encoded option hash
         * @param voterList the voters that picked the option
         */
        private Votes(String voteHash, VoterList voterList) {
            this.voteHash = voteHash;
            this.voterList = voterList;
        }

        /**
         * Returns the base64-encoded option hash.
         *
         * @return the option hash, or empty when the relay omitted the field
         */
        public Optional<String> voteHash() {
            return Optional.ofNullable(voteHash);
        }

        /**
         * Returns the voters that picked the option.
         *
         * @return the parsed {@link VoterList}, or empty when the relay omitted the field
         */
        public Optional<VoterList> voterList() {
            return Optional.ofNullable(voterList);
        }

        /**
         * Wraps the {@code voter_list} sub-object.
         *
         * <p>Holds the Relay-style {@code edges} array of voters.
         */
        public static final class VoterList {
            /**
             * Holds the Relay-style edges.
             */
            private final List<Edges> edges;

            /**
             * Constructs a voter-list wrapper from the parsed sub-fields.
             *
             * @param edges the Relay-style edges
             */
            private VoterList(List<Edges> edges) {
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
             * <p>Carries the vote {@code action_time} (microseconds since epoch on the wire, which WA
             * Web divides by {@code 1e6} to convert to seconds) and the voter profile {@link Node}.
             */
            public static final class Edges {
                /**
                 * Holds the vote action time, in microseconds since the epoch.
                 */
                private final Long actionTime;

                /**
                 * Holds the voter profile sub-object.
                 */
                private final Node node;

                /**
                 * Constructs an edge wrapper from the parsed sub-fields.
                 *
                 * @param actionTime the vote action time, in microseconds
                 * @param node       the voter profile sub-object
                 */
                private Edges(Long actionTime, Node node) {
                    this.actionTime = actionTime;
                    this.node = node;
                }

                /**
                 * Returns the voter profile sub-object.
                 *
                 * @return the parsed {@link Node}, or empty when the relay omitted the field
                 */
                public Optional<Node> node() {
                    return Optional.ofNullable(node);
                }

                /**
                 * Wraps the voter profile {@code stanza} sub-object.
                 *
                 * <p>Carries only the voter Jid string; WA Web logs a warning when the id does not
                 * resolve to a plain-user Jid.
                 */
                public static final class Node {
                    /**
                     * Holds the voter Jid string.
                     */
                    private final String id;

                    /**
                     * Constructs a stanza wrapper from the parsed sub-fields.
                     *
                     * @param id the voter Jid string
                     */
                    private Node(String id) {
                        this.id = id;
                    }

                    /**
                     * Returns the voter Jid string.
                     *
                     * @return the Jid string, or empty when the relay omitted the field
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
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
                        return Optional.of(new Node(id));
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

                    var actionTime = obj.getLong("action_time");
                    var node = Node.of(obj.getJSONObject("node")).orElse(null);
                    return Optional.of(new Edges(actionTime, node));
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
             * Parses a {@link VoterList} from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return the parsed entry, or empty when {@code obj} is {@code null}
             */
            static Optional<VoterList> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var edges = Edges.ofArray(obj.getJSONArray("edges"));
                return Optional.of(new VoterList(edges));
            }

            /**
             * Parses a list of {@link VoterList} entries from the given JSON array.
             *
             * @param arr the JSON array to parse
             * @return the parsed list, empty when {@code arr} is {@code null}
             */
            static List<VoterList> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<VoterList>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@link Votes} from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return the parsed entry, or empty when {@code obj} is {@code null}
         */
        static Optional<Votes> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var voteHash = obj.getString("vote_hash");
            var voterList = VoterList.of(obj.getJSONObject("voter_list")).orElse(null);
            return Optional.of(new Votes(voteHash, voterList));
        }

        /**
         * Parses a list of {@link Votes} entries from the given JSON array.
         *
         * @param arr the JSON array to parse
         * @return the parsed list, empty when {@code arr} is {@code null}
         */
        static List<Votes> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Votes>(arr.size());
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
     *         {@code data.voter_list} root
     */
    private static Optional<FetchNewsletterPollVotersMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("voter_list");
        if (root == null) {
            return Optional.empty();
        }

        var votes = Votes.ofArray(root.getJSONArray("votes"));

        return Optional.of(new FetchNewsletterPollVotersMexResponse(votes));
    }
}
