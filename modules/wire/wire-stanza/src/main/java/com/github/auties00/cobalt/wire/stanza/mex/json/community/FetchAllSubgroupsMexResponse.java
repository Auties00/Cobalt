package com.github.auties00.cobalt.wire.stanza.mex.json.community;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Parsed response of the {@code mexFetchAllSubgroups} MEX query.
 *
 * <p>This response carries the community's default subgroup record together
 * with the list of regular subgroups, projected from
 * {@code data.xwa2_group_query_by_id}. It is the inbound counterpart of
 * {@link FetchAllSubgroupsMexRequest}.
 *
 * @implNote This implementation preserves the GraphQL connection shape
 * ({@code sub_groups.edges[].stanza}) verbatim rather than flattening it into a
 * single list, so callers can distinguish a missing {@code sub_groups}
 * container from an empty edges list. The default announcement subgroup and the
 * regular subgroups are kept on separate fields, mirroring how WA Web seeds the
 * announcement entry with {@code defaultSubgroup=true}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchAllSubgroupsJob")
public final class FetchAllSubgroupsMexResponse implements MexStanza.Response.Json {
    /**
     * Community group identifier returned by the relay, or {@code null} when
     * the relay omitted it.
     *
     * <p>Identifies the parent community that the default and regular
     * subgroups belong to; matches the {@code group_id} sent in the request.
     */
    private final String id;

    /**
     * Community's default subgroup record, or {@code null} when the relay
     * omitted it.
     *
     * <p>The default subgroup is the always-present announcement group
     * automatically attached to every community; WA Web processes it
     * separately from regular subgroups and tags it
     * {@code defaultSubgroup=true} downstream.
     */
    private final DefaultSubGroup defaultSubGroup;

    /**
     * List of regular subgroups under the community, or {@code null} when the
     * relay omitted it.
     *
     * <p>Each edge carries a subgroup id, subject metadata, property flags and
     * the pending approval-request counter.
     */
    private final SubGroups subGroups;

