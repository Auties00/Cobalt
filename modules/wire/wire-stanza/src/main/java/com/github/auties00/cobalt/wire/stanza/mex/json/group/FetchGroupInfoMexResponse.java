package com.github.auties00.cobalt.wire.stanza.mex.json.group;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Inbound parsed response of the {@link FetchGroupInfoMexRequest} query, exposing the full
 * {@code xwa2_group_query_by_id} envelope returned by the relay.
 *
 * <p>This drives the group-info panel, chat header and chat-info sidebar. The four-way group typename
 * (regular group, community, default subgroup, subgroup) is not surfaced here; callers may infer it
 * from the populated property set.
 *
 * @implNote This implementation keeps the nested class hierarchy aligned with the GraphQL response
 * shape and exposes every scalar as an {@link Optional}, leaving cross-property derivation (such as
 * the suspended/terminated distinction folded out of the {@code state} scalar, or the typename
 * dispatch) to the caller.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchGroupInfoJob")
public final class FetchGroupInfoMexResponse implements MexStanza.Response.Json {
    /**
     * Group identifier scalar projected from {@code xwa2_group_query_by_id.id}.
     */
    private final String id;

    /**
     * Group creation timestamp in seconds since epoch projected from
     * {@code xwa2_group_query_by_id.creation_time}.
     */
    private final Long creationTime;

    /**
     * Parsed {@code creator} sub-object exposing the group founder's identifiers.
     */
    private final Creator creator;

    /**
     * Group lifecycle state scalar projected from {@code xwa2_group_query_by_id.state}; one of
     * {@code "ACTIVE"}, {@code "SUSPENDED"} or {@code "NON_EXISTENT"}.
     */
    private final String state;

    /**
     * Parsed {@code subject} sub-object exposing the current subject text and last-edit author.
     */
    private final Subject subject;

    /**
     * Parsed {@code description} sub-object exposing the current description text and last-edit
     * author.
     */
    private final Description description;

    /**
     * Parsed {@code participants} sub-object carrying the edge list and the
     * {@code participants_phash_match} flag.
     */
    private final Participants participants;

    /**
     * Total participant count scalar projected from
     * {@code xwa2_group_query_by_id.total_participants_count}.
     */
    private final Long totalParticipantsCount;

    /**
     * Flag scalar projected from {@code xwa2_group_query_by_id.missing_participant_identification},
     * indicating that the relay could not identify every participant.
     */
    private final String missingParticipantIdentification;

    /**
     * Parsed {@code properties} sub-object aggregating the configurable group properties.
     */
    private final Properties properties;

    /**
     * Membership-approval request flag scalar projected from
     * {@code xwa2_group_query_by_id.membership_approval_request}.
     */
    private final String membershipApprovalRequest;

    /**
     * Constructs a new response wrapping the parsed scalar and nested fields of the
     * {@code xwa2_group_query_by_id} envelope.
     *
     * <p>Instances are produced by the {@link #of(Stanza)} parser.
     *
     * @param id                                 the group identifier scalar
     * @param creationTime                       the group creation timestamp in seconds since epoch
     * @param creator                            the parsed {@code creator} sub-object
     * @param state                              the group {@code state} scalar
     * @param subject                            the parsed {@code subject} sub-object
     * @param description                        the parsed {@code description} sub-object
     * @param participants                       the parsed {@code participants} sub-object
     * @param totalParticipantsCount             the {@code total_participants_count} scalar
     * @param missingParticipantIdentification   the {@code missing_participant_identification} scalar
     * @param properties                         the parsed {@code properties} sub-object
     * @param membershipApprovalRequest          the {@code membership_approval_request} scalar
     */
    private FetchGroupInfoMexResponse(String id, Long creationTime, Creator creator, String state, Subject subject, Description description, Participants participants, Long totalParticipantsCount, String missingParticipantIdentification, Properties properties, String membershipApprovalRequest) {
        this.id = id;
        this.creationTime = creationTime;
        this.creator = creator;
        this.state = state;
        this.subject = subject;
        this.description = description;
        this.participants = participants;
        this.totalParticipantsCount = totalParticipantsCount;
        this.missingParticipantIdentification = missingParticipantIdentification;
        this.properties = properties;
        this.membershipApprovalRequest = membershipApprovalRequest;
    }

    /**
     * Parses the MEX response carried by an inbound IQ stanza.
     *
     * <p>This is the entry point for receivers handling the IQ reply of
     * {@link FetchGroupInfoMexRequest}. The returned value is {@link Optional#empty()} when the reply
     * lacks a {@code <result>} child or its JSON body cannot be parsed into the expected envelope.
     *
     * @param stanza the inbound IQ stanza carrying the {@code <result>} child
     * @return the parsed response, or {@link Optional#empty()} if the expected JSON shape is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchGroupInfoJob", exports = "mexGetGroupInfo",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<FetchGroupInfoMexResponse> of(Stanza stanza) {
        return stanza.getChild("result")
                .flatMap(Stanza::toContentBytes)
                .flatMap(FetchGroupInfoMexResponse::of);
    }

    /**
     * Returns the group identifier scalar.
     *
     * <p>This mirrors the {@code id} variable echoed by the relay; it is useful when correlating the
     * reply against a batched dispatch.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the group creation timestamp.
     *
     * <p>The relay's seconds-since-epoch value is adapted into a JDK {@link Instant}.
     *
     * @return an {@link Optional} containing the value as an {@link Instant}, or empty if absent
     */
    public Optional<Instant> creationTime() {
        return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
    }

    /**
     * Returns the parsed {@code creator} sub-object exposing the group founder's identifiers.
     *
     * <p>The sub-object carries the founder's default id, LID, phone number and username.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<Creator> creator() {
        return Optional.ofNullable(creator);
    }

    /**
     * Returns the group lifecycle state scalar.
     *
     * <p>The value is one of {@code "ACTIVE"}, {@code "SUSPENDED"} or {@code "NON_EXISTENT"}; the
     * latter two map to the suspended and terminated states of the group.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Returns the parsed {@code subject} sub-object.
     *
     * <p>The sub-object carries the current subject text, its last-edit timestamp and the editor's
     * identifiers.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<Subject> subject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Returns the parsed {@code description} sub-object.
     *
     * <p>The sub-object carries the current description text, its identifier, last-edit timestamp and
     * the editor's identifiers.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<Description> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the parsed {@code participants} sub-object.
     *
     * <p>The edge list carries human participants only; bots are excluded, and
     * {@link FetchGroupInfoIncludBotsMexResponse} includes them. The {@code participants_phash_match}
     * flag is carried alongside the edge list.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<Participants> participants() {
        return Optional.ofNullable(participants);
    }

    /**
     * Returns the total participant count scalar.
     *
     * <p>The relay reports this count even when the edge list is omitted (for example when
     * {@code participants_phash_match} is {@code true}), allowing UI surfaces to render the group
     * size without the full edge list.
     *
     * @return an {@link OptionalLong} containing the value, or empty if absent
     */
    public OptionalLong totalParticipantsCount() {
        return totalParticipantsCount != null ? OptionalLong.of(totalParticipantsCount) : OptionalLong.empty();
    }

