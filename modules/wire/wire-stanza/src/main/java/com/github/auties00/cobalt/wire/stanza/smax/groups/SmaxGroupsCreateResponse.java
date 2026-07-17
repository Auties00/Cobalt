package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sealed reply family for a {@link SmaxGroupsCreateRequest}.
 *
 * The four variants partition every reply the relay can return: {@link Success} carries the provisioned group's
 * metadata and seed participants; {@link GroupAlreadyExists} surfaces the dedup-driven path where the
 * creator-plus-token tuple already maps to an existing group; {@link ClientError} and {@link ServerError} surface
 * caller-side and relay-side failures. Callers obtain the right variant by passing the inbound IQ to
 * {@link #of(Stanza, Stanza)}.
 */
public sealed interface SmaxGroupsCreateResponse extends SmaxStanza.Response
        permits SmaxGroupsCreateResponse.Success, SmaxGroupsCreateResponse.GroupAlreadyExists,
                SmaxGroupsCreateResponse.ClientError, SmaxGroupsCreateResponse.ServerError {

    /**
     * Parses the inbound IQ stanza into the first matching {@link SmaxGroupsCreateResponse} variant.
     *
     * The variant probes run in priority order: {@link Success}, {@link GroupAlreadyExists}, {@link ClientError},
     * {@link ServerError}. An empty {@link Optional} signals a stanza shape outside the documented union.
     *
     * @implNote This implementation does not throw a parsing-failure exception, leaving the recovery decision to
     * the caller.
     *
     * @param stanza    the inbound IQ stanza received from the relay
     * @param request the original outbound {@link SmaxGroupsCreateRequest} stanza, used to validate the echoed
     *                {@code id} attribute
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsCreateRPC",
            exports = "sendCreateRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsCreateResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var alreadyExists = GroupAlreadyExists.of(stanza, request);
        if (alreadyExists.isPresent()) {
            return alreadyExists;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Reply variant returned when the relay materialised the new group and echoed its provisioned metadata.
     *
     * Carries the identity triple ({@link #groupId()}, {@link #groupCreator()}, {@link #groupCreation()}), the
     * optional sync-token and sync-owner pair, every echoed policy marker, and the non-empty list of
     * seed-participant rows.
     *
     * @implNote This implementation surfaces the raw {@code <group/>} sub-stanza via {@link #group()} so callers can
     * read mixin metadata (addressing mode, subject-owner identity, member-add, member-link and
     * member-share-history mixins, dedup attribute echo) that Cobalt does not project as typed accessors.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateResponseSuccess")
    final class Success implements SmaxGroupsCreateResponse {
        /**
         * Holds the new group's user-component id (the portion of the JID preceding {@code @g.us}).
         */
        private final String groupId;

        /**
         * Holds the fully-qualified group {@link Jid}, derived from {@code <groupId>@g.us}.
         */
        private final Jid groupJid;

        /**
         * Holds the creator {@link Jid} that initiated the group.
         */
        private final Jid groupCreator;

        /**
         * Holds the creation timestamp in seconds since epoch.
         */
        private final long groupCreation;

        /**
         * Holds the optional {@code s_t} sync-time mixin echoed by the relay; {@code null} when omitted.
         */
        private final Long groupSyncTime;

        /**
         * Holds the optional {@code s_o} sync-owner {@link Jid} echoed by the relay; {@code null} when omitted.
         */
        private final Jid groupSyncOwner;

        /**
         * Holds the group's subject (display name) echoed by the relay.
         */
        private final String subject;

        /**
         * Holds the optional description id echoed inside the {@code <description id="...">} attribute;
         * {@code null} when the relay did not commit a description.
         */
        private final String descriptionId;

        /**
         * Holds the optional description-error string echoed inside the {@code <description error="...">}
         * attribute (e.g. {@code "406"} or {@code "500"}); {@code null} when the description committed cleanly.
         */
        private final String descriptionError;

        /**
         * Indicates whether the relay echoed a {@code <locked/>} child marking chat-info edits admin-only.
         */
        private final boolean locked;

        /**
         * Indicates whether the relay echoed an {@code <announcement/>} child restricting posting to admins.
         */
        private final boolean announcement;

        /**
         * Indicates whether the relay echoed a {@code <parent/>} child marking the new group as a community
         * parent.
         */
        private final boolean parent;

        /**
         * Indicates whether the relay echoed a {@code <no_frequently_forwarded/>} child.
         */
        private final boolean noFrequentlyForwarded;

        /**
         * Holds the optional {@code <ephemeral expiration="...">} attribute echoed by the relay; {@code null} when
         * no ephemeral expiration was committed.
         */
        private final Integer ephemeralExpiration;

        /**
         * Holds the optional {@code <ephemeral trigger="...">} attribute echoed by the relay; {@code null} when
         * omitted.
         */
        private final Integer ephemeralTrigger;

        /**
         * Indicates whether the relay echoed a {@code <membership_approval_mode/>} child.
         */
        private final boolean membershipApprovalMode;

        /**
         * Indicates whether the relay echoed a {@code <breakout/>} child marking the group as a breakout
         * sub-group.
         */
        private final boolean breakout;

        /**
         * Holds the optional linked parent community {@link Jid} echoed inside a {@code <linked_parent jid="...">}
         * child; {@code null} when the new group has no parent.
         */
        private final Jid linkedParentJid;

        /**
         * Indicates whether the relay echoed a {@code <hidden_group/>} child hiding the group from the community
         * directory.
         */
        private final boolean hiddenGroup;

        /**
         * Indicates whether the relay echoed an {@code <allow_non_admin_sub_group_creation/>} child.
         */
        private final boolean allowNonAdminSubGroupCreation;

        /**
         * Indicates whether the relay echoed a {@code <group_history/>} child.
         */
        private final boolean groupHistory;

        /**
         * Indicates whether the relay echoed a {@code <capi/>} child.
         */
        private final boolean capi;

        /**
         * Holds the non-empty list of seed-participant echo rows, one per requested participant.
         */
        private final List<ResponseParticipant> participants;

        /**
         * Holds the verbatim {@code <group/>} child exposed for callers that need to read mixin metadata
         * (addressing mode, subject-owner identity, member-add, member-link and member-share-history mixins, dedup
         * attribute echo) that Cobalt does not project as typed accessors.
         */
        private final Stanza group;

        /**
         * Constructs a success variant.
         *
         * The supplied participant list is defensively copied. Direct construction is primarily used to seed test
         * fixtures; most callers obtain instances via {@link #of(Stanza, Stanza)}.
         *
         * @param groupId                       the user-component id; never {@code null}
         * @param groupJid                      the full group {@link Jid}; never {@code null}
         * @param groupCreator                  the creator {@link Jid}; never {@code null}
         * @param groupCreation                 the creation timestamp in seconds since epoch
         * @param groupSyncTime                 the optional {@code s_t} mixin; may be {@code null}
         * @param groupSyncOwner                the optional {@code s_o} mixin; may be {@code null}
         * @param subject                       the subject; never {@code null}
         * @param descriptionId                 the optional description id; may be {@code null}
         * @param descriptionError              the optional description-error string; may be {@code null}
         * @param locked                        whether {@code <locked/>} was echoed
         * @param announcement                  whether {@code <announcement/>} was echoed
         * @param parent                        whether {@code <parent/>} was echoed
         * @param noFrequentlyForwarded         whether {@code <no_frequently_forwarded/>} was echoed
         * @param ephemeralExpiration           the optional ephemeral expiration; may be {@code null}
         * @param ephemeralTrigger              the optional ephemeral trigger; may be {@code null}
         * @param membershipApprovalMode        whether {@code <membership_approval_mode/>} was echoed
         * @param breakout                      whether {@code <breakout/>} was echoed
         * @param linkedParentJid               the optional linked parent community JID; may be {@code null}
         * @param hiddenGroup                   whether {@code <hidden_group/>} was echoed
         * @param allowNonAdminSubGroupCreation whether {@code <allow_non_admin_sub_group_creation/>} was echoed
         * @param groupHistory                  whether {@code <group_history/>} was echoed
         * @param capi                          whether {@code <capi/>} was echoed
         * @param participants                  the seed-participant rows; never {@code null} and must be non-empty
         * @param group                         the raw {@code <group/>} sub-stanza; never {@code null}
         * @throws NullPointerException     if any non-nullable argument is {@code null}
         * @throws IllegalArgumentException if {@code participants} is empty
         */
        public Success(String groupId,
                       Jid groupJid,
                       Jid groupCreator,
                       long groupCreation,
                       Long groupSyncTime,
                       Jid groupSyncOwner,
                       String subject,
                       String descriptionId,
                       String descriptionError,
                       boolean locked,
                       boolean announcement,
                       boolean parent,
                       boolean noFrequentlyForwarded,
                       Integer ephemeralExpiration,
                       Integer ephemeralTrigger,
                       boolean membershipApprovalMode,
                       boolean breakout,
                       Jid linkedParentJid,
                       boolean hiddenGroup,
                       boolean allowNonAdminSubGroupCreation,
                       boolean groupHistory,
                       boolean capi,
                       List<ResponseParticipant> participants,
                       Stanza group) {
            this.groupId = Objects.requireNonNull(groupId, "groupId cannot be null");
            this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
            this.groupCreator = Objects.requireNonNull(groupCreator, "groupCreator cannot be null");
            this.groupCreation = groupCreation;
            this.groupSyncTime = groupSyncTime;
            this.groupSyncOwner = groupSyncOwner;
            this.subject = Objects.requireNonNull(subject, "subject cannot be null");
            this.descriptionId = descriptionId;
            this.descriptionError = descriptionError;
            this.locked = locked;
            this.announcement = announcement;
            this.parent = parent;
            this.noFrequentlyForwarded = noFrequentlyForwarded;
            this.ephemeralExpiration = ephemeralExpiration;
            this.ephemeralTrigger = ephemeralTrigger;
            this.membershipApprovalMode = membershipApprovalMode;
            this.breakout = breakout;
            this.linkedParentJid = linkedParentJid;
            this.hiddenGroup = hiddenGroup;
            this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
            this.groupHistory = groupHistory;
            this.capi = capi;
            Objects.requireNonNull(participants, "participants cannot be null");
            if (participants.isEmpty()) {
                throw new IllegalArgumentException("participants must contain at least one entry");
            }
            this.participants = List.copyOf(participants);
            this.group = Objects.requireNonNull(group, "group cannot be null");
        }

        /**
         * Returns the new group's user-component id.
         *
         * Concatenate with {@code @g.us} to recover the fully-qualified group JID; the same value is exposed
         * pre-built via {@link #groupJid()}.
         *
         * @return the group id; never {@code null}
         */
        public String groupId() {
            return groupId;
        }

        /**
         * Returns the fully-qualified group {@link Jid}.
         *
         * @return the group JID; never {@code null}
         */
        public Jid groupJid() {
            return groupJid;
        }

        /**
         * Returns the creator {@link Jid}.
         *
         * @return the creator JID; never {@code null}
         */
        public Jid groupCreator() {
            return groupCreator;
        }

        /**
         * Returns the creation timestamp.
         *
         * @return the creation timestamp in seconds since epoch
         */
        public long groupCreation() {
            return groupCreation;
        }

        /**
         * Returns the optional {@code s_t} sync-time mixin value.
         *
         * @return an {@link Optional} carrying the value, or empty when omitted
         */
        public Optional<Long> groupSyncTime() {
            return Optional.ofNullable(groupSyncTime);
        }

        /**
         * Returns the optional {@code s_o} sync-owner {@link Jid}.
         *
         * @return an {@link Optional} carrying the JID, or empty when omitted
         */
        public Optional<Jid> groupSyncOwner() {
            return Optional.ofNullable(groupSyncOwner);
        }

        /**
         * Returns the subject (display name).
         *
         * @return the subject; never {@code null}
         */
        public String subject() {
            return subject;
        }

        /**
         * Returns the optional description id.
         *
         * @return an {@link Optional} carrying the id, or empty when the relay did not commit a description
         */
        public Optional<String> descriptionId() {
            return Optional.ofNullable(descriptionId);
        }

        /**
         * Returns the optional description-error string.
         *
         * Non-empty when the group was created but the description body was rejected (e.g. {@code "406"} or
         * {@code "500"}); callers should surface a partial-acceptance result rather than treating the whole
         * operation as failed.
         *
         * @return an {@link Optional} carrying the error string, or empty when the description committed cleanly
         */
        public Optional<String> descriptionError() {
            return Optional.ofNullable(descriptionError);
        }

        /**
         * Returns whether the relay echoed the {@code <locked/>} marker.
         *
         * @return {@code true} when chat-info edits are admin-only
         */
        public boolean locked() {
            return locked;
        }

        /**
         * Returns whether the relay echoed the {@code <announcement/>} marker.
         *
         * @return {@code true} when posting is restricted to admins
         */
        public boolean announcement() {
            return announcement;
        }

        /**
         * Returns whether the relay echoed the {@code <parent/>} marker.
         *
         * @return {@code true} when the new group is a community parent
         */
        public boolean parent() {
            return parent;
        }

        /**
         * Returns whether the relay echoed the {@code <no_frequently_forwarded/>} marker.
         *
         * @return {@code true} when the marker is present
         */
        public boolean noFrequentlyForwarded() {
            return noFrequentlyForwarded;
        }

        /**
         * Returns the optional {@code <ephemeral expiration="...">} value.
         *
         * @return an {@link Optional} carrying the value in seconds, or empty when no ephemeral expiration was
         *         committed
         */
        public Optional<Integer> ephemeralExpiration() {
            return Optional.ofNullable(ephemeralExpiration);
        }

        /**
         * Returns the optional {@code <ephemeral trigger="...">} value.
         *
         * @return an {@link Optional} carrying the value, or empty when omitted
         */
        public Optional<Integer> ephemeralTrigger() {
            return Optional.ofNullable(ephemeralTrigger);
        }

        /**
         * Returns whether the relay echoed the {@code <membership_approval_mode/>} marker.
         *
         * @return {@code true} when the marker is present
         */
        public boolean membershipApprovalMode() {
            return membershipApprovalMode;
        }

        /**
         * Returns whether the relay echoed the {@code <breakout/>} marker.
         *
         * @return {@code true} when the new group is a breakout sub-group
         */
        public boolean breakout() {
            return breakout;
        }

        /**
         * Returns the optional linked parent community {@link Jid}.
         *
         * @return an {@link Optional} carrying the JID, or empty when the new group has no parent community
         */
        public Optional<Jid> linkedParentJid() {
            return Optional.ofNullable(linkedParentJid);
        }

        /**
         * Returns whether the relay echoed the {@code <hidden_group/>} marker.
         *
         * @return {@code true} when the new group is hidden from the community directory
         */
        public boolean hiddenGroup() {
            return hiddenGroup;
        }

        /**
         * Returns whether the relay echoed the {@code <allow_non_admin_sub_group_creation/>} marker.
         *
         * @return {@code true} when the marker is present
         */
        public boolean allowNonAdminSubGroupCreation() {
            return allowNonAdminSubGroupCreation;
        }

        /**
         * Returns whether the relay echoed the {@code <group_history/>} marker.
         *
         * @return {@code true} when the marker is present
         */
        public boolean groupHistory() {
            return groupHistory;
        }

        /**
         * Returns whether the relay echoed the {@code <capi/>} marker.
         *
         * @return {@code true} when the marker is present
         */
        public boolean capi() {
            return capi;
        }

        /**
         * Returns the seed-participant echo rows.
         *
         * One entry per participant the relay processed; an entry's {@link ResponseParticipant#notRegisteredOnWa()}
         * discriminates between successfully-added and non-registered WA user.
         *
         * @return an unmodifiable list of participant rows; never empty
         */
        public List<ResponseParticipant> participants() {
            return participants;
        }

        /**
         * Returns the verbatim {@code <group/>} sub-stanza carrying the remaining group-info mixin metadata.
         *
         * Exposed for callers that need to read mixin metadata (addressing mode, subject-owner identity,
         * member-add, member-link and member-share-history mixins, dedup attribute echo) that Cobalt does not
         * project as typed accessors.
         *
         * @return the raw {@code <group/>} {@link Stanza}; never {@code null}
         */
        public Stanza group() {
            return group;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant.
         *
         * Runs as the first probe in the variant cascade driven by {@link SmaxGroupsCreateResponse#of(Stanza, Stanza)}.
         *
         * @implNote This implementation validates that the IQ envelope is an {@code <iq type="result">} addressed
         * {@code from} a {@code g.us} JID with the echoed {@code id} attribute, then extracts the {@code <group/>}
         * child and reads the mandatory identity attributes ({@code id}, {@code creator}, {@code creation},
         * {@code subject}), the optional mixin attributes ({@code s_t}, {@code s_o}), the boolean-gated marker
         * children, the optional ephemeral, description and linked-parent sub-nodes, and the participant rows. Any
         * missing mandatory attribute or unparseable participant short-circuits the parse and returns
         * {@link Optional#empty()}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the stanza
         *         does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateResponseSuccess",
                exports = "parseCreateResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var fromAttr = stanza.getAttributeAsString("from").orElse(null);
            if (fromAttr == null || !fromAttr.endsWith("g.us")) {
                return Optional.empty();
            }
            var group = stanza.getChild("group").orElse(null);
            if (group == null) {
                return Optional.empty();
            }
            var groupId = group.getAttributeAsString("id").orElse(null);
            if (groupId == null) {
                return Optional.empty();
            }
            var groupCreator = group.getAttributeAsJid("creator").orElse(null);
            if (groupCreator == null) {
                return Optional.empty();
            }
            if (group.getAttributeAsLong("creation").isEmpty()) {
                return Optional.empty();
            }
            var groupCreation = group.getAttributeAsLong("creation").getAsLong();
            Long groupSyncTime = null;
            if (group.getAttributeAsLong("s_t").isPresent()) {
                groupSyncTime = group.getAttributeAsLong("s_t").getAsLong();
            }
            var groupSyncOwner = group.getAttributeAsJid("s_o").orElse(null);
            var subject = group.getAttributeAsString("subject").orElse(null);
            if (subject == null) {
                return Optional.empty();
            }
            var groupJid = Jid.of(groupId, JidServer.groupOrCommunity());
            var description = group.getChild("description").orElse(null);
            String descriptionId = null;
            String descriptionError = null;
            if (description != null) {
                descriptionId = description.getAttributeAsString("id").orElse(null);
                descriptionError = description.getAttributeAsString("error").orElse(null);
            }
            var locked = group.getChild("locked").isPresent();
            var announcement = group.getChild("announcement").isPresent();
            var parent = group.getChild("parent").isPresent();
            var noFrequentlyForwarded = group.getChild("no_frequently_forwarded").isPresent();
            Integer ephemeralExpiration = null;
            Integer ephemeralTrigger = null;
            var ephemeral = group.getChild("ephemeral").orElse(null);
            if (ephemeral != null) {
                ephemeralExpiration = ephemeral.getAttributeAsInt("expiration").orElse(0);
                if (ephemeral.getAttributeAsInt("trigger").isPresent()) {
                    ephemeralTrigger = ephemeral.getAttributeAsInt("trigger").getAsInt();
                }
            }
            var membershipApprovalMode = group.getChild("membership_approval_mode").isPresent();
            var breakout = group.getChild("breakout").isPresent();
            Jid linkedParentJid = null;
            var linkedParent = group.getChild("linked_parent").orElse(null);
            if (linkedParent != null) {
                linkedParentJid = linkedParent.getAttributeAsJid("jid").orElse(null);
            }
            var hiddenGroup = group.getChild("hidden_group").isPresent();
            var allowNonAdmin = group.getChild("allow_non_admin_sub_group_creation").isPresent();
            var groupHistory = group.getChild("group_history").isPresent();
            var capi = group.getChild("capi").isPresent();
            var participants = new ArrayList<ResponseParticipant>();
            for (var participantNode : group.getChildren("participant")) {
                var parsed = ResponseParticipant.of(participantNode).orElse(null);
                if (parsed == null) {
                    return Optional.empty();
                }
                participants.add(parsed);
            }
            if (participants.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(groupId, groupJid, groupCreator, groupCreation, groupSyncTime,
                    groupSyncOwner, subject, descriptionId, descriptionError, locked, announcement, parent,
                    noFrequentlyForwarded, ephemeralExpiration, ephemeralTrigger, membershipApprovalMode,
                    breakout, linkedParentJid, hiddenGroup, allowNonAdmin, groupHistory, capi, participants,
                    group));
        }

        /**
         * Compares this success to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return this.groupCreation == that.groupCreation
                    && this.locked == that.locked
                    && this.announcement == that.announcement
                    && this.parent == that.parent
                    && this.noFrequentlyForwarded == that.noFrequentlyForwarded
                    && this.membershipApprovalMode == that.membershipApprovalMode
                    && this.breakout == that.breakout
                    && this.hiddenGroup == that.hiddenGroup
                    && this.allowNonAdminSubGroupCreation == that.allowNonAdminSubGroupCreation
                    && this.groupHistory == that.groupHistory
                    && this.capi == that.capi
                    && Objects.equals(this.groupId, that.groupId)
                    && Objects.equals(this.groupJid, that.groupJid)
                    && Objects.equals(this.groupCreator, that.groupCreator)
                    && Objects.equals(this.groupSyncTime, that.groupSyncTime)
                    && Objects.equals(this.groupSyncOwner, that.groupSyncOwner)
                    && Objects.equals(this.subject, that.subject)
                    && Objects.equals(this.descriptionId, that.descriptionId)
                    && Objects.equals(this.descriptionError, that.descriptionError)
                    && Objects.equals(this.ephemeralExpiration, that.ephemeralExpiration)
                    && Objects.equals(this.ephemeralTrigger, that.ephemeralTrigger)
                    && Objects.equals(this.linkedParentJid, that.linkedParentJid)
                    && Objects.equals(this.participants, that.participants)
                    && Objects.equals(this.group, that.group);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @implNote This implementation splits the hash into two {@code Objects.hash} batches because the field
         * list exceeds the practical width of a single varargs call; the {@code primary * 31 + secondary} mix
         * preserves permutation-sensitive distribution across the batches.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            var primary = Objects.hash(groupId, groupJid, groupCreator, groupCreation, groupSyncTime,
                    groupSyncOwner, subject, descriptionId, descriptionError, locked, announcement, parent,
                    noFrequentlyForwarded, ephemeralExpiration, ephemeralTrigger);
            var secondary = Objects.hash(membershipApprovalMode, breakout, linkedParentJid, hiddenGroup,
                    allowNonAdminSubGroupCreation, groupHistory, capi, participants);
            return primary * 31 + secondary;
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateResponse.Success[groupId=" + groupId
                    + ", groupJid=" + groupJid
                    + ", groupCreator=" + groupCreator
                    + ", groupCreation=" + groupCreation
                    + ", groupSyncTime=" + groupSyncTime
                    + ", groupSyncOwner=" + groupSyncOwner
                    + ", subject=" + subject
                    + ", descriptionId=" + descriptionId
                    + ", descriptionError=" + descriptionError
                    + ", locked=" + locked
                    + ", announcement=" + announcement
                    + ", parent=" + parent
                    + ", noFrequentlyForwarded=" + noFrequentlyForwarded
                    + ", ephemeralExpiration=" + ephemeralExpiration
                    + ", ephemeralTrigger=" + ephemeralTrigger
                    + ", membershipApprovalMode=" + membershipApprovalMode
                    + ", breakout=" + breakout
                    + ", linkedParentJid=" + linkedParentJid
                    + ", hiddenGroup=" + hiddenGroup
                    + ", allowNonAdminSubGroupCreation=" + allowNonAdminSubGroupCreation
                    + ", groupHistory=" + groupHistory
                    + ", capi=" + capi
                    + ", participants=" + participants + ']';
        }

        /**
         * Single seed-participant echo row inside a {@link Success}.
         *
         * Each requested participant surfaces either as a successfully-added participant
         * ({@link #notRegisteredOnWa()} returns {@code false}, with {@link #username()} populated when the relay
         * echoed the username mixin) or as a not-registered marker ({@link #notRegisteredOnWa()} returns
         * {@code true}).
         *
         * @implNote This implementation collapses the WA Web alternation (added-participant arm vs.
         * non-registered-WA-user arm) into a single class with the {@link #notRegisteredOnWa()} flag because the
         * two sub-shapes share the same attribute schema and downstream callers branch on a single boolean.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateParticipantAddedResponseMixin")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsNonRegisteredWaUserParticipantErrorLidResponseMixin")
        public static final class ResponseParticipant {
            /**
             * Holds the participant {@link Jid} echoed by the relay when the participant was successfully added;
             * {@code null} for not-registered entries.
             */
            private final Jid jid;

            /**
             * Holds the optional phone-number mixin echoed by the relay (always present on not-registered entries;
             * optional on added entries).
             */
            private final Jid phoneNumber;

            /**
             * Holds the optional username echoed by the relay for LID-addressed participants.
             */
            private final String username;

            /**
             * Indicates whether the relay surfaced the not-registered marker (parsed via the non-registered-WA-user
             * mixin rather than the added-participant mixin).
             */
            private final boolean notRegisteredOnWa;

            /**
             * Constructs a participant echo row.
             *
             * Direct construction is primarily used to seed test fixtures; most callers obtain instances via
             * {@link #of(Stanza)}.
             *
             * @param jid               the participant JID; may be {@code null} for not-registered rows
             * @param phoneNumber       the optional phone JID
             * @param username          the optional username
             * @param notRegisteredOnWa whether the participant is a non-registered WA user
             */
            public ResponseParticipant(Jid jid, Jid phoneNumber, String username, boolean notRegisteredOnWa) {
                this.jid = jid;
                this.phoneNumber = phoneNumber;
                this.username = username;
                this.notRegisteredOnWa = notRegisteredOnWa;
            }

            /**
             * Returns the participant {@link Jid}.
             *
             * @return an {@link Optional} carrying the JID, or empty for not-registered rows
             */
            public Optional<Jid> jid() {
                return Optional.ofNullable(jid);
            }

            /**
             * Returns the optional phone-number {@link Jid}.
             *
             * Always present on not-registered rows; optional on added rows depending on whether the relay echoed
             * the phone-number mixin.
             *
             * @return an {@link Optional} carrying the phone JID, or empty when omitted
             */
            public Optional<Jid> phoneNumber() {
                return Optional.ofNullable(phoneNumber);
            }

            /**
             * Returns the optional username echoed by the relay for LID-addressed participants.
             *
             * @return an {@link Optional} carrying the username, or empty when omitted
             */
            public Optional<String> username() {
                return Optional.ofNullable(username);
            }

            /**
             * Returns whether this row represents a non-registered WhatsApp user.
             *
             * @return {@code true} when the relay surfaced the not-registered marker
             */
            public boolean notRegisteredOnWa() {
                return notRegisteredOnWa;
            }

            /**
             * Parses a single {@code <participant/>} child into a {@link ResponseParticipant} row.
             *
             * Invoked by {@link Success#of(Stanza, Stanza)} for every {@code <participant/>} child inside the echoed
             * {@code <group/>} subtree.
             *
             * @implNote This implementation reads the {@code jid}, {@code phone_number} and {@code username}
             * attributes and infers the not-registered flag from the absence of a {@code jid} attribute combined
             * with the presence of a {@code phone_number} attribute. A row with neither {@code jid} nor
             * {@code phone_number} is rejected.
             *
             * @param participantStanza the {@code <participant/>} child
             * @return an {@link Optional} carrying the parsed row, or {@link Optional#empty()} when the child
             *         satisfies neither sub-shape
             */
            static Optional<ResponseParticipant> of(Stanza participantStanza) {
                var jid = participantStanza.getAttributeAsJid("jid").orElse(null);
                var phoneNumber = participantStanza.getAttributeAsJid("phone_number").orElse(null);
                var username = participantStanza.getAttributeAsString("username").orElse(null);
                var notRegistered = jid == null && phoneNumber != null;
                if (jid == null && !notRegistered) {
                    return Optional.empty();
                }
                return Optional.of(new ResponseParticipant(jid, phoneNumber, username, notRegistered));
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is a {@link ResponseParticipant} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (ResponseParticipant) obj;
                return this.notRegisteredOnWa == that.notRegisteredOnWa
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.phoneNumber, that.phoneNumber)
                        && Objects.equals(this.username, that.username);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, phoneNumber, username, notRegisteredOnWa);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsCreateResponse.Success.ResponseParticipant[jid=" + jid
                        + ", phoneNumber=" + phoneNumber
                        + ", username=" + username
                        + ", notRegisteredOnWa=" + notRegisteredOnWa + ']';
            }
        }
    }

    /**
     * Reply variant returned when the relay detected an existing group whose creator-plus-dedup-token tuple matches
     * the new request and returned the existing group's JID rather than creating a new one.
     *
     * Callers that want to surface the existing-group experience should branch on this variant explicitly and
     * extract {@link #groupJid()} to navigate to the existing chat.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateResponseGroupAlreadyExists")
    final class GroupAlreadyExists implements SmaxGroupsCreateResponse {
        /**
         * Holds the existing group {@link Jid} surfaced by the relay.
         */
        private final Jid groupJid;

        /**
         * Constructs a group-already-exists variant.
         *
         * Direct construction is primarily used to seed test fixtures; most callers obtain instances via
         * {@link #of(Stanza, Stanza)}.
         *
         * @param groupJid the existing group {@link Jid}; never {@code null}
         * @throws NullPointerException if {@code groupJid} is {@code null}
         */
        public GroupAlreadyExists(Jid groupJid) {
            this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        }

        /**
         * Returns the existing group {@link Jid}.
         *
         * @return the group JID; never {@code null}
         */
        public Jid groupJid() {
            return groupJid;
        }

        /**
         * Parses the inbound stanza into a {@link GroupAlreadyExists} variant.
         *
         * Runs as the second probe in the variant cascade driven by
         * {@link SmaxGroupsCreateResponse#of(Stanza, Stanza)}.
         *
         * @implNote This implementation validates the {@code <iq type="result">} envelope addressed {@code from} a
         * {@code g.us} JID with the echoed {@code id} attribute, extracts the {@code <group/>} child, and reads the
         * {@code jid} attribute. The {@code <group/>} child on this branch carries only the existing group's JID,
         * distinct from the rich {@code <group/>} subtree on the {@link Success} branch.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the stanza
         *         does not match the already-exists schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateResponseGroupAlreadyExists",
                exports = "parseCreateResponseGroupAlreadyExists",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<GroupAlreadyExists> of(Stanza stanza, Stanza request) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var requestId = request.getAttributeAsString("id").orElse(null);
            if (requestId == null) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("id", requestId)) {
                return Optional.empty();
            }
            var fromAttr = stanza.getAttributeAsString("from").orElse(null);
            if (fromAttr == null || !fromAttr.endsWith("g.us")) {
                return Optional.empty();
            }
            var group = stanza.getChild("group").orElse(null);
            if (group == null) {
                return Optional.empty();
            }
            var groupJid = group.getAttributeAsJid("jid").orElse(null);
            if (groupJid == null) {
                return Optional.empty();
            }
            return Optional.of(new GroupAlreadyExists(groupJid));
        }

        /**
         * Compares this variant to {@code obj} for value equality on {@link #groupJid()}.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link GroupAlreadyExists} with the same group JID
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (GroupAlreadyExists) obj;
            return Objects.equals(this.groupJid, that.groupJid);
        }

        /**
         * Returns a hash derived from {@link #groupJid()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groupJid);
        }

        /**
         * Returns a debug string carrying {@link #groupJid()}.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateResponse.GroupAlreadyExists[groupJid=" + groupJid + ']';
        }
    }

    /**
     * Reply variant returned when the relay rejected the request as malformed, unauthorised, or bumping a
     * per-creator rate-limit cap.
     *
     * @implNote This implementation exposes the raw error envelope and leaves any rate-limit projection (such as
     * distinguishing time-based from count-based participant-add caps) to the caller.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateResponseClientError")
    final class ClientError implements SmaxGroupsCreateResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a client-error variant.
         *
         * Direct construction is primarily used to seed test fixtures; most callers obtain instances via
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} envelope.
         *
         * Runs as the third probe in the variant cascade driven by {@link SmaxGroupsCreateResponse#of(Stanza, Stanza)}.
         *
         * @implNote This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} so every SMAX response in the family shares
         * the same client-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the envelope
         *         does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateResponseClientError",
                exports = "parseCreateResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reply variant returned when the relay encountered a transient internal failure.
     *
     * Callers can decide whether to retry based on the surfaced {@link #errorCode()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsCreateResponseServerError")
    final class ServerError implements SmaxGroupsCreateResponse {
        /**
         * Holds the numeric server-side error code, mirroring the {@code <error code="...">} attribute on the
         * inbound stanza.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay; {@code null} when the relay omitted the
         * {@code <error text="...">} attribute.
         */
        private final String errorText;

        /**
         * Constructs a server-error variant.
         *
         * Direct construction is primarily used to seed test fixtures; most callers obtain instances via
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} envelope.
         *
         * Runs as the terminal probe in the variant cascade driven by
         * {@link SmaxGroupsCreateResponse#of(Stanza, Stanza)}.
         *
         * @implNote This implementation delegates the error-envelope extraction to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} so every SMAX response in the family shares
         * the same server-error parsing.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the envelope
         *         does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsCreateResponseServerError",
                exports = "parseCreateResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsCreateResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