    /**
     * Constructs a new response with the given fields.
     *
     * @param id              the community group identifier
     * @param defaultSubGroup the default subgroup record
     * @param subGroups       the regular subgroups list
     */
    private FetchAllSubgroupsMexResponse(String id, DefaultSubGroup defaultSubGroup, SubGroups subGroups) {
        this.id = id;
        this.defaultSubGroup = defaultSubGroup;
        this.subGroups = subGroups;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling
     * {@code <iq xmlns="w:mex">} replies tagged with
     * {@link FetchAllSubgroupsMexRequest#QUERY_ID}. It unwraps the
     * {@code <result>} child, reads its content bytes and decodes the GraphQL
     * JSON envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected
     *         JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchAllSubgroupsJob", exports = "mexFetchAllSubgroups",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchAllSubgroupsMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchAllSubgroupsMexResponse::of);
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
     * Returns the community's default subgroup record.
     *
     * <p>The value is empty when the relay returned the community envelope
     * without the {@code default_sub_group} slot.
     *
     * @implNote WA Web treats a missing announcement group as a server error,
     * whereas Cobalt surfaces it as {@link Optional#empty()} and lets callers
     * decide whether to reject.
     *
     * @return an {@link Optional} containing the record, or empty if absent
     */
    public Optional<DefaultSubGroup> defaultSubGroup() {
        return Optional.ofNullable(defaultSubGroup);
    }

    /**
     * Returns the list of regular subgroups under the community.
     *
     * <p>The value is empty when the relay returned the community envelope
     * without the {@code sub_groups} container; the edges list inside may
     * itself be empty when the community has no regular subgroups.
     *
     * @return an {@link Optional} containing the subgroup list, or empty if
     *         absent
     */
    public Optional<SubGroups> subGroups() {
        return Optional.ofNullable(subGroups);
    }

    /**
     * Default subgroup record for a community.
     *
     * <p>This wraps the announcement subgroup automatically attached to a
     * community. It carries the subgroup identifier and the subject metadata;
     * unlike regular subgroups, no property flags or approval counters are
     * projected because the announcement subgroup's properties are fixed.
     */
    public static final class DefaultSubGroup {
        /**
         * Default subgroup identifier, or {@code null} when the relay omitted
         * it.
         *
         * <p>Mirrors the announcement subgroup's WhatsApp WID.
         */
        private final String id;

        /**
         * Default subgroup subject metadata, or {@code null} when the relay
         * omitted it.
         *
         * <p>Pairs the subject text with the creation timestamp.
         */
        private final Subject subject;

        /**
         * Constructs a new default-subgroup record.
         *
         * @param id      the subgroup identifier
         * @param subject the subject metadata
         */
        private DefaultSubGroup(String id, Subject subject) {
            this.id = id;
            this.subject = subject;
        }

        /**
         * Returns the default subgroup identifier.
         *
         * <p>The value is empty when the field is absent from a malformed relay
         * reply.
         *
         * @return an {@link Optional} containing the identifier, or empty if
         *         absent
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the default subgroup subject metadata.
         *
         * <p>The value is empty when the relay returned the subgroup envelope
         * without the {@code subject} GraphQL object.
         *
         * @return an {@link Optional} containing the subject, or empty if
         *         absent
         */
        public Optional<Subject> subject() {
            return Optional.ofNullable(subject);
        }

        /**
         * Subject metadata for the default subgroup.
         *
         * <p>This captures the subject text value and the epoch-second creation
         * timestamp the relay records when the subject is set.
         */
        public static final class Subject {
            /**
             * Subject text value, or {@code null} when the GraphQL object did
             * not project it.
             *
             * <p>The user-visible group title as the relay last recorded it.
             */
            private final String value;

            /**
             * Subject creation epoch-second timestamp, or {@code null} when the
             * GraphQL object did not project it.
             *
             * <p>The timestamp the relay assigned the most recent subject
             * change.
             */
            private final Long creationTime;

            /**
             * Constructs a new subject record.
             *
             * @param value        the subject text value
             * @param creationTime the creation epoch-second timestamp
             */
            private Subject(String value, Long creationTime) {
                this.value = value;
                this.creationTime = creationTime;
            }

            /**
             * Returns the subject text value.
             *
             * <p>The value is empty when the GraphQL object did not project a
             * {@code value} field.
             *
             * @return an {@link Optional} containing the value, or empty if
             *         absent
             */
            public Optional<String> value() {
                return Optional.ofNullable(value);
            }

            /**
             * Returns the subject creation timestamp.
             *
             * <p>The value is empty when the GraphQL object did not project a
             * {@code creation_time} field; present values are widened through
             * {@link Instant#ofEpochSecond(long)} from the wire epoch-second
             * long.
             *
             * @return an {@link Optional} containing the {@link Instant}, or
             *         empty if absent
             */
            public Optional<Instant> creationTime() {
                return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
            }

            /**
             * Parses a subject record from the given JSON object.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed record, or
             *         empty if {@code obj} is {@code null}
             */
            static Optional<Subject> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var value = obj.getString("value");
                var creationTime = obj.getLong("creation_time");
                return Optional.of(new Subject(value, creationTime));
            }

            /**
             * Parses a list of subject records from the given JSON array,
             * skipping {@code null} entries.
             *
             * <p>This is a symmetry helper for array-shaped subject data; it is
             * currently unused at the call sites of this response, but is kept
             * because the WA Web GraphQL shape can project subjects as arrays
             * elsewhere.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed records, empty if {@code arr} is
             *         {@code null}
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
         * Parses a default-subgroup record from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed record, or empty if
         *         {@code obj} is {@code null}
         */
        static Optional<DefaultSubGroup> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var subject = Subject.of(obj.getJSONObject("subject")).orElse(null);
            return Optional.of(new DefaultSubGroup(id, subject));
        }

        /**
         * Parses a list of default-subgroup records from the given JSON array,
         * skipping {@code null} entries.
         *
         * <p>This is a symmetry helper for array-shaped default-subgroup data;
         * it is currently unused at the call sites of this response.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed records, empty if {@code arr} is
         *         {@code null}
         */
        static List<DefaultSubGroup> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<DefaultSubGroup>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Subgroups list wrapping the {@code edges} array of regular subgroups
     * under a community.
     *
     * <p>This type preserves the GraphQL connection shape; each {@link Edges}
     * entry wraps a single subgroup {@link Edges.Node}.
     */
    public static final class SubGroups {
        /**
         * Subgroup edges returned by the relay, never {@code null} once
         * {@link SubGroups#of(JSONObject)} has succeeded.
         *
         * <p>Mirrors the {@code sub_groups.edges} GraphQL array.
         */
        private final List<Edges> edges;

        /**
         * Constructs a new subgroups list.
         *
         * @param edges the subgroup edges
         */
        private SubGroups(List<Edges> edges) {
            this.edges = edges;
        }

        /**
         * Returns the subgroup edges.
         *
         * <p>Iterate this list to walk the regular subgroups; each entry
         * exposes a {@link Edges#node()} accessor that may be empty when the
         * relay returned a malformed edge.
         *
         * @return the list of edges, empty if absent
         */
        public List<Edges> edges() {
            return edges;
        }

        /**
         * Single edge wrapper around a subgroup stanza, mirroring the GraphQL
         * connection pattern.
         *
         * <p>Each edge wraps exactly one {@link Node}; the edge level exists to
         * leave room for future GraphQL cursor metadata without breaking the
         * binary shape.
         */
        public static final class Edges {
            /**
             * Subgroup stanza carried by the edge, or {@code null} when the relay
             * returned an edge envelope without a {@code stanza} child.
             */
            private final Node node;

            /**
             * Constructs a new edge wrapping the given stanza.
             *
             * @param node the subgroup stanza
             */
            private Edges(Node node) {
                this.node = node;
            }

            /**
             * Returns the subgroup stanza carried by this edge.
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
             * Subgroup stanza carrying the identifier together with subject
             * metadata, group properties and the pending membership
             * approval-request counter.
             *
             * <p>There is one such stanza per regular subgroup under the
             * community; it populates the community subgroup store.
             */
            public static final class Node {
                /**
                 * Subgroup identifier, or {@code null} when the relay omitted
                 * it.
                 *
                 * <p>The subgroup's WhatsApp WID stringified.
                 */
                private final String id;

                /**
                 * Subgroup subject metadata, or {@code null} when the relay
                 * omitted it.
                 *
                 * <p>Pairs the subject text with the creation timestamp.
                 */
                private final Subject subject;

                /**
                 * Subgroup property flags, or {@code null} when the relay
                 * omitted them.
                 *
                 * <p>Capture the general-chat tag, the membership-approval-mode
                 * flag and the hidden-group state.
                 */
                private final Properties properties;

                /**
                 * Pending membership-approval-request counter, or {@code null}
                 * when the relay omitted it.
                 *
                 * <p>Non-zero when the subgroup has at least one pending join
                 * request that the current user can act on.
                 */
                private final MembershipApprovalRequests membershipApprovalRequests;

                /**
                 * Constructs a new subgroup stanza.
                 *
                 * @param id                         the subgroup identifier
                 * @param subject                    the subject metadata
                 * @param properties                 the subgroup properties
                 * @param membershipApprovalRequests the pending membership
                 *                                   approval requests counter
                 */
                private Node(String id, Subject subject, Properties properties, MembershipApprovalRequests membershipApprovalRequests) {
                    this.id = id;
                    this.subject = subject;
                    this.properties = properties;
                    this.membershipApprovalRequests = membershipApprovalRequests;
                }

                /**
                 * Returns the subgroup identifier.
                 *
                 * <p>The value is empty when the relay omitted the {@code id}
                 * field from this subgroup stanza.
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
                 * Returns the subgroup properties.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code properties} GraphQL object; callers that need a
                 * specific flag should fall back to {@code false} on empty.
                 *
                 * @return an {@link Optional} containing the properties, or
                 *         empty if absent
                 */
                public Optional<Properties> properties() {
                    return Optional.ofNullable(properties);
                }

                /**
                 * Returns the pending membership-approval-request counter.
                 *
                 * <p>The value is empty when the relay omitted the
                 * {@code membership_approval_requests} GraphQL object; callers
                 * should treat empty as zero.
                 *
                 * @return an {@link Optional} containing the counter record, or
                 *         empty if absent
                 */
                public Optional<MembershipApprovalRequests> membershipApprovalRequests() {
                    return Optional.ofNullable(membershipApprovalRequests);
                }

                /**
                 * Subject metadata for a regular subgroup.
                 *
                 * <p>This mirrors the {@link DefaultSubGroup.Subject} shape; it
                 * is kept as a separate type to scope it under the subgroup
                 * {@link Node} for the GraphQL connection model.
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
                     * Subject creation epoch-second timestamp, or {@code null}
                     * when the GraphQL object did not project it.
                     *
                     * <p>The timestamp the relay assigned the most recent
                     * subject change.
                     */
                    private final Long creationTime;

                    /**
                     * Constructs a new subject record.
                     *
                     * @param value        the subject text value
                     * @param creationTime the creation epoch-second timestamp
                     */
                    private Subject(String value, Long creationTime) {
                        this.value = value;
                        this.creationTime = creationTime;
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
                     * Returns the subject creation timestamp.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code creation_time} field; present values are
                     * widened through {@link Instant#ofEpochSecond(long)}.
                     *
                     * @return an {@link Optional} containing the
                     *         {@link Instant}, or empty if absent
                     */
                    public Optional<Instant> creationTime() {
                        return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
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
                        var creationTime = obj.getLong("creation_time");
                        return Optional.of(new Subject(value, creationTime));
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
                 * Subgroup property flags.
                 *
                 * <p>These capture the three flags carried per subgroup: the
                 * general-chat tag, the membership-approval-mode toggle and the
                 * hidden-group state.
                 */
                public static final class Properties {
                    /**
                     * General-chat tag for the subgroup, or {@code null} when
                     * the GraphQL object did not project it.
                     *
                     * <p>Sourced from the {@code general_chat} GraphQL scalar;
                     * WA Web treats any non-null value as truthy in the
                     * downstream {@code generalSubgroup} projection.
                     */
                    private final String generalChat;

                    /**
                     * Whether membership-approval mode is enabled, or
                     * {@code null} when the GraphQL object did not project it.
                     *
                     * <p>When {@code true}, new members need admin approval
                     * before joining.
                     */
                    private final Boolean membershipApprovalModeEnabled;

                    /**
                     * Hidden-group state tag, or {@code null} when the GraphQL
                     * object did not project it.
                     *
                     * <p>Sourced from the {@code hidden_group} GraphQL scalar;
                     * WA Web treats any non-null value as truthy in the
                     * downstream {@code hiddenSubgroup} projection.
                     */
                    private final String hiddenGroup;

                    /**
                     * Constructs a new properties record.
                     *
                     * @param generalChat                   the general-chat tag
                     * @param membershipApprovalModeEnabled whether approval mode
                     *                                      is enabled
                     * @param hiddenGroup                   the hidden-group
                     *                                      state tag
                     */
                    private Properties(String generalChat, Boolean membershipApprovalModeEnabled, String hiddenGroup) {
                        this.generalChat = generalChat;
                        this.membershipApprovalModeEnabled = membershipApprovalModeEnabled;
                        this.hiddenGroup = hiddenGroup;
                    }

                    /**
                     * Returns the general-chat tag for the subgroup.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code general_chat} field.
                     *
                     * @return an {@link Optional} containing the tag, or empty
                     *         if absent
                     */
                    public Optional<String> generalChat() {
                        return Optional.ofNullable(generalChat);
                    }

                    /**
                     * Returns whether membership-approval mode is enabled for
                     * this subgroup.
                     *
                     * <p>Returns {@code false} when the relay omitted the
                     * field, matching WA Web's
                     * {@code membership_approval_mode_enabled ?? false}
                     * fallback.
                     *
                     * @return {@code true} when the flag is present and set,
                     *         {@code false} otherwise
                     */
                    public boolean membershipApprovalModeEnabled() {
                        return membershipApprovalModeEnabled != null && membershipApprovalModeEnabled;
                    }

                    /**
                     * Returns the hidden-group state tag.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code hidden_group} field.
                     *
                     * @return an {@link Optional} containing the tag, or empty
                     *         if absent
                     */
                    public Optional<String> hiddenGroup() {
                        return Optional.ofNullable(hiddenGroup);
                    }

                    /**
                     * Parses a properties record from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed record,
                     *         or empty if {@code obj} is {@code null}
                     */
                    static Optional<Properties> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var generalChat = obj.getString("general_chat");
                        var membershipApprovalModeEnabled = obj.getBoolean("membership_approval_mode_enabled");
                        var hiddenGroup = obj.getString("hidden_group");
                        return Optional.of(new Properties(generalChat, membershipApprovalModeEnabled, hiddenGroup));
                    }

                    /**
                     * Parses a list of properties records from the given JSON
                     * array, skipping {@code null} entries.
                     *
                     * <p>This is a symmetry helper for array-shaped property
                     * data; it is currently unused at the call sites of this
                     * response.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed records, empty if {@code arr}
                     *         is {@code null}
                     */
                    static List<Properties> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<Properties>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Counter record for pending membership-approval requests.
                 *
                 * <p>This drives the badge on the community subgroup row when
                 * approvals are pending; WA Web maps a non-zero
                 * {@code total_count} to a boolean downstream.
                 */
                public static final class MembershipApprovalRequests {
                    /**
                     * Number of pending approval requests, or {@code null} when
                     * the relay did not project the field.
                     *
                     * <p>A {@code null} value is distinct from a zero count.
                     */
                    private final Long totalCount;

                    /**
                     * Constructs a new counter record.
                     *
                     * @param totalCount the number of pending approval requests
                     */
                    private MembershipApprovalRequests(Long totalCount) {
                        this.totalCount = totalCount;
                    }

                    /**
                     * Returns the number of pending approval requests.
                     *
                     * <p>The value is empty when the GraphQL object did not
                     * project a {@code total_count} field; callers that need a
                     * primitive can fall back to {@code 0}.
                     *
                     * @return an {@link OptionalLong} containing the count, or
                     *         empty if absent
                     */
                    public OptionalLong totalCount() {
                        return totalCount != null ? OptionalLong.of(totalCount) : OptionalLong.empty();
                    }

                    /**
                     * Parses a counter record from the given JSON object.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed record,
                     *         or empty if {@code obj} is {@code null}
                     */
                    static Optional<MembershipApprovalRequests> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var totalCount = obj.getLong("total_count");
                        return Optional.of(new MembershipApprovalRequests(totalCount));
                    }

                    /**
                     * Parses a list of counter records from the given JSON
                     * array, skipping {@code null} entries.
                     *
                     * <p>This is a symmetry helper for array-shaped counter
                     * data; it is currently unused at the call sites of this
                     * response.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed records, empty if {@code arr}
                     *         is {@code null}
                     */
                    static List<MembershipApprovalRequests> ofArray(JSONArray arr) {
                        if (arr == null) {
                            return List.of();
                        }

                        var result = new ArrayList<MembershipApprovalRequests>(arr.size());
                        for (var i = 0; i < arr.size(); i++) {
                            of(arr.getJSONObject(i)).ifPresent(result::add);
                        }
                        return result;
                    }
                }

                /**
                 * Parses a subgroup stanza from the given JSON object.
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
                    var properties = Properties.of(obj.getJSONObject("properties")).orElse(null);
                    var membershipApprovalRequests = MembershipApprovalRequests.of(obj.getJSONObject("membership_approval_requests")).orElse(null);
                    return Optional.of(new Node(id, subject, properties, membershipApprovalRequests));
                }

                /**
                 * Parses a list of subgroup nodes from the given JSON array,
                 * skipping {@code null} entries.
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
         * Parses a subgroups list from the given JSON object.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed list, or empty if
         *         {@code obj} is {@code null}
         */
        static Optional<SubGroups> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var edges = Edges.ofArray(obj.getJSONArray("edges"));
            return Optional.of(new SubGroups(edges));
        }

        /**
         * Parses a list of subgroups containers from the given JSON array,
         * skipping {@code null} entries.
         *
         * <p>This is a symmetry helper for array-shaped container data; it is
         * currently unused at the call sites of this response.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed containers, empty if {@code arr} is
         *         {@code null}
         */
        static List<SubGroups> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<SubGroups>(arr.size());
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
     * WA Web's pre-check before destructuring; missing inner fields
     * ({@code default_sub_group}, {@code sub_groups}) collapse to {@code null}
     * on the response rather than raising, which is laxer than WA Web's server
     * error path so callers can decide how to surface the gap.
     *
     * @param json the raw JSON bytes from the {@code <result>} child
     * @return an {@link Optional} containing the parsed response, or empty if
     *         the envelope is missing
     */
    private static Optional<FetchAllSubgroupsMexResponse> of(byte[] json) {
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
        var defaultSubGroup = DefaultSubGroup.of(root.getJSONObject("default_sub_group")).orElse(null);
        var subGroups = SubGroups.of(root.getJSONObject("sub_groups")).orElse(null);

        return Optional.of(new FetchAllSubgroupsMexResponse(id, defaultSubGroup, subGroups));
    }
}