    /**
     * Returns the {@code missing_participant_identification} scalar.
     *
     * <p>This surfaces the case where the relay could not identify every participant in the edge
     * list.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> missingParticipantIdentification() {
        return Optional.ofNullable(missingParticipantIdentification);
    }

    /**
     * Returns the parsed {@code properties} sub-object aggregating the configurable group properties.
     *
     * <p>The sub-object includes membership-approval flags, member-add mode, announcement-only mode,
     * ephemeral timer, parent-community link, LID migration state, growth lock, hidden-group flag and
     * more.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<Properties> properties() {
        return Optional.ofNullable(properties);
    }

    /**
     * Returns the {@code membership_approval_request} scalar.
     *
     * <p>This indicates whether the current user has a pending join request awaiting admin approval;
     * the relay surfaces the flag as a stringly-typed boolean.
     *
     * @return an {@link Optional} containing the value, or empty if absent
     */
    public Optional<String> membershipApprovalRequest() {
        return Optional.ofNullable(membershipApprovalRequest);
    }

    /**
     * Parsed projection of the top-level {@code creator} sub-object.
     *
     * <p>This surfaces the group founder's identifiers; the nested {@link UsernameInfo} carries the
     * username only when the relay projects it.
     */
    public static final class Creator {
        /**
         * Default identifier scalar.
         */
        private final String id;

        /**
         * LID identifier scalar.
         */
        private final String lid;

        /**
         * Phone-number identifier scalar.
         */
        private final String pn;

        /**
         * Parsed {@code username_info} sub-object.
         */
        private final UsernameInfo usernameInfo;

        /**
         * Constructs a new {@code Creator} wrapping the parsed scalar and nested fields.
         *
         * <p>Instances are produced by {@link #of(JSONObject)}.
         *
         * @param id           the default identifier scalar
         * @param lid          the LID identifier scalar
         * @param pn           the phone-number identifier scalar
         * @param usernameInfo the parsed {@code username_info} sub-object
         */
        private Creator(String id, String lid, String pn, UsernameInfo usernameInfo) {
            this.id = id;
            this.lid = lid;
            this.pn = pn;
            this.usernameInfo = usernameInfo;
        }

        /**
         * Returns the default identifier scalar.
         *
         * <p>This identifies the group owner.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the LID identifier scalar.
         *
         * <p>The value is present once the founder has migrated to LID addressing; it is absent on
         * legacy-only accounts.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> lid() {
            return Optional.ofNullable(lid);
        }

        /**
         * Returns the phone-number identifier scalar.
         *
         * <p>This identifies the founder by phone number.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> pn() {
            return Optional.ofNullable(pn);
        }

        /**
         * Returns the parsed {@code username_info} sub-object.
         *
         * <p>The value is present only when the relay projects usernames; it carries the founder's
         * username.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<UsernameInfo> usernameInfo() {
            return Optional.ofNullable(usernameInfo);
        }

        /**
         * Parsed projection of the {@code creator.username_info} sub-object.
         *
         * <p>This carries the founder's username scalar; it is absent when usernames are gated off at
         * relay-side.
         */
        public static final class UsernameInfo {
            /**
             * Username scalar projected from {@code creator.username_info.username}.
             */
            private final String username;

            /**
             * Constructs a new {@code UsernameInfo} wrapping the parsed {@code username} scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param username the username scalar
             */
            private UsernameInfo(String username) {
                this.username = username;
            }

            /**
             * Returns the username scalar.
             *
             * <p>This is the founder's username surfaced when usernames are projected.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> username() {
                return Optional.ofNullable(username);
            }

            /**
             * Parses a {@code UsernameInfo} from the given JSON object.
             *
             * <p>Called from {@link Creator#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<UsernameInfo> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var username = obj.getString("username");
                return Optional.of(new UsernameInfo(username));
            }

            /**
             * Parses a list of {@code UsernameInfo} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
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
         * Parses a {@code Creator} from the given JSON object.
         *
         * <p>Called from the top-level {@link FetchGroupInfoMexResponse#of(byte[])} parser.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
         */
        static Optional<Creator> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var lid = obj.getString("lid");
            var pn = obj.getString("pn");
            var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
            return Optional.of(new Creator(id, lid, pn, usernameInfo));
        }

        /**
         * Parses a list of {@code Creator} entries from the given JSON array.
         *
         * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed results, empty if {@code arr} is {@code null}
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
     * Parsed projection of the {@code subject} sub-object.
     *
     * <p>This carries the current subject text together with the last edit's author and timestamp.
     */
    public static final class Subject {
        /**
         * Parsed {@code creator} sub-object exposing the subject editor's identifiers.
         */
        private final Creator creator;

        /**
         * Subject edit timestamp in seconds since epoch projected from {@code subject.creation_time}.
         */
        private final Long creationTime;

        /**
         * Subject text scalar projected from {@code subject.value}.
         */
        private final String value;

        /**
         * Constructs a new {@code Subject} wrapping the parsed scalar and nested fields.
         *
         * <p>Instances are produced by {@link #of(JSONObject)}.
         *
         * @param creator      the parsed {@code creator} sub-object
         * @param creationTime the subject edit timestamp in seconds since epoch
         * @param value        the subject text scalar
         */
        private Subject(Creator creator, Long creationTime, String value) {
            this.creator = creator;
            this.creationTime = creationTime;
            this.value = value;
        }

        /**
         * Returns the parsed {@code creator} sub-object.
         *
         * <p>The sub-object carries the identifiers of the user who last edited the subject.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<Creator> creator() {
            return Optional.ofNullable(creator);
        }

        /**
         * Returns the subject edit timestamp.
         *
         * <p>The relay's seconds-since-epoch value is adapted into a JDK {@link Instant}.
         *
         * @return an {@link Optional} containing the value as an {@link Instant}, or empty if absent
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the subject text scalar.
         *
         * <p>This is the current group subject.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Parsed projection of the {@code subject.creator} sub-object.
         *
         * <p>This mirrors the top-level {@link FetchGroupInfoMexResponse.Creator} shape; the nested
         * class is declared here because the GraphQL schema inlines a separate copy under each parent
         * object.
         */
        public static final class Creator {
            /**
             * Default identifier scalar.
             */
            private final String id;

            /**
             * LID identifier scalar.
             */
            private final String lid;

            /**
             * Phone-number identifier scalar.
             */
            private final String pn;

            /**
             * Parsed {@code username_info} sub-object.
             */
            private final UsernameInfo usernameInfo;

