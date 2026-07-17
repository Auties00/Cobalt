package com.github.auties00.cobalt.wire.stanza.mex.json.community;

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
import java.util.OptionalLong;

/**
 * Parsed response of the {@code mexFetchSubgroupSuggestions} MEX query.
 *
 * <p>This response carries the per-community list of suggested subgroups
 * projected from
 * {@code data.xwa2_group_query_by_id.sub_group_suggestions}. It is the inbound
 * counterpart of {@link FetchSubgroupSuggestionsMexRequest} and populates the
 * suggested-subgroups picker.
 *
 * @implNote This implementation preserves the GraphQL connection shape
 * ({@code sub_group_suggestions.edges[].stanza}) verbatim rather than flattening
 * it into a single list, so callers can distinguish a missing suggestions
 * container from an empty edges list. Unlike WA Web, missing {@code id},
 * {@code creator.id} or {@code is_existing_group} fields do not raise; they
 * collapse to empty fields on the projection.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchSubgroupSuggestionsJob")
public final class FetchSubgroupSuggestionsMexResponse implements MexStanza.Response.Json {
    /**
     * Community group identifier returned by the relay, or {@code null} when
     * the relay omitted it.
     *
     * <p>Identifies the parent community that the suggestions belong to;
     * matches the {@code group_id} sent in the request.
     */
    private final String id;

    /**
     * Subgroup suggestions container returned by the relay, or {@code null}
     * when the relay did not project it.
     *
     * <p>Wraps the {@code edges} GraphQL array of suggested subgroup nodes.
     */
    private final SubGroupSuggestions subGroupSuggestions;

    /**
     * Constructs a new response with the given fields.
     *
     * @param id                  the community group identifier
     * @param subGroupSuggestions the subgroup suggestions container
     */
    private FetchSubgroupSuggestionsMexResponse(String id, SubGroupSuggestions subGroupSuggestions) {
        this.id = id;
        this.subGroupSuggestions = subGroupSuggestions;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling
     * {@code <iq xmlns="w:mex">} replies tagged with
     * {@link FetchSubgroupSuggestionsMexRequest#QUERY_ID}. It unwraps the
     * {@code <result>} child, reads its content bytes and decodes the GraphQL
     * JSON envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected
     *         JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchSubgroupSuggestionsJob", exports = "mexFetchSubgroupSuggestions",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchSubgroupSuggestionsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchSubgroupSuggestionsMexResponse::of);
    }

    /**
     * Returns the community group identifier returned by the relay.
     *
     * <p>Matches the {@code group_id} sent in the request; the value is empty
     * when the field is absent from a malformed relay reply.
     *
     * @return an {@link Optional} containing the identifier, or empty if absent
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the subgroup suggestions container.
     *
     * <p>The value is empty when the relay omitted the
     * {@code sub_group_suggestions} GraphQL container; the edges list inside
     * may itself be empty when the community has no candidates.
     *
     * @return an {@link Optional} containing the container, or empty if absent
     */
    public Optional<SubGroupSuggestions> subGroupSuggestions() {
        return Optional.ofNullable(subGroupSuggestions);
    }

    /**
     * Subgroup suggestions container wrapping the {@code edges} array of
     * suggested subgroup nodes.
     *
     * <p>This type preserves the GraphQL connection shape; each {@link Edges}
     * entry wraps a single suggested {@link Edges.Node}.
     */
    public static final class SubGroupSuggestions {
        /**
         * Suggestion edges returned by the relay, never {@code null} once
         * {@link SubGroupSuggestions#of(JSONObject)} has succeeded.
         *
         * <p>Mirrors the {@code sub_group_suggestions.edges} GraphQL array.
         */
        private final List<Edges> edges;

        /**
         * Constructs a new suggestions container with the given edges.
         *
         * @param edges the suggestion edges
         */
        private SubGroupSuggestions(List<Edges> edges) {
            this.edges = edges;
        }

        /**
         * Returns the suggestion edges.
         *
         * <p>Iterate this list to walk the suggested subgroups; each entry
         * exposes a {@link Edges#node()} accessor that may be empty when the
         * relay returned a malformed edge.
         *
         * @return the list of edges, empty if absent
         */
        public List<Edges> edges() {
            return edges;
        }

        /**
         * Single edge wrapper around a suggested subgroup stanza.
         *
         * <p>The edge level exists to leave room for future GraphQL cursor
         * metadata without breaking the binary shape.
         */
        public static final class Edges {
            /**
             * Suggested subgroup stanza carried by the edge, or {@code null} when
             * the relay returned an edge envelope without a {@code stanza} child.
             */
            private final Node node;

            /**
             * Constructs a new edge wrapping the given stanza.
             *
             * @param node the suggested subgroup stanza
             */
            private Edges(Node node) {
                this.node = node;
            }

            /**
             * Returns the suggested subgroup stanza.
             *
             * <p>The value is empty when the relay returned the edge envelope
             * without a {@code stanza} child; WA Web treats this case as an error
             * whereas Cobalt prefers to surface it as {@link Optional#empty()}.
             *
             * @return an {@link Optional} containing the stanza, or empty if
             *         absent
             */
            public Optional<Node> node() {
                return Optional.ofNullable(node);
            }

            /**
             * Suggested subgroup stanza carrying the subgroup identifier with its
             * subject, description, creator, creation timestamp, participant
             * count, existing-group flag and hidden-group state.
             *
             * <p>There is one such stanza per candidate suggestion under the
             * community; it populates the suggestions store.
             */
            public static final class Node {
                /**
                 * Subgroup identifier, or {@code null} when the relay omitted
                 * it.
                 *
                 * <p>The suggested subgroup's WhatsApp WID stringified.
                 */
                private final String id;

                /**
                 * Subgroup subject metadata, or {@code null} when the relay
                 * omitted it.
                 *
                 * <p>Carries the subject text only; the suggestions GraphQL
                 * document does not project the creation timestamp.
                 */
                private final Subject subject;

                /**
                 * Subgroup description metadata, or {@code null} when the relay
                 * omitted it.
                 *
                 * <p>Carries the description text and the description metadata
                 * identifier.
                 */
                private final Description description;

                /**
                 * Subgroup creator metadata, or {@code null} when the relay
                 * omitted it.
                 *
                 * <p>Carries the creator WID.
                 */
                private final Creator creator;

                /**
                 * Subgroup creation epoch-second timestamp, or {@code null}
                 * when the relay omitted it.
                 *
                 * <p>Sourced from the {@code creation_time} GraphQL scalar;
                 * widened to {@link Instant} on read.
                 */
                private final Long creationTime;

                /**
                 * Total participant count for the subgroup, or {@code null}
                 * when the relay omitted it.
                 *
                 * <p>Mirrors the {@code total_participants_count} GraphQL
                 * scalar; reflects all members, not just those who already
                 * overlap with the community.
                 */
                private final Long totalParticipantsCount;

                /**
                 * Whether the suggested subgroup is already an existing group
                 * the user is part of, or {@code null} when the relay omitted
                 * the field.
                 *
                 * <p>{@code true} when the user is already a member (and the
                 * community admin could promote it to a subgroup),
                 * {@code false} when it is a discovery suggestion.
                 */
                private final Boolean isExistingGroup;

                /**
                 * Hidden-group state tag, or {@code null} when the relay
                 * omitted it.
                 *
                 * <p>Sourced from the {@code hidden_group} GraphQL scalar.
                 */
                private final String hiddenGroup;

                /**
                 * Constructs a new suggested subgroup stanza.
                 *
                 * @param id                     the subgroup identifier
                 * @param subject                the subject metadata
                 * @param description            the description metadata
                 * @param creator                the creator metadata
                 * @param creationTime           the creation epoch-second
                 *                               timestamp
                 * @param totalParticipantsCount the total participant count
                 * @param isExistingGroup        whether the user is already a
                 *                               participant
                 * @param hiddenGroup            the hidden-group state tag
                 */
                private Node(String id, Subject subject, Description description, Creator creator, Long creationTime, Long totalParticipantsCount, Boolean isExistingGroup, String hiddenGroup) {
                    this.id = id;
                    this.subject = subject;
                    this.description = description;
                    this.creator = creator;
                    this.creationTime = creationTime;
                    this.totalParticipantsCount = totalParticipantsCount;
                    this.isExistingGroup = isExistingGroup;
                    this.hiddenGroup = hiddenGroup;
                }

                /**
                 * Returns the subgroup identifier.
                 *
                 * <p>The value is empty when the relay omitted the {@code id}
                 * field from this suggested subgroup.
                 *
                 * @return an {@link Optional} containing the identifier, or
                 *         empty if absent
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the subgroup subject metadata.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code subject} GraphQL object.
                 *
                 * @return an {@link Optional} containing the subject, or empty
                 *         if absent
                 */
                public Optional<Subject> subject() {
                    return Optional.ofNullable(subject);
                }

                /**
                 * Returns the subgroup description metadata.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code description} GraphQL object; suggestions for groups
                 * without a description return empty here.
                 *
                 * @return an {@link Optional} containing the description, or
                 *         empty if absent
                 */
                public Optional<Description> description() {
                    return Optional.ofNullable(description);
                }

                /**
                 * Returns the subgroup creator metadata.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code creator} GraphQL object.
                 *
                 * @return an {@link Optional} containing the creator, or empty
                 *         if absent
                 */
                public Optional<Creator> creator() {
                    return Optional.ofNullable(creator);
                }

                /**
                 * Returns the subgroup creation timestamp.
                 *
                 * <p>Present values are widened from the wire epoch-second long
                 * through {@link Instant#ofEpochSecond(long)}; the value is
                 * empty when the relay omitted the {@code creation_time} field.
                 *
                 * @return an {@link Optional} containing the {@link Instant}, or
                 *         empty if absent
                 */
                public Optional<Instant> creationTime() {
                    return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
                }

                /**
                 * Returns the total participant count for the subgroup.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code total_participants_count} field; callers that need a
                 * primitive can fall back to {@code 0}.
                 *
                 * @return an {@link OptionalLong} containing the count, or empty
                 *         if absent
                 */
                public OptionalLong totalParticipantsCount() {
                    return totalParticipantsCount != null ? OptionalLong.of(totalParticipantsCount) : OptionalLong.empty();
                }

                /**
                 * Returns whether the suggested subgroup is already an existing
                 * group the user is part of.
                 *
                 * <p>Returns {@code false} when the relay omitted the field.
                 *
                 * @implNote WA Web treats a missing field as a hard error,
                 * whereas Cobalt falls back to {@code false} so callers can
                 * decide how to handle the gap.
                 *
                 * @return {@code true} when the flag is present and set,
                 *         {@code false} otherwise
                 */
                public boolean isExistingGroup() {
                    return isExistingGroup != null && isExistingGroup;
                }

                /**
                 * Returns the hidden-group state tag.
                 *
                 * <p>The value is empty when the GraphQL object did not project
                 * a {@code hidden_group} field.
                 *
                 * @return an {@link Optional} containing the tag, or empty if
                 *         absent
                 */
                public Optional<String> hiddenGroup() {
                    return Optional.ofNullable(hiddenGroup);
                }

                /**
                 * Subject metadata for a suggested subgroup.
                 *
                 * <p>This carries the subject text value only; unlike
                 * {@link FetchAllSubgroupsMexResponse.SubGroups.Edges.Node.Subject},
                 * the suggestions document does not project a creation
                 * timestamp here.
                 */
                public static final class Subject {
                    /**
                     * Subject text value, or {@code null} when the GraphQL
                     * object did not project it.
                     *
                     * <p>The user-visible group title as the relay last
                     * recorded it.
                     */
                    private final String value;

                    /**
                     * Constructs a new subject record.
                     *
                     * @param value the subject text value
                     */
                    private Subject(String value) {
                        this.value = value;
                    }

                    /**
                     * Returns the subject text value.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code value} field.
                     *
                     * @return an {@link Optional} containing the value, or
                     *         empty if absent
                     */
                    public Optional<String> value() {
                        return Optional.ofNullable(value);
                    }

                    /**
                     * Parses a subject record from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed record,
                     *         or empty if {@code obj} is {@code null}
                     */
                    static Optional<Subject> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var value = obj.getString("value");
                        return Optional.of(new Subject(value));
                    }

                    /**
                     * Parses a list of subject records from the given JSON
                     * array, skipping {@code null} entries.
                     *
                     * <p>This is a symmetry helper for array-shaped subject
                     * data; it is currently unused at the call sites of this
                     * response.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed records, empty if {@code arr}
                     *         is {@code null}
                     */
                    static List<Subject> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<Subject>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Description metadata for a suggested subgroup.
                 *
                 * <p>This carries the description text and the description
                 * metadata identifier the relay assigns to each description
                 * change.
                 */
                public static final class Description {
                    /**
                     * Description text value, or {@code null} when the GraphQL
                     * object did not project it.
                     *
                     * <p>The user-visible group description as the relay last
                     * recorded it.
                     */
                    private final String value;

                    /**
                     * Description metadata identifier, or {@code null} when the
                     * GraphQL object did not project it.
                     *
                     * <p>Opaque identifier the relay assigns to each
                     * description revision; WA Web uses it to detect stale
                     * cached descriptions.
                     */
                    private final String id;

                    /**
                     * Constructs a new description record.
                     *
                     * @param value the description text value
                     * @param id    the description metadata identifier
                     */
                    private Description(String value, String id) {
                        this.value = value;
                        this.id = id;
                    }

                    /**
                     * Returns the description text value.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code value} field.
                     *
                     * @return an {@link Optional} containing the value, or
                     *         empty if absent
                     */
                    public Optional<String> value() {
                        return Optional.ofNullable(value);
                    }

                    /**
                     * Returns the description metadata identifier.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project an {@code id} field.
                     *
                     * @return an {@link Optional} containing the identifier, or
                     *         empty if absent
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Parses a description record from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed record,
                     *         or empty if {@code obj} is {@code null}
                     */
                    static Optional<Description> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var value = obj.getString("value");
                        var id = obj.getString("id");
                        return Optional.of(new Description(value, id));
                    }

                    /**
                     * Parses a list of description records from the given JSON
                     * array, skipping {@code null} entries.
                     *
                     * <p>This is a symmetry helper for array-shaped description
                     * data; it is currently unused at the call sites of this
                     * response.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed records, empty if {@code arr}
                     *         is {@code null}
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
                 * Creator metadata for a suggested subgroup.
                 *
                 * <p>This carries the creator WID; the suggestions document
                 * only projects the {@code id} field of the creator object.
                 */
                public static final class Creator {
                    /**
                     * Creator identifier, or {@code null} when the GraphQL
                     * object did not project it.
                     *
                     * <p>The creator's WhatsApp WID stringified.
                     */
                    private final String id;

                    /**
                     * Constructs a new creator record.
                     *
                     * @param id the creator identifier
                     */
                    private Creator(String id) {
                        this.id = id;
                    }

                    /**
                     * Returns the creator identifier.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project an {@code id} field.
                     *
                     * @return an {@link Optional} containing the identifier, or
                     *         empty if absent
                     */
                    public Optional<String> id() {
                        return Optional.ofNullable(id);
                    }

                    /**
                     * Parses a creator record from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed record,
                     *         or empty if {@code obj} is {@code null}
                     */
                    static Optional<Creator> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var id = obj.getString("id");
                        return Optional.of(new Creator(id));
                    }

                    /**
                     * Parses a list of creator records from the given JSON
                     * array, skipping {@code null} entries.
                     *
                     * <p>This is a symmetry helper for array-shaped creator
                     * data; it is currently unused at the call sites of this
                     * response.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed records, empty if {@code arr}
                     *         is {@code null}
                     */
                    static List<Creator> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<Creator>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Parses a suggested subgroup stanza from the given JSON object.
                 *
                 * @param obj the JSON object to parse
                 * @return an {@link Optional} containing the parsed stanza, or
                 *         empty if {@code obj} is {@code null}
                 */
                static Optional<Node> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var subject = Subject.of(obj.getJSONObject("subject")).orElse(null);
                    var description = Description.of(obj.getJSONObject("description")).orElse(null);
                    var creator = Creator.of(obj.getJSONObject("creator")).orElse(null);
                    var creationTime = obj.getLong("creation_time");
                    var totalParticipantsCount = obj.getLong("total_participants_count");
                    var isExistingGroup = obj.getBoolean("is_existing_group");
                    var hiddenGroup = obj.getString("hidden_group");
                    return Optional.of(new Node(id, subject, description, creator, creationTime, totalParticipantsCount, isExistingGroup, hiddenGroup));
                }

                /**
                 * Parses a list of suggested subgroup nodes from the given JSON
                 * array, skipping {@code null} entries.
                 *
                 * <p>This is a symmetry helper for array-shaped stanza data; it
                 * is currently unused at the call sites of this response.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed nodes, empty if {@code arr} is
                 *         {@code null}
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
             * Parses an edge wrapper from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed edge, or empty
             *         if {@code obj} is {@code null}
             */
            static Optional<Edges> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var node = Node.of(obj.getJSONObject("node")).orElse(null);
                return Optional.of(new Edges(node));
            }

            /**
             * Parses a list of edge wrappers from the given JSON array,
             * skipping {@code null} entries.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed edges, empty if {@code arr} is
             *         {@code null}
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
         * Parses a suggestions container from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed container, or empty
         *         if {@code obj} is {@code null}
         */
        static Optional<SubGroupSuggestions> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var edges = Edges.ofArray(obj.getJSONArray("edges"));
            return Optional.of(new SubGroupSuggestions(edges));
        }

        /**
         * Parses a list of suggestions containers from the given JSON array,
         * skipping {@code null} entries.
         *
         * <p>This is a symmetry helper for array-shaped container data; it is
         * currently unused at the call sites of this response.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed containers, empty if {@code arr} is
         *         {@code null}
         */
        static List<SubGroupSuggestions> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<SubGroupSuggestions>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parses the response from the raw JSON payload bytes.
     *
     * <p>This is only invoked via the {@link #of(Stanza)} entry point after the
     * IQ stanza has been unwrapped.
     *
     * @implNote This implementation requires the {@code data} and
     * {@code data.xwa2_group_query_by_id} envelopes to be present, matching
     * WA Web's pre-check before destructuring; a missing
     * {@code sub_group_suggestions} container collapses to {@code null} on the
     * response rather than raising, which is laxer than WA Web's server error
     * path so callers can decide how to surface the gap.
     *
     * @param json the raw JSON bytes from the {@code <result>} child
     * @return an {@link Optional} containing the parsed response, or empty if
     *         the envelope is missing
     */
    private static Optional<FetchSubgroupSuggestionsMexResponse> of(byte[] json) {
        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa2_group_query_by_id");
        if (root == null) {
            return Optional.empty();
        }

        var id = root.getString("id");
        var subGroupSuggestions = SubGroupSuggestions.of(root.getJSONObject("sub_group_suggestions")).orElse(null);

        return Optional.of(new FetchSubgroupSuggestionsMexResponse(id, subGroupSuggestions));
    }
}