            /**
             * Constructs a new {@code Creator} wrapping the parsed scalar and nested fields.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param id           the default identifier scalar
             * @param lid          the LID identifier scalar
             * @param pn           the phone-number identifier scalar
             * @param usernameInfo the parsed {@code username_info} sub-object
             */
            private Creator(String id, String lid, String pn, UsernameInfo usernameInfo) {
                this.id = id;
                this.lid = lid;
                this.pn = pn;
                this.usernameInfo = usernameInfo;
            }

            /**
             * Returns the default identifier scalar.
             *
             * <p>This identifies the user who last edited the subject.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the LID identifier scalar.
             *
             * <p>The value is present once the subject editor has migrated to LID addressing; it is
             * absent on legacy-only accounts.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> lid() {
                return Optional.ofNullable(lid);
            }

            /**
             * Returns the phone-number identifier scalar.
             *
             * <p>This identifies the subject editor by phone number.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> pn() {
                return Optional.ofNullable(pn);
            }

            /**
             * Returns the parsed {@code username_info} sub-object.
             *
             * <p>The value is present only when the relay projects usernames; it carries the editor's
             * username.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<UsernameInfo> usernameInfo() {
                return Optional.ofNullable(usernameInfo);
            }

            /**
             * Parsed projection of the {@code subject.creator.username_info} sub-object.
             *
             * <p>This carries the editor's username scalar; it is absent when usernames are gated off
             * at relay-side.
             */
            public static final class UsernameInfo {
                /**
                 * Username scalar projected from {@code subject.creator.username_info.username}.
                 */
                private final String username;

                /**
                 * Constructs a new {@code UsernameInfo} wrapping the parsed {@code username} scalar.
                 *
                 * <p>Instances are produced by {@link #of(JSONObject)}.
                 *
                 * @param username the username scalar
                 */
                private UsernameInfo(String username) {
                    this.username = username;
                }

                /**
                 * Returns the username scalar.
                 *
                 * <p>This is the subject editor's username surfaced when usernames are projected.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> username() {
                    return Optional.ofNullable(username);
                }

                /**
                 * Parses a {@code UsernameInfo} from the given JSON object.
                 *
                 * <p>Called from {@link Creator#of(JSONObject)}.
                 *
                 * @param obj the JSON object to parse
                 * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
                 */
                static Optional<UsernameInfo> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var username = obj.getString("username");
                    return Optional.of(new UsernameInfo(username));
                }

                /**
                 * Parses a list of {@code UsernameInfo} entries from the given JSON array.
                 *
                 * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed results, empty if {@code arr} is {@code null}
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
             * Parses a {@code Creator} from the given JSON object.
             *
             * <p>Called from {@link Subject#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<Creator> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var id = obj.getString("id");
                var lid = obj.getString("lid");
                var pn = obj.getString("pn");
                var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
                return Optional.of(new Creator(id, lid, pn, usernameInfo));
            }

            /**
             * Parses a list of {@code Creator} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
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
         * Parses a {@code Subject} from the given JSON object.
         *
         * <p>Called from the top-level {@link FetchGroupInfoMexResponse#of(byte[])} parser.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
         */
        static Optional<Subject> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var creator = Creator.of(obj.getJSONObject("creator")).orElse(null);
            var creationTime = obj.getLong("creation_time");
            var value = obj.getString("value");
            return Optional.of(new Subject(creator, creationTime, value));
        }

        /**
         * Parses a list of {@code Subject} entries from the given JSON array.
         *
         * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed results, empty if {@code arr} is {@code null}
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
     * Parsed projection of the {@code description} sub-object.
     *
     * <p>This carries the current description text together with the last edit's author and
     * timestamp.
     */
    public static final class Description {
        /**
         * Description identifier scalar projected from {@code description.id}.
         */
        private final String id;

        /**
         * Description edit timestamp in seconds since epoch projected from
         * {@code description.creation_time}.
         */
        private final Long creationTime;

        /**
         * Parsed {@code creator} sub-object exposing the description editor's identifiers.
         */
        private final Creator creator;

        /**
         * Description text scalar projected from {@code description.value}.
         */
        private final String value;

        /**
         * Constructs a new {@code Description} wrapping the parsed scalar and nested fields.
         *
         * <p>Instances are produced by {@link #of(JSONObject)}.
         *
         * @param id           the description identifier scalar
         * @param creationTime the description edit timestamp in seconds since epoch
         * @param creator      the parsed {@code creator} sub-object
         * @param value        the description text scalar
         */
        private Description(String id, Long creationTime, Creator creator, String value) {
            this.id = id;
            this.creationTime = creationTime;
            this.creator = creator;
            this.value = value;
        }

        /**
         * Returns the description identifier scalar.
         *
         * <p>The identifier is the description version, rotated on each edit.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        /**
         * Returns the description edit timestamp.
         *
         * <p>The relay's seconds-since-epoch value is adapted into a JDK {@link Instant}.
         *
         * @return an {@link Optional} containing the value as an {@link Instant}, or empty if absent
         */
        public Optional<Instant> creationTime() {
            return Optional.ofNullable(creationTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the parsed {@code creator} sub-object.
         *
         * <p>The sub-object carries the identifiers of the user who last edited the description.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<Creator> creator() {
            return Optional.ofNullable(creator);
        }

        /**
         * Returns the description text scalar.
         *
         * <p>This is the current group description.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Parsed projection of the {@code description.creator} sub-object.
         *
         * <p>This mirrors the top-level {@link FetchGroupInfoMexResponse.Creator} shape; the nested
         * class is declared here because the GraphQL schema inlines a separate copy under each parent
         * object.
         */
        public static final class Creator {
            /**
             * Default identifier scalar.
             */
            private final String id;

            /**
             * LID identifier scalar.
             */
            private final String lid;

            /**
             * Phone-number identifier scalar.
             */
            private final String pn;

            /**
             * Parsed {@code username_info} sub-object.
             */
            private final UsernameInfo usernameInfo;

            /**
             * Constructs a new {@code Creator} wrapping the parsed scalar and nested fields.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param id           the default identifier scalar
             * @param lid          the LID identifier scalar
             * @param pn           the phone-number identifier scalar
             * @param usernameInfo the parsed {@code username_info} sub-object
             */
            private Creator(String id, String lid, String pn, UsernameInfo usernameInfo) {
                this.id = id;
                this.lid = lid;
                this.pn = pn;
                this.usernameInfo = usernameInfo;
            }

            /**
             * Returns the default identifier scalar.
             *
             * <p>This identifies the user who last edited the description.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> id() {
                return Optional.ofNullable(id);
            }

            /**
             * Returns the LID identifier scalar.
             *
             * <p>The value is present once the description editor has migrated to LID addressing; it
             * is absent on legacy-only accounts.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> lid() {
                return Optional.ofNullable(lid);
            }

            /**
             * Returns the phone-number identifier scalar.
             *
             * <p>This identifies the description editor by phone number.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> pn() {
                return Optional.ofNullable(pn);
            }

            /**
             * Returns the parsed {@code username_info} sub-object.
             *
             * <p>The value is present only when the relay projects usernames; it carries the editor's
             * username.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<UsernameInfo> usernameInfo() {
                return Optional.ofNullable(usernameInfo);
            }

            /**
             * Parsed projection of the {@code description.creator.username_info} sub-object.
             *
             * <p>This carries the editor's username scalar; it is absent when usernames are gated off
             * at relay-side.
             */
            public static final class UsernameInfo {
                /**
                 * Username scalar projected from {@code description.creator.username_info.username}.
                 */
                private final String username;

                /**
                 * Constructs a new {@code UsernameInfo} wrapping the parsed {@code username} scalar.
                 *
                 * <p>Instances are produced by {@link #of(JSONObject)}.
                 *
                 * @param username the username scalar
                 */
                private UsernameInfo(String username) {
                    this.username = username;
                }

                /**
                 * Returns the username scalar.
                 *
                 * <p>This is the description editor's username surfaced when usernames are projected.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> username() {
                    return Optional.ofNullable(username);
                }

                /**
                 * Parses a {@code UsernameInfo} from the given JSON object.
                 *
                 * <p>Called from {@link Creator#of(JSONObject)}.
                 *
                 * @param obj the JSON object to parse
                 * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
                 */
                static Optional<UsernameInfo> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var username = obj.getString("username");
                    return Optional.of(new UsernameInfo(username));
                }

                /**
                 * Parses a list of {@code UsernameInfo} entries from the given JSON array.
                 *
                 * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed results, empty if {@code arr} is {@code null}
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
             * Parses a {@code Creator} from the given JSON object.
             *
             * <p>Called from {@link Description#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<Creator> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var id = obj.getString("id");
                var lid = obj.getString("lid");
                var pn = obj.getString("pn");
                var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
                return Optional.of(new Creator(id, lid, pn, usernameInfo));
            }

            /**
             * Parses a list of {@code Creator} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
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
         * Parses a {@code Description} from the given JSON object.
         *
         * <p>Called from the top-level {@link FetchGroupInfoMexResponse#of(byte[])} parser.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
         */
        static Optional<Description> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var id = obj.getString("id");
            var creationTime = obj.getLong("creation_time");
            var creator = Creator.of(obj.getJSONObject("creator")).orElse(null);
            var value = obj.getString("value");
            return Optional.of(new Description(id, creationTime, creator, value));
        }

        /**
         * Parses a list of {@code Description} entries from the given JSON array.
         *
         * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed results, empty if {@code arr} is {@code null}
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
     * Parsed projection of the {@code participants} sub-object.
     *
     * <p>This carries the edge list of human participants and the {@code participants_phash_match}
     * flag that signals the relay skipped the edge list because the caller's cache matched.
     */
    public static final class Participants {
        /**
         * Parsed participant edge list projected from {@code participants.edges}.
         */
        private final List<Edges> edges;

        /**
         * Participant-set hash match flag projected from
         * {@code participants.participants_phash_match}.
         */
        private final Boolean participantsPhashMatch;

        /**
         * Constructs a new {@code Participants} wrapping the parsed edge list and hash-match flag.
         *
         * <p>Instances are produced by {@link #of(JSONObject)}.
         *
         * @param edges                  the parsed edge list
         * @param participantsPhashMatch the participant-set hash match flag
         */
        private Participants(List<Edges> edges, Boolean participantsPhashMatch) {
            this.edges = edges;
            this.participantsPhashMatch = participantsPhashMatch;
        }

        /**
         * Returns the parsed participant edge list.
         *
         * <p>The list is empty when the relay omitted the edges, typically because
         * {@link #participantsPhashMatch()} is {@code true}.
         *
         * @return the list of edge entries, empty if absent
         */
        public List<Edges> edges() {
            return edges;
        }

        /**
         * Returns the participant-set hash match flag.
         *
         * <p>The result is {@code true} when the relay confirms the caller's locally-known
         * participant hash matches its own, in which case the edge list is intentionally omitted to
         * save bandwidth.
         *
         * @return {@code true} if the value is present and true, {@code false} otherwise
         */
        public boolean participantsPhashMatch() {
            return participantsPhashMatch != null && participantsPhashMatch;
        }

        /**
         * Parsed projection of a single {@code participants.edges} entry.
         *
         * <p>This wraps the edge's {@code stanza} (the participant details) and the {@code role} scalar.
         */
        public static final class Edges {
            /**
             * Parsed {@code stanza} sub-object exposing the participant's identifiers.
             */
            private final Node node;

            /**
             * Participant role scalar projected from {@code edges.role}.
             */
            private final String role;

            /**
             * Constructs a new {@code Edges} wrapping the parsed stanza and role scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param node the parsed {@code stanza} sub-object
             * @param role the participant role scalar
             */
            private Edges(Node node, String role) {
                this.node = node;
                this.role = role;
            }

            /**
             * Returns the parsed {@code stanza} sub-object.
             *
             * <p>The sub-object carries the participant's identifiers.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<Node> node() {
                return Optional.ofNullable(node);
            }

            /**
             * Returns the participant role scalar.
             *
             * <p>The value is one of {@code "ADMIN_MEMBER"} or {@code "SUPERADMIN_MEMBER"} for admins;
             * a non-admin member returns the default, typically {@code null} or an empty role.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> role() {
                return Optional.ofNullable(role);
            }

            /**
             * Parsed projection of a single {@code participants.edges.stanza} sub-object.
             *
             * <p>This carries the participant's identifiers and display fields; the bot-aware variant
             * (with an extra {@code jid} field) lives in {@link FetchGroupInfoIncludBotsMexResponse}.
             */
            public static final class Node {
                /**
                 * Default identifier scalar projected from {@code stanza.id}.
                 */
                private final String id;

                /**
                 * LID identifier scalar projected from {@code stanza.lid}.
                 */
                private final String lid;

                /**
                 * Phone-number identifier scalar projected from {@code stanza.pn}.
                 */
                private final String pn;

                /**
                 * Participant display name scalar projected from {@code stanza.display_name}.
                 */
                private final String displayName;

                /**
                 * Parsed {@code username_info} sub-object.
                 */
                private final UsernameInfo usernameInfo;

                /**
                 * Constructs a new {@code Stanza} wrapping the parsed scalar and nested fields.
                 *
                 * <p>Instances are produced by {@link #of(JSONObject)}.
                 *
                 * @param id           the default identifier scalar
                 * @param lid          the LID identifier scalar
                 * @param pn           the phone-number identifier scalar
                 * @param displayName  the display name scalar
                 * @param usernameInfo the parsed {@code username_info} sub-object
                 */
                private Node(String id, String lid, String pn, String displayName, UsernameInfo usernameInfo) {
                    this.id = id;
                    this.lid = lid;
                    this.pn = pn;
                    this.displayName = displayName;
                    this.usernameInfo = usernameInfo;
                }

                /**
                 * Returns the default identifier scalar.
                 *
                 * <p>This identifies the participant.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> id() {
                    return Optional.ofNullable(id);
                }

                /**
                 * Returns the LID identifier scalar.
                 *
                 * <p>The value is present once the participant has migrated to LID addressing; it is
                 * absent on legacy-only accounts.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> lid() {
                    return Optional.ofNullable(lid);
                }

                /**
                 * Returns the phone-number identifier scalar.
                 *
                 * <p>This identifies the participant by phone number.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> pn() {
                    return Optional.ofNullable(pn);
                }

                /**
                 * Returns the participant display name scalar.
                 *
                 * <p>This is the participant's display name.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<String> displayName() {
                    return Optional.ofNullable(displayName);
                }

                /**
                 * Returns the parsed {@code username_info} sub-object.
                 *
                 * <p>The value is present only when the relay projects usernames; it carries the
                 * participant's username.
                 *
                 * @return an {@link Optional} containing the value, or empty if absent
                 */
                public Optional<UsernameInfo> usernameInfo() {
                    return Optional.ofNullable(usernameInfo);
                }

                /**
                 * Parsed projection of the {@code stanza.username_info} sub-object.
                 *
                 * <p>This carries the participant's username scalar; it is absent when usernames are
                 * gated off at relay-side.
                 */
                public static final class UsernameInfo {
                    /**
                     * Username scalar projected from {@code stanza.username_info.username}.
                     */
                    private final String username;

                    /**
                     * Constructs a new {@code UsernameInfo} wrapping the parsed {@code username}
                     * scalar.
                     *
                     * <p>Instances are produced by {@link #of(JSONObject)}.
                     *
                     * @param username the username scalar
                     */
                    private UsernameInfo(String username) {
                        this.username = username;
                    }

                    /**
                     * Returns the username scalar.
                     *
                     * <p>This is the participant's username surfaced when usernames are projected.
                     *
                     * @return an {@link Optional} containing the value, or empty if absent
                     */
                    public Optional<String> username() {
                        return Optional.ofNullable(username);
                    }

                    /**
                     * Parses a {@code UsernameInfo} from the given JSON object.
                     *
                     * <p>Called from {@link Node#of(JSONObject)}.
                     *
                     * @param obj the JSON object to parse
                     * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
                     */
                    static Optional<UsernameInfo> of(JSONObject obj) {
                        if (obj == null) {
                            return Optional.empty();
                        }

                        var username = obj.getString("username");
                        return Optional.of(new UsernameInfo(username));
                    }

                    /**
                     * Parses a list of {@code UsernameInfo} entries from the given JSON array.
                     *
                     * <p>Array entries whose object form parses to {@link Optional#empty()} are
                     * skipped.
                     *
                     * @param arr the JSON array to parse
                     * @return the list of parsed results, empty if {@code arr} is {@code null}
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
                 * Parses a {@code Stanza} from the given JSON object.
                 *
                 * <p>Called from {@link Edges#of(JSONObject)}.
                 *
                 * @param obj the JSON object to parse
                 * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
                 */
                static Optional<Node> of(JSONObject obj) {
                    if (obj == null) {
                        return Optional.empty();
                    }

                    var id = obj.getString("id");
                    var lid = obj.getString("lid");
                    var pn = obj.getString("pn");
                    var displayName = obj.getString("display_name");
                    var usernameInfo = UsernameInfo.of(obj.getJSONObject("username_info")).orElse(null);
                    return Optional.of(new Node(id, lid, pn, displayName, usernameInfo));
                }

                /**
                 * Parses a list of {@code Stanza} entries from the given JSON array.
                 *
                 * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
                 *
                 * @param arr the JSON array to parse
                 * @return the list of parsed results, empty if {@code arr} is {@code null}
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
             * Parses an {@code Edges} entry from the given JSON object.
             *
             * <p>Called from {@link #ofArray(JSONArray)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<Edges> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var node = Node.of(obj.getJSONObject("node")).orElse(null);
                var role = obj.getString("role");
                return Optional.of(new Edges(node, role));
            }

            /**
             * Parses a list of {@code Edges} entries from the given JSON array.
             *
             * <p>Called by {@link Participants#of(JSONObject)} to unwrap the GraphQL connection edge
             * array; array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
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
         * Parses a {@code Participants} from the given JSON object.
         *
         * <p>Called from the top-level {@link FetchGroupInfoMexResponse#of(byte[])} parser.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
         */
        static Optional<Participants> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var edges = Edges.ofArray(obj.getJSONArray("edges"));
            var participantsPhashMatch = obj.getBoolean("participants_phash_match");
            return Optional.of(new Participants(edges, participantsPhashMatch));
        }

        /**
         * Parses a list of {@code Participants} entries from the given JSON array.
         *
         * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed results, empty if {@code arr} is {@code null}
         */
        static List<Participants> ofArray(JSONArray arr) {
            if (arr == null) {
                return List.of();
            }

            var result = new ArrayList<Participants>(arr.size());
            for (var i = 0; i < arr.size(); i++) {
                of(arr.getJSONObject(i)).ifPresent(result::add);
            }
            return result;
        }
    }

    /**
     * Parsed projection of the {@code properties} sub-object aggregating the configurable group
     * properties.
     *
     * <p>Each scalar maps onto a single group-metadata field. The sub-objects
     * ({@link LimitSharing}, {@link LidMigrationState}, {@link Ephemeral}, {@link GrowthLocked2})
     * mirror the GraphQL sub-shape because the relay nests their properties one level deeper than the
     * rest.
     */
    public static final class Properties {
        /**
         * The {@code allow_non_admin_sub_group_creation} flag scalar.
         */
        private final Boolean allowNonAdminSubGroupCreation;

        /**
         * The {@code closed_by_membership_approval_mode} scalar.
         */
        private final String closedByMembershipApprovalMode;

        /**
         * The {@code appeal_status} scalar carrying the moderation-appeal state.
         */
        private final String appealStatus;

        /**
         * The {@code appeal_update_time} timestamp in seconds since epoch recording the last
         * moderation-appeal state change.
         */
        private final Long appealUpdateTime;

        /**
         * Parsed {@code limit_sharing} sub-object.
         */
        private final LimitSharing limitSharing;

        /**
         * Parsed {@code lid_migration_state} sub-object.
         */
        private final LidMigrationState lidMigrationState;

        /**
         * Parsed {@code ephemeral} sub-object carrying the disappearing-messages timer.
         */
        private final Ephemeral ephemeral;

        /**
         * Parsed {@code growth_locked2} sub-object.
         */
        private final GrowthLocked2 growthLocked2;

        /**
         * The {@code member_add_mode} scalar; one of {@code "ADMIN_ADD"} or {@code "ALL_MEMBER_ADD"}.
         */
        private final String memberAddMode;

        /**
         * The {@code parent_group_jid} scalar linking a subgroup to its parent community.
         */
        private final String parentGroupJid;

        /**
         * The {@code group_safety_check} scalar.
         */
        private final String groupSafetyCheck;

        /**
         * The {@code allow_admin_reports} flag scalar.
         */
        private final Boolean allowAdminReports;

        /**
         * The {@code announcement} flag scalar.
         */
        private final String announcement;

        /**
         * The {@code locked} flag scalar carrying the restrict-mode indicator.
         */
        private final String locked;

        /**
         * The {@code member_link_mode} scalar.
         */
        private final String memberLinkMode;

        /**
         * The {@code member_share_group_history_mode} scalar.
         */
        private final String memberShareGroupHistoryMode;

        /**
         * The {@code membership_approval_mode_enabled} flag scalar.
         */
        private final Boolean membershipApprovalModeEnabled;

        /**
         * The {@code general_chat} flag scalar marking a subgroup as the community's general-chat
         * thread.
         */
        private final String generalChat;

        /**
         * The {@code auto_add_disabled} flag scalar.
         */
        private final Boolean autoAddDisabled;

        /**
         * The {@code hidden_group} flag scalar.
         */
        private final String hiddenGroup;

        /**
         * The {@code capi} flag scalar carrying Conversion-API attribution.
         */
        private final String capi;

        /**
         * The {@code support} flag scalar marking the group as a support channel.
         */
        private final String support;

        /**
         * Constructs a new {@code Properties} wrapping the parsed scalar and nested fields.
         *
         * <p>Instances are produced by {@link #of(JSONObject)}.
         *
         * @param allowNonAdminSubGroupCreation   the {@code allow_non_admin_sub_group_creation} scalar
         * @param closedByMembershipApprovalMode  the {@code closed_by_membership_approval_mode} scalar
         * @param appealStatus                    the {@code appeal_status} scalar
         * @param appealUpdateTime                the {@code appeal_update_time} timestamp in seconds since epoch
         * @param limitSharing                    the parsed {@code limit_sharing} sub-object
         * @param lidMigrationState               the parsed {@code lid_migration_state} sub-object
         * @param ephemeral                       the parsed {@code ephemeral} sub-object
         * @param growthLocked2                   the parsed {@code growth_locked2} sub-object
         * @param memberAddMode                   the {@code member_add_mode} scalar
         * @param parentGroupJid                  the {@code parent_group_jid} scalar
         * @param groupSafetyCheck                the {@code group_safety_check} scalar
         * @param allowAdminReports               the {@code allow_admin_reports} scalar
         * @param announcement                    the {@code announcement} scalar
         * @param locked                          the {@code locked} scalar
         * @param memberLinkMode                  the {@code member_link_mode} scalar
         * @param memberShareGroupHistoryMode     the {@code member_share_group_history_mode} scalar
         * @param membershipApprovalModeEnabled   the {@code membership_approval_mode_enabled} scalar
         * @param generalChat                     the {@code general_chat} scalar
         * @param autoAddDisabled                 the {@code auto_add_disabled} scalar
         * @param hiddenGroup                     the {@code hidden_group} scalar
         * @param capi                            the {@code capi} scalar
         * @param support                         the {@code support} scalar
         */
        private Properties(Boolean allowNonAdminSubGroupCreation, String closedByMembershipApprovalMode, String appealStatus, Long appealUpdateTime, LimitSharing limitSharing, LidMigrationState lidMigrationState, Ephemeral ephemeral, GrowthLocked2 growthLocked2, String memberAddMode, String parentGroupJid, String groupSafetyCheck, Boolean allowAdminReports, String announcement, String locked, String memberLinkMode, String memberShareGroupHistoryMode, Boolean membershipApprovalModeEnabled, String generalChat, Boolean autoAddDisabled, String hiddenGroup, String capi, String support) {
            this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
            this.closedByMembershipApprovalMode = closedByMembershipApprovalMode;
            this.appealStatus = appealStatus;
            this.appealUpdateTime = appealUpdateTime;
            this.limitSharing = limitSharing;
            this.lidMigrationState = lidMigrationState;
            this.ephemeral = ephemeral;
            this.growthLocked2 = growthLocked2;
            this.memberAddMode = memberAddMode;
            this.parentGroupJid = parentGroupJid;
            this.groupSafetyCheck = groupSafetyCheck;
            this.allowAdminReports = allowAdminReports;
            this.announcement = announcement;
            this.locked = locked;
            this.memberLinkMode = memberLinkMode;
            this.memberShareGroupHistoryMode = memberShareGroupHistoryMode;
            this.membershipApprovalModeEnabled = membershipApprovalModeEnabled;
            this.generalChat = generalChat;
            this.autoAddDisabled = autoAddDisabled;
            this.hiddenGroup = hiddenGroup;
            this.capi = capi;
            this.support = support;
        }

        /**
         * Returns the {@code allow_non_admin_sub_group_creation} flag.
         *
         * <p>The result is {@code true} when a community lets non-admins create subgroups.
         *
         * @return {@code true} if the value is present and true, {@code false} otherwise
         */
        public boolean allowNonAdminSubGroupCreation() {
            return allowNonAdminSubGroupCreation != null && allowNonAdminSubGroupCreation;
        }

        /**
         * Returns the {@code closed_by_membership_approval_mode} scalar.
         *
         * <p>This indicates whether the parent community is closed; the relay surfaces it as a
         * stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> closedByMembershipApprovalMode() {
            return Optional.ofNullable(closedByMembershipApprovalMode);
        }

        /**
         * Returns the {@code appeal_status} scalar.
         *
         * <p>This carries the state of any moderation appeal raised against the group.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> appealStatus() {
            return Optional.ofNullable(appealStatus);
        }

        /**
         * Returns the {@code appeal_update_time} timestamp.
         *
         * <p>The relay's seconds-since-epoch value, recording the last moderation-appeal state
         * change, is adapted into a JDK {@link Instant}.
         *
         * @return an {@link Optional} containing the value as an {@link Instant}, or empty if absent
         */
        public Optional<Instant> appealUpdateTime() {
            return Optional.ofNullable(appealUpdateTime).map(Instant::ofEpochSecond);
        }

        /**
         * Returns the parsed {@code limit_sharing} sub-object.
         *
         * <p>The sub-object carries the {@code limit_sharing_enabled} flag.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<LimitSharing> limitSharing() {
            return Optional.ofNullable(limitSharing);
        }

        /**
         * Returns the parsed {@code lid_migration_state} sub-object.
         *
         * <p>The sub-object carries the {@code addressing_mode} scalar that distinguishes LID from
         * phone-number addressing.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<LidMigrationState> lidMigrationState() {
            return Optional.ofNullable(lidMigrationState);
        }

        /**
         * Returns the parsed {@code ephemeral} sub-object carrying the disappearing-messages timer.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<Ephemeral> ephemeral() {
            return Optional.ofNullable(ephemeral);
        }

        /**
         * Returns the parsed {@code growth_locked2} sub-object.
         *
         * <p>The nested {@code locked} flag suspends invite-link growth when set.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<GrowthLocked2> growthLocked2() {
            return Optional.ofNullable(growthLocked2);
        }

        /**
         * Returns the {@code member_add_mode} scalar.
         *
         * <p>The value is one of {@code "ADMIN_ADD"} or {@code "ALL_MEMBER_ADD"}, controlling who may
         * add new members.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> memberAddMode() {
            return Optional.ofNullable(memberAddMode);
        }

        /**
         * Returns the {@code parent_group_jid} scalar.
         *
         * <p>The value is set on subgroups to point at the parent community JID.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> parentGroupJid() {
            return Optional.ofNullable(parentGroupJid);
        }

        /**
         * Returns the {@code group_safety_check} scalar.
         *
         * <p>This carries the group-safety pipeline verdict.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> groupSafetyCheck() {
            return Optional.ofNullable(groupSafetyCheck);
        }

        /**
         * Returns the {@code allow_admin_reports} flag.
         *
         * <p>The result is {@code true} when the group lets admins receive member reports.
         *
         * @return {@code true} if the value is present and true, {@code false} otherwise
         */
        public boolean allowAdminReports() {
            return allowAdminReports != null && allowAdminReports;
        }

        /**
         * Returns the {@code announcement} scalar.
         *
         * <p>When set, the group restricts new messages to admins only; the relay surfaces it as a
         * stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> announcement() {
            return Optional.ofNullable(announcement);
        }

        /**
         * Returns the {@code locked} scalar.
         *
         * <p>When set, the group restricts group-info edits to admins only; the relay surfaces it as
         * a stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> locked() {
            return Optional.ofNullable(locked);
        }

        /**
         * Returns the {@code member_link_mode} scalar.
         *
         * <p>This controls who may share the group's invite link.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> memberLinkMode() {
            return Optional.ofNullable(memberLinkMode);
        }

        /**
         * Returns the {@code member_share_group_history_mode} scalar.
         *
         * <p>This controls chat-history sharing on member joins.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> memberShareGroupHistoryMode() {
            return Optional.ofNullable(memberShareGroupHistoryMode);
        }

        /**
         * Returns the {@code membership_approval_mode_enabled} flag.
         *
         * <p>The result is {@code true} when joins require admin approval.
         *
         * @return {@code true} if the value is present and true, {@code false} otherwise
         */
        public boolean membershipApprovalModeEnabled() {
            return membershipApprovalModeEnabled != null && membershipApprovalModeEnabled;
        }

        /**
         * Returns the {@code general_chat} scalar.
         *
         * <p>When set, the subgroup is the community's auto-created general chat; the relay surfaces
         * it as a stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> generalChat() {
            return Optional.ofNullable(generalChat);
        }

        /**
         * Returns the {@code auto_add_disabled} flag.
         *
         * <p>The result is {@code true} when the general-chat subgroup will not auto-add new community
         * members.
         *
         * @return {@code true} if the value is present and true, {@code false} otherwise
         */
        public boolean autoAddDisabled() {
            return autoAddDisabled != null && autoAddDisabled;
        }

        /**
         * Returns the {@code hidden_group} scalar.
         *
         * <p>When set, the subgroup is hidden; the relay surfaces it as a stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> hiddenGroup() {
            return Optional.ofNullable(hiddenGroup);
        }

        /**
         * Returns the {@code capi} scalar.
         *
         * <p>When set, the group carries Conversion-API attribution; the relay surfaces it as a
         * stringly-typed boolean.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> capi() {
            return Optional.ofNullable(capi);
        }

        /**
         * Returns the {@code support} scalar.
         *
         * <p>When set, the group is a support channel with the corresponding chat-UI treatment.
         *
         * @return an {@link Optional} containing the value, or empty if absent
         */
        public Optional<String> support() {
            return Optional.ofNullable(support);
        }

        /**
         * Parsed projection of the {@code limit_sharing} sub-object.
         *
         * <p>This wraps the single {@code limit_sharing_enabled} flag; the GraphQL schema exposes a
         * sub-object to leave room for future per-property extensions.
         */
        public static final class LimitSharing {
            /**
             * The {@code limit_sharing_enabled} flag scalar.
             */
            private final Boolean limitSharingEnabled;

            /**
             * Constructs a new {@code LimitSharing} wrapping the parsed {@code limit_sharing_enabled}
             * flag scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param limitSharingEnabled the flag scalar
             */
            private LimitSharing(Boolean limitSharingEnabled) {
                this.limitSharingEnabled = limitSharingEnabled;
            }

            /**
             * Returns the {@code limit_sharing_enabled} flag.
             *
             * <p>The result is {@code true} when the group restricts cross-app sharing of its content.
             *
             * @return {@code true} if the value is present and true, {@code false} otherwise
             */
            public boolean limitSharingEnabled() {
                return limitSharingEnabled != null && limitSharingEnabled;
            }

            /**
             * Parses a {@code LimitSharing} from the given JSON object.
             *
             * <p>Called from {@link Properties#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<LimitSharing> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var limitSharingEnabled = obj.getBoolean("limit_sharing_enabled");
                return Optional.of(new LimitSharing(limitSharingEnabled));
            }

            /**
             * Parses a list of {@code LimitSharing} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
             */
            static List<LimitSharing> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<LimitSharing>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parsed projection of the {@code lid_migration_state} sub-object.
         *
         * <p>This wraps the single {@code addressing_mode} scalar, one of {@code "LID"} or
         * {@code "PHONE_NUMBER"}.
         */
        public static final class LidMigrationState {
            /**
             * The {@code addressing_mode} scalar.
             */
            private final String addressingMode;

            /**
             * Constructs a new {@code LidMigrationState} wrapping the parsed {@code addressing_mode}
             * scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param addressingMode the addressing-mode scalar
             */
            private LidMigrationState(String addressingMode) {
                this.addressingMode = addressingMode;
            }

            /**
             * Returns the {@code addressing_mode} scalar.
             *
             * <p>The value is one of {@code "LID"} or {@code "PHONE_NUMBER"}; {@code "LID"} marks the
             * group as using LID addressing.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> addressingMode() {
                return Optional.ofNullable(addressingMode);
            }

            /**
             * Parses a {@code LidMigrationState} from the given JSON object.
             *
             * <p>Called from {@link Properties#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<LidMigrationState> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var addressingMode = obj.getString("addressing_mode");
                return Optional.of(new LidMigrationState(addressingMode));
            }

            /**
             * Parses a list of {@code LidMigrationState} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
             */
            static List<LidMigrationState> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<LidMigrationState>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parsed projection of the {@code ephemeral} sub-object carrying the disappearing-messages
         * timer.
         *
         * <p>This wraps the {@code expiration_time_in_sec} scalar.
         */
        public static final class Ephemeral {
            /**
             * The {@code expiration_time_in_sec} scalar.
             */
            private final Long expirationTimeInSec;

            /**
             * Constructs a new {@code Ephemeral} wrapping the parsed {@code expiration_time_in_sec}
             * scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param expirationTimeInSec the expiration-time scalar in seconds
             */
            private Ephemeral(Long expirationTimeInSec) {
                this.expirationTimeInSec = expirationTimeInSec;
            }

            /**
             * Returns the {@code expiration_time_in_sec} value as a {@link Duration}.
             *
             * <p>The relay's seconds value is adapted into a JDK {@link Duration}.
             *
             * @return an {@link Optional} containing the value as a {@link Duration}, or empty if absent
             */
            public Optional<Duration> expirationTimeInSec() {
                return Optional.ofNullable(expirationTimeInSec).map(Duration::ofSeconds);
            }

            /**
             * Parses an {@code Ephemeral} from the given JSON object.
             *
             * <p>Called from {@link Properties#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<Ephemeral> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var expirationTimeInSec = obj.getLong("expiration_time_in_sec");
                return Optional.of(new Ephemeral(expirationTimeInSec));
            }

            /**
             * Parses a list of {@code Ephemeral} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
             */
            static List<Ephemeral> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<Ephemeral>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parsed projection of the {@code growth_locked2} sub-object.
         *
         * <p>This wraps the single {@code locked} flag; when set, invite-link growth is suspended.
         */
        public static final class GrowthLocked2 {
            /**
             * The {@code locked} flag scalar.
             */
            private final String locked;

            /**
             * Constructs a new {@code GrowthLocked2} wrapping the parsed {@code locked} scalar.
             *
             * <p>Instances are produced by {@link #of(JSONObject)}.
             *
             * @param locked the locked-flag scalar
             */
            private GrowthLocked2(String locked) {
                this.locked = locked;
            }

            /**
             * Returns the {@code locked} flag scalar.
             *
             * <p>When set, invite-link growth is suspended; the relay surfaces it as a stringly-typed
             * boolean.
             *
             * @return an {@link Optional} containing the value, or empty if absent
             */
            public Optional<String> locked() {
                return Optional.ofNullable(locked);
            }

            /**
             * Parses a {@code GrowthLocked2} from the given JSON object.
             *
             * <p>Called from {@link Properties#of(JSONObject)}.
             *
             * @param obj the JSON object to parse
             * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
             */
            static Optional<GrowthLocked2> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var locked = obj.getString("locked");
                return Optional.of(new GrowthLocked2(locked));
            }

            /**
             * Parses a list of {@code GrowthLocked2} entries from the given JSON array.
             *
             * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
             *
             * @param arr the JSON array to parse
             * @return the list of parsed results, empty if {@code arr} is {@code null}
             */
            static List<GrowthLocked2> ofArray(JSONArray arr) {
                if (arr == null) {
                    return List.of();
                }

                var result = new ArrayList<GrowthLocked2>(arr.size());
                for (var i = 0; i < arr.size(); i++) {
                    of(arr.getJSONObject(i)).ifPresent(result::add);
                }
                return result;
            }
        }

        /**
         * Parses a {@code Properties} from the given JSON object.
         *
         * <p>Called from the top-level {@link FetchGroupInfoMexResponse#of(byte[])} parser.
         *
         * @param obj the JSON object to parse
         * @return an {@link Optional} containing the parsed result, or empty if {@code obj} is {@code null}
         */
        static Optional<Properties> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var allowNonAdminSubGroupCreation = obj.getBoolean("allow_non_admin_sub_group_creation");
            var closedByMembershipApprovalMode = obj.getString("closed_by_membership_approval_mode");
            var appealStatus = obj.getString("appeal_status");
            var appealUpdateTime = obj.getLong("appeal_update_time");
            var limitSharing = LimitSharing.of(obj.getJSONObject("limit_sharing")).orElse(null);
            var lidMigrationState = LidMigrationState.of(obj.getJSONObject("lid_migration_state")).orElse(null);
            var ephemeral = Ephemeral.of(obj.getJSONObject("ephemeral")).orElse(null);
            var growthLocked2 = GrowthLocked2.of(obj.getJSONObject("growth_locked2")).orElse(null);
            var memberAddMode = obj.getString("member_add_mode");
            var parentGroupJid = obj.getString("parent_group_jid");
            var groupSafetyCheck = obj.getString("group_safety_check");
            var allowAdminReports = obj.getBoolean("allow_admin_reports");
            var announcement = obj.getString("announcement");
            var locked = obj.getString("locked");
            var memberLinkMode = obj.getString("member_link_mode");
            var memberShareGroupHistoryMode = obj.getString("member_share_group_history_mode");
            var membershipApprovalModeEnabled = obj.getBoolean("membership_approval_mode_enabled");
            var generalChat = obj.getString("general_chat");
            var autoAddDisabled = obj.getBoolean("auto_add_disabled");
            var hiddenGroup = obj.getString("hidden_group");
            var capi = obj.getString("capi");
            var support = obj.getString("support");
            return Optional.of(new Properties(allowNonAdminSubGroupCreation, closedByMembershipApprovalMode, appealStatus, appealUpdateTime, limitSharing, lidMigrationState, ephemeral, growthLocked2, memberAddMode, parentGroupJid, groupSafetyCheck, allowAdminReports, announcement, locked, memberLinkMode, memberShareGroupHistoryMode, membershipApprovalModeEnabled, generalChat, autoAddDisabled, hiddenGroup, capi, support));
        }

        /**
         * Parses a list of {@code Properties} entries from the given JSON array.
         *
         * <p>Array entries whose object form parses to {@link Optional#empty()} are skipped.
         *
         * @param arr the JSON array to parse
         * @return the list of parsed results, empty if {@code arr} is {@code null}
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
     * Parses the JSON payload carried by the {@code <result>} child into a
     * {@link FetchGroupInfoMexResponse}.
     *
     * <p>The {@code data.xwa2_group_query_by_id} envelope is walked, returning
     * {@link Optional#empty()} when any intermediate object is missing.
     *
     * @param json the UTF-8 encoded JSON payload
     * @return an {@link Optional} containing the parsed response, or empty if the
     *         {@code data.xwa2_group_query_by_id} envelope is absent
     */
    private static Optional<FetchGroupInfoMexResponse> of(byte[] json) {
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
        var creationTime = root.getLong("creation_time");
        var creator = Creator.of(root.getJSONObject("creator")).orElse(null);
        var state = root.getString("state");
        var subject = Subject.of(root.getJSONObject("subject")).orElse(null);
        var description = Description.of(root.getJSONObject("description")).orElse(null);
        var participants = Participants.of(root.getJSONObject("participants")).orElse(null);
        var totalParticipantsCount = root.getLong("total_participants_count");
        var missingParticipantIdentification = root.getString("missing_participant_identification");
        var properties = Properties.of(root.getJSONObject("properties")).orElse(null);
        var membershipApprovalRequest = root.getString("membership_approval_request");

        return Optional.of(new FetchGroupInfoMexResponse(id, creationTime, creator, state, subject, description, participants, totalParticipantsCount, missingParticipantIdentification, properties, membershipApprovalRequest));
    }
}
