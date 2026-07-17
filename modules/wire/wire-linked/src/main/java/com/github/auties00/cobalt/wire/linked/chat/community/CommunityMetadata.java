package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.linked.chat.ChatPolicy;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Represents the metadata of a WhatsApp community.
 *
 * <p>A WhatsApp community is a top-level organizational entity that groups
 * related conversations (subgroups) under a single umbrella. Each community
 * has its own identity (JID and subject), a founder, an optional description,
 * a participant list, administrative policy settings, and a set of linked
 * subgroups accessible via {@link #communityGroups()}.
 *
 * <p>Communities support extensive administrative controls including:
 * <ul>
 *   <li>Announcement mode ({@link #isAnnounce()}) to restrict messaging to
 *       administrators only</li>
 *   <li>Metadata restriction ({@link #isRestrict()}) to prevent non-admin
 *       changes to the subject, description, and profile picture</li>
 *   <li>Membership approval ({@link #isMembershipApprovalMode()}) to require
 *       admin approval for new join requests</li>
 *   <li>Member add mode ({@link #isMemberAddModeAdminOnly()}) to restrict
 *       who can add new members</li>
 *   <li>Subgroup creation permissions
 *       ({@link #isAllowNonAdminSubGroupCreation()}) controlling whether
 *       non-admin members can create new subgroups</li>
 *   <li>Growth lock ({@link #growthLockExpiration()}) to temporarily prevent
 *       new members from joining</li>
 * </ul>
 *
 * <p>Instances of this class are mutable. All fields can be changed after
 * construction through the setter methods. Collection-typed fields
 * ({@link #participants()} and {@link #communityGroups()}) additionally
 * expose dedicated add, remove, and clear operations.
 *
 * @see ChatMetadata
 * @see CommunityLinkedGroup
 */
@ProtobufMessage
public final class CommunityMetadata implements ChatMetadata {
    /**
     * The JID that uniquely identifies this community within WhatsApp.
     * Community JIDs use the group JID format and are distinct from the
     * JIDs of the community's linked subgroups.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The subject (display name) of this community. The subject is set by
     * a community administrator and is visible to all participants.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String subject;

    /**
     * The JID of the participant who last changed the subject, or
     * {@code null} if the author is not known.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    Jid subjectAuthorJid;

    /**
     * The instant at which the subject was last changed, or {@code null}
     * if the timestamp is not available.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant subjectTimestamp;

    /**
     * The instant at which this community was created, or {@code null} if
     * the timestamp is not available.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant foundationTimestamp;

    /**
     * The JID of the user who originally created this community, or
     * {@code null} if the founder is not known.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    Jid founderJid;

    /**
     * The free-form description text of this community, or {@code null} if
     * no description has been set. The description is typically a paragraph
     * explaining the purpose or topic of the community.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String description;

    /**
     * The server-assigned identifier for the current description revision,
     * or {@code null} if no description identifier is available. This
     * identifier is used to detect conflicting concurrent edits to the
     * community description.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String descriptionId;

    /**
     * The instant at which the description was last changed, or
     * {@code null} if the timestamp is not available.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant descriptionTimestamp;

    /**
     * The ordered set of participants currently in this community. Each
     * participant has a JID, role (regular member, admin, or super admin),
     * and other metadata. Community participants are the users who have
     * joined the community itself (not necessarily every subgroup).
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    SequencedSet<GroupParticipant> participants;

    /**
     * The JID of the participant who last changed the description, or
     * {@code null} if not known.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    Jid descriptionAuthorJid;

    /**
     * The ephemeral message timer for this community, or {@code null} if
     * ephemeral messaging is disabled. When set, messages sent to this
     * community automatically disappear after the specified duration.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT32)
    ChatEphemeralTimer ephemeralExpiration;

    /**
     * Whether metadata editing is restricted to administrators only. When
     * {@code true}, only administrators can change the community's subject,
     * description, and profile picture. When {@code false}, any member can
     * modify these properties.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    boolean restrict;

    /**
     * Whether the community is in announcement mode. When {@code true},
     * only administrators can send messages to the community. Regular
     * members can only read messages.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    boolean announce;

    /**
     * The ordered set of subgroups linked to this community. This includes
     * both joined and unjoined subgroups, the default announcement subgroup,
     * the general chat subgroup, and any additional subgroups created by
     * administrators or permitted members.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    SequencedSet<CommunityLinkedGroup> communityGroups;

    /**
     * Whether this community uses LID (Linked Identity) addressing mode
     * instead of traditional phone-number-based addressing. In LID mode,
     * participants are identified by server-assigned opaque identifiers
     * rather than phone numbers, providing enhanced privacy.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    boolean isLidAddressingMode;

    /**
     * Whether this community operates in incognito mode. In incognito
     * mode, the community's membership and activity details may be
     * restricted from external visibility.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
    boolean isIncognito;

    /**
     * Whether frequently forwarded messages are blocked in this community.
     * When {@code true}, messages that WhatsApp has identified as frequently
     * forwarded cannot be sent into this community.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    boolean noFrequentlyForwarded;

    /**
     * Whether admin approval is required for new members to join this
     * community. When {@code true}, users who request to join must be
     * approved by an administrator before they become members.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    boolean membershipApprovalMode;

    /**
     * Whether invite link sharing is restricted to administrators. When
     * {@code true}, only administrators can share the community's invite
     * link to add new members. When {@code false}, any member can share
     * the link.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    boolean memberLinkModeAdminOnly;

    /**
     * Whether non-admin members are allowed to create or link subgroups
     * within this community. When {@code true}, any member can create new
     * subgroups. When {@code false}, only administrators can do so.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    boolean allowNonAdminSubGroupCreation;

    /**
     * Whether only administrators can add members to this community. When
     * {@code true}, direct member addition is restricted to administrators.
     * When {@code false}, any existing member can add new participants.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.BOOL)
    boolean memberAddModeAdminOnly;

    /**
     * The instant at which the growth lock expires, or {@code null} if no
     * growth lock is active. A growth lock temporarily prevents new members
     * from joining the community, typically imposed as a policy enforcement
     * measure.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant growthLockExpiration;

    /**
     * The type of growth lock applied to this community (for example
     * {@code "invite"}), or {@code null} if no growth lock is active.
     * The type indicates which growth vector is restricted.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.STRING)
    String growthLockType;

    /**
     * Whether the "report to admin" feature is enabled for this community.
     * When enabled, community members can report messages or participants
     * directly to community administrators for review.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.BOOL)
    boolean reportToAdminMode;

    /**
     * The instant of the last report-to-admin event, or {@code null} if no
     * report has been filed yet.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastReportToAdminTimestamp;

    /**
     * The server-reported total participant count, or {@code null} if not
     * available. This is the count reported by the server and may differ
     * from the size of {@link #participants} if the full participant list
     * has not been fetched.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.UINT32)
    Integer size;

    /**
     * Whether this is a WhatsApp support group. Support groups are
     * managed by WhatsApp and have restricted administrative capabilities
     * for regular members and administrators.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    boolean support;

    /**
     * Whether this community has been suspended by WhatsApp. A suspended
     * community cannot receive or send messages until it is restored.
     * This typically happens as a result of policy enforcement actions.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean suspended;

    /**
     * Whether this community has been permanently terminated by WhatsApp.
     * A terminated community is permanently deactivated and cannot be
     * restored, unlike a suspended community which may be reinstated.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    boolean terminated;

    /**
     * Whether the parent community group is closed. A closed community
     * requires membership approval for all join requests, effectively
     * making it invitation-only.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.BOOL)
    boolean isParentGroupClosed;

    /**
     * Whether this community contains a default announcement subgroup.
     * The default announcement subgroup is a read-only group where only
     * administrators can post messages, used for community-wide
     * announcements.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BOOL)
    boolean defaultSubgroup;

    /**
     * Whether this community contains a general chat subgroup. The general
     * chat subgroup is the main open discussion space where all community
     * members can participate.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.BOOL)
    boolean generalSubgroup;

    /**
     * Whether this community contains a hidden subgroup. Hidden subgroups
     * are not visible in the community's public subgroup list but still
     * exist and function normally for their members.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    boolean hiddenSubgroup;

    /**
     * Whether the group safety check flag is set. The safety check is a
     * trust signal that indicates whether the community has been reviewed
     * for compliance with platform policies.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    boolean groupSafetyCheck;

    /**
     * The JID of the user who added the current user to this community, or
     * {@code null} if the adder is not known. This is used to determine
     * trust relationships within the community.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.STRING)
    Jid groupAdder;

    /**
     * Whether automatic addition to the general chat subgroup is disabled.
     * When {@code true}, new community members are not automatically added
     * to the general chat subgroup and must join it manually.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.BOOL)
    boolean generalChatAutoAddDisabled;

    /**
     * The instant of the last community poll, or {@code null} if no poll
     * has been conducted. Community polls are periodic server-side checks
     * to refresh community metadata.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastCommunityPollTimestamp;

    /**
     * The instant of the last activity in this community, or {@code null}
     * if not available. Activity includes messages, membership changes,
     * and administrative actions across the community and its subgroups.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastActivityTimestamp;

    /**
     * The instant of the last seen activity in this community, or
     * {@code null} if not available. This tracks the most recent activity
     * that the current user has seen, as opposed to
     * {@link #lastActivityTimestamp} which tracks overall activity.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastSeenActivityTimestamp;

    /**
     * Whether this community has CAPI (Community API) capabilities enabled.
     * CAPI provides programmatic access to community management features
     * for business integrations.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean hasCapi;

    /**
     * Whether this community is a TEE (Trusted Execution Environment) bot
     * group. TEE bot groups run AI bots in a secure enclave environment
     * for privacy-preserving interactions.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.BOOL)
    boolean isTeeBotGroup;

    /**
     * The trigger that caused the disappearing message mode to be set, or
     * {@code null} if disappearing messages have not been configured. The
     * trigger indicates whether the setting was changed per-chat, via
     * account-wide defaults, or by a group administrator.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.ENUM)
    ChatDisappearingMode.Trigger disappearingModeTrigger;

    /**
     * Whether the current user initiated the disappearing message mode
     * for this community. This is {@code true} if the logged-in user was
     * the one who enabled disappearing messages.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.BOOL)
    boolean disappearingModeInitiatedByMe;

    /**
     * The server-reported number of subgroups in this community, or
     * {@code null} if not available. This count may include both joined
     * and unjoined subgroups.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.UINT32)
    Integer numSubgroups;

    /**
     * Whether the limit sharing feature is enabled for this community.
     * When enabled, personal information sharing between members is
     * restricted to enhance privacy.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.BOOL)
    boolean limitSharingEnabled;

    /**
     * The group evolution version, or {@code null} if not available. This
     * version number tracks schema or feature-level changes to the
     * community structure over time.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.UINT32)
    Integer evolutionVersion;

    /**
     * Whether participant labels are enabled for this community. When
     * enabled, administrators can assign labels to community members to
     * categorize or tag them.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    boolean participantLabelEnabled;

    /**
     * Whether this community has the open Meta AI bot feature enabled.
     * When enabled, the Meta AI bot can participate in the community and
     * respond to messages. This field is not serialized as a protobuf
     * property and is instead populated programmatically from the group
     * query response when bot participants are detected.
     */
    boolean isOpenBotGroup;

    /**
     * Constructs a new {@code CommunityMetadata} with the specified values.
     *
     * <p>The {@code jid} and {@code subject} parameters are required and
     * must not be {@code null}. All other parameters accept {@code null} to
     * indicate an absent or unknown value. Collection-typed parameters that
     * are {@code null} are replaced with empty mutable collections so that
     * callers can safely add elements after construction.
     *
     * @param jid                           the non-{@code null} community JID
     * @param subject                       the non-{@code null} display name
     * @param subjectAuthorJid              the author of the last subject
     *                                      change, or {@code null}
     * @param subjectTimestamp              the subject change instant, or
     *                                      {@code null}
     * @param foundationTimestamp           the community creation instant, or
     *                                      {@code null}
     * @param founderJid                    the founder JID, or {@code null}
     * @param description                   the description text, or
     *                                      {@code null}
     * @param descriptionId                 the server-assigned description
     *                                      revision identifier, or
     *                                      {@code null}
     * @param descriptionTimestamp          the description change instant, or
     *                                      {@code null}
     * @param participants                  the participant set, or
     *                                      {@code null}
     * @param descriptionAuthorJid          the description author JID, or
     *                                      {@code null}
     * @param ephemeralExpiration            the ephemeral timer, or
     *                                      {@code null} to disable
     * @param restrict                      whether metadata editing is
     *                                      restricted to admins
     * @param announce                      whether announcement mode is on
     * @param communityGroups               the linked subgroups, or
     *                                      {@code null}
     * @param isLidAddressingMode           whether LID addressing is enabled
     * @param isIncognito                   whether incognito mode is enabled
     * @param noFrequentlyForwarded         whether forwarded messages are
     *                                      blocked
     * @param membershipApprovalMode        whether admin approval is required
     *                                      to join
     * @param memberLinkModeAdminOnly       whether invite links are
     *                                      admin-only
     * @param allowNonAdminSubGroupCreation whether non-admins can create
     *                                      subgroups
     * @param memberAddModeAdminOnly        whether only admins can add
     *                                      members
     * @param growthLockExpiration          the growth lock expiration instant,
     *                                      or {@code null}
     * @param growthLockType                the growth lock type, or
     *                                      {@code null}
     * @param reportToAdminMode             whether report-to-admin is enabled
     * @param lastReportToAdminTimestamp    the last report-to-admin instant,
     *                                      or {@code null}
     * @param size                          the server-reported participant
     *                                      count, or {@code null}
     * @param support                       whether this is a support group
     * @param suspended                     whether the community is suspended
     * @param terminated                    whether the community is
     *                                      terminated
     * @param isParentGroupClosed           whether the community is closed
     * @param defaultSubgroup               whether a default announcement
     *                                      subgroup exists
     * @param generalSubgroup               whether a general chat subgroup
     *                                      exists
     * @param hiddenSubgroup                whether a hidden subgroup exists
     * @param groupSafetyCheck              whether the safety check flag is
     *                                      set
     * @param groupAdder                    the JID of who added the current
     *                                      user, or {@code null}
     * @param generalChatAutoAddDisabled    whether auto-add to general chat
     *                                      is disabled
     * @param lastCommunityPollTimestamp    the last community poll instant,
     *                                      or {@code null}
     * @param lastActivityTimestamp         the last activity instant, or
     *                                      {@code null}
     * @param lastSeenActivityTimestamp     the last seen activity instant, or
     *                                      {@code null}
     * @param hasCapi                       whether CAPI is available
     * @param isTeeBotGroup                 whether this is a TEE bot group
     * @param disappearingModeTrigger       the disappearing mode trigger, or
     *                                      {@code null}
     * @param disappearingModeInitiatedByMe whether the current user initiated
     *                                      disappearing mode
     * @param numSubgroups                  the subgroup count, or
     *                                      {@code null}
     * @param limitSharingEnabled           whether limit sharing is enabled
     * @param evolutionVersion              the evolution version, or
     *                                      {@code null}
     * @param participantLabelEnabled       whether participant labels are
     *                                      enabled
     */
    CommunityMetadata(
            Jid jid,
            String subject,
            Jid subjectAuthorJid,
            Instant subjectTimestamp,
            Instant foundationTimestamp,
            Jid founderJid,
            String description,
            String descriptionId,
            Instant descriptionTimestamp,
            SequencedSet<GroupParticipant> participants,
            Jid descriptionAuthorJid,
            ChatEphemeralTimer ephemeralExpiration,
            boolean restrict,
            boolean announce,
            SequencedSet<CommunityLinkedGroup> communityGroups,
            boolean isLidAddressingMode,
            boolean isIncognito,
            boolean noFrequentlyForwarded,
            boolean membershipApprovalMode,
            boolean memberLinkModeAdminOnly,
            boolean allowNonAdminSubGroupCreation,
            boolean memberAddModeAdminOnly,
            Instant growthLockExpiration,
            String growthLockType,
            boolean reportToAdminMode,
            Instant lastReportToAdminTimestamp,
            Integer size,
            boolean support,
            boolean suspended,
            boolean terminated,
            boolean isParentGroupClosed,
            boolean defaultSubgroup,
            boolean generalSubgroup,
            boolean hiddenSubgroup,
            boolean groupSafetyCheck,
            Jid groupAdder,
            boolean generalChatAutoAddDisabled,
            Instant lastCommunityPollTimestamp,
            Instant lastActivityTimestamp,
            Instant lastSeenActivityTimestamp,
            boolean hasCapi,
            boolean isTeeBotGroup,
            ChatDisappearingMode.Trigger disappearingModeTrigger,
            boolean disappearingModeInitiatedByMe,
            Integer numSubgroups,
            boolean limitSharingEnabled,
            Integer evolutionVersion,
            boolean participantLabelEnabled
    ) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.subject = Objects.requireNonNull(subject, "subject cannot be null");
        this.subjectAuthorJid = subjectAuthorJid;
        this.subjectTimestamp = subjectTimestamp;
        this.foundationTimestamp = foundationTimestamp;
        this.founderJid = founderJid;
        this.description = description;
        this.descriptionId = descriptionId;
        this.descriptionTimestamp = descriptionTimestamp;
        this.participants = Objects.requireNonNullElseGet(participants, LinkedHashSet::new);
        this.descriptionAuthorJid = descriptionAuthorJid;
        this.ephemeralExpiration = ephemeralExpiration;
        this.restrict = restrict;
        this.announce = announce;
        this.communityGroups = Objects.requireNonNullElseGet(communityGroups, LinkedHashSet::new);
        this.isLidAddressingMode = isLidAddressingMode;
        this.isIncognito = isIncognito;
        this.noFrequentlyForwarded = noFrequentlyForwarded;
        this.membershipApprovalMode = membershipApprovalMode;
        this.memberLinkModeAdminOnly = memberLinkModeAdminOnly;
        this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
        this.memberAddModeAdminOnly = memberAddModeAdminOnly;
        this.growthLockExpiration = growthLockExpiration;
        this.growthLockType = growthLockType;
        this.reportToAdminMode = reportToAdminMode;
        this.lastReportToAdminTimestamp = lastReportToAdminTimestamp;
        this.size = size;
        this.support = support;
        this.suspended = suspended;
        this.terminated = terminated;
        this.isParentGroupClosed = isParentGroupClosed;
        this.defaultSubgroup = defaultSubgroup;
        this.generalSubgroup = generalSubgroup;
        this.hiddenSubgroup = hiddenSubgroup;
        this.groupSafetyCheck = groupSafetyCheck;
        this.groupAdder = groupAdder;
        this.generalChatAutoAddDisabled = generalChatAutoAddDisabled;
        this.lastCommunityPollTimestamp = lastCommunityPollTimestamp;
        this.lastActivityTimestamp = lastActivityTimestamp;
        this.lastSeenActivityTimestamp = lastSeenActivityTimestamp;
        this.hasCapi = hasCapi;
        this.isTeeBotGroup = isTeeBotGroup;
        this.disappearingModeTrigger = disappearingModeTrigger;
        this.disappearingModeInitiatedByMe = disappearingModeInitiatedByMe;
        this.numSubgroups = numSubgroups;
        this.limitSharingEnabled = limitSharingEnabled;
        this.evolutionVersion = evolutionVersion;
        this.participantLabelEnabled = participantLabelEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Jid jid() {
        return jid;
    }

    /**
     * Sets the JID that identifies this community.
     *
     * @param jid the community JID
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String subject() {
        return subject;
    }

    /**
     * Sets the subject (display name) of this community.
     *
     * @param subject the subject text
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Jid> subjectAuthorJid() {
        return Optional.ofNullable(subjectAuthorJid);
    }

    /**
     * Sets the JID of the participant who last changed the subject.
     *
     * @param subjectAuthorJid the author JID, or {@code null} to clear
     */
    public void setSubjectAuthorJid(Jid subjectAuthorJid) {
        this.subjectAuthorJid = subjectAuthorJid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Instant> subjectTimestamp() {
        return Optional.ofNullable(subjectTimestamp);
    }

    /**
     * Sets the instant at which the subject was last changed.
     *
     * @param subjectTimestamp the subject change instant, or {@code null}
     *                        to clear
     */
    public void setSubjectTimestamp(Instant subjectTimestamp) {
        this.subjectTimestamp = subjectTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Instant> foundationTimestamp() {
        return Optional.ofNullable(foundationTimestamp);
    }

    /**
     * Sets the instant at which this community was created.
     *
     * @param foundationTimestamp the creation instant, or {@code null} to
     *                           clear
     */
    public void setFoundationTimestamp(Instant foundationTimestamp) {
        this.foundationTimestamp = foundationTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Jid> founderJid() {
        return Optional.ofNullable(founderJid);
    }

    /**
     * Sets the JID of the user who originally created this community.
     *
     * @param founderJid the founder JID, or {@code null} to clear
     */
    public void setFounderJid(Jid founderJid) {
        this.founderJid = founderJid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets the free-form description text of this community.
     *
     * @param description the description text, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> descriptionId() {
        return Optional.ofNullable(descriptionId);
    }

    /**
     * Sets the server-assigned description revision identifier.
     *
     * @param descriptionId the description identifier, or {@code null} to
     *                      clear
     */
    public void setDescriptionId(String descriptionId) {
        this.descriptionId = descriptionId;
    }

    /**
     * Returns the instant at which the description was last changed, if
     * known.
     *
     * @return an {@code Optional} containing the description timestamp, or
     *         empty if not available
     */
    public Optional<Instant> descriptionTimestamp() {
        return Optional.ofNullable(descriptionTimestamp);
    }

    /**
     * Sets the instant at which the description was last changed.
     *
     * @param descriptionTimestamp the description change instant, or
     *                            {@code null} to clear
     */
    public void setDescriptionTimestamp(Instant descriptionTimestamp) {
        this.descriptionTimestamp = descriptionTimestamp;
    }

    /**
     * Returns the JID of the participant who last changed the description,
     * if known.
     *
     * @return an {@code Optional} containing the description author JID, or
     *         empty if not known
     */
    public Optional<Jid> descriptionAuthorJid() {
        return Optional.ofNullable(descriptionAuthorJid);
    }

    /**
     * Sets the JID of the participant who last changed the description.
     *
     * @param descriptionAuthorJid the author JID, or {@code null} to clear
     */
    public void setDescriptionAuthorJid(Jid descriptionAuthorJid) {
        this.descriptionAuthorJid = descriptionAuthorJid;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no participants have been added an empty set is returned.
     */
    @Override
    public Set<GroupParticipant> participants() {
        return participants == null ? Set.of() : Collections.unmodifiableSet(participants);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addParticipant(GroupParticipant participant) {
        return participants.add(participant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeParticipant(GroupParticipant participant) {
        return participants.remove(participant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeParticipant(Jid jid) {
        return participants.removeIf(participant -> participant.userJid().equals(jid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearParticipants() {
        participants.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAllParticipants(Collection<GroupParticipant> participants) {
        return this.participants.addAll(participants);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ChatEphemeralTimer> ephemeralExpiration() {
        return Optional.ofNullable(ephemeralExpiration);
    }

    /**
     * Sets the ephemeral message timer for this community.
     *
     * @param ephemeralExpiration the ephemeral timer to set, or {@code null}
     *        to disable ephemeral messaging
     */
    @Override
    public void setEphemeralExpiration(ChatEphemeralTimer ephemeralExpiration) {
        this.ephemeralExpiration = ephemeralExpiration;
    }

    /**
     * Returns whether metadata editing is restricted to administrators.
     * When {@code true}, only administrators can change the community's
     * subject, description, and profile picture.
     *
     * @return {@code true} if only administrators can edit metadata
     */
    public boolean isRestrict() {
        return restrict;
    }

    /**
     * Sets whether metadata editing is restricted to administrators.
     *
     * @param restrict {@code true} to restrict to admins
     */
    public void setRestrict(boolean restrict) {
        this.restrict = restrict;
    }

    /**
     * Returns whether the community is in announcement mode.
     *
     * @return {@code true} if only administrators can send messages
     */
    public boolean isAnnounce() {
        return announce;
    }

    /**
     * Sets whether the community is in announcement mode.
     *
     * @param announce {@code true} to enable announcement mode
     */
    public void setAnnounce(boolean announce) {
        this.announce = announce;
    }

    /**
     * Returns an unmodifiable view of the subgroups linked to this
     * community. The set includes the default announcement subgroup,
     * general chat subgroup, and any additional subgroups. If no subgroups
     * have been linked, an empty set is returned.
     *
     * @return an unmodifiable {@code SequencedSet} of linked groups, never
     *         {@code null}
     */
    public SequencedSet<CommunityLinkedGroup> communityGroups() {
        return communityGroups == null
                ? Collections.emptyNavigableSet()
                : Collections.unmodifiableSequencedSet(communityGroups);
    }

    /**
     * Sets the collection of subgroups linked to this community, replacing
     * any previously linked subgroups.
     *
     * @param communityGroups the linked subgroups, or {@code null} to clear
     */
    public void setCommunityGroups(SequencedSet<CommunityLinkedGroup> communityGroups) {
        this.communityGroups = Objects.requireNonNullElseGet(communityGroups, LinkedHashSet::new);
    }

    /**
     * Adds a linked subgroup to this community.
     *
     * @param group the non-{@code null} linked group to add
     */
    public void addCommunityGroup(CommunityLinkedGroup group) {
        communityGroups.add(group);
    }

    /**
     * Removes the specified linked subgroup from this community.
     *
     * @param group the non-{@code null} linked group to remove
     * @return {@code true} if the group was present and removed
     */
    public boolean removeCommunityGroup(CommunityLinkedGroup group) {
        return communityGroups.remove(group);
    }

    /**
     * Removes the linked subgroup identified by the given JID from this
     * community.
     *
     * @param jid the non-{@code null} JID of the linked group to remove
     * @return {@code true} if a matching group was found and removed
     */
    public boolean removeCommunityGroup(Jid jid) {
        return communityGroups.removeIf(group -> group.jid().equals(jid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLidAddressingMode() {
        return isLidAddressingMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLidAddressingMode(boolean lidAddressingMode) {
        this.isLidAddressingMode = lidAddressingMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIncognito() {
        return isIncognito;
    }

    /**
     * Sets whether this community operates in incognito mode.
     *
     * @param incognito {@code true} to enable incognito mode, {@code false}
     *                  to disable it
     */
    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
    }

    /**
     * Returns whether frequently forwarded messages are blocked in this
     * community.
     *
     * @return {@code true} if frequently forwarded messages are blocked
     */
    public boolean isNoFrequentlyForwarded() {
        return noFrequentlyForwarded;
    }

    /**
     * Sets whether frequently forwarded messages are blocked.
     *
     * @param noFrequentlyForwarded {@code true} to block forwarded messages
     */
    public void setNoFrequentlyForwarded(boolean noFrequentlyForwarded) {
        this.noFrequentlyForwarded = noFrequentlyForwarded;
    }

    /**
     * Returns whether admin approval is required for new members.
     *
     * @return {@code true} if membership approval mode is enabled
     */
    public boolean isMembershipApprovalMode() {
        return membershipApprovalMode;
    }

    /**
     * Sets whether admin approval is required for new members.
     *
     * @param membershipApprovalMode {@code true} to enable approval mode
     */
    public void setMembershipApprovalMode(boolean membershipApprovalMode) {
        this.membershipApprovalMode = membershipApprovalMode;
    }

    /**
     * Returns whether invite link sharing is restricted to administrators.
     *
     * @return {@code true} if only admins can share invite links
     */
    public boolean isMemberLinkModeAdminOnly() {
        return memberLinkModeAdminOnly;
    }

    /**
     * Sets whether invite link usage is restricted to administrators.
     *
     * @param memberLinkModeAdminOnly {@code true} to restrict to admins
     */
    public void setMemberLinkModeAdminOnly(boolean memberLinkModeAdminOnly) {
        this.memberLinkModeAdminOnly = memberLinkModeAdminOnly;
    }

    /**
     * Returns whether non-admin members can create or link subgroups.
     *
     * @return {@code true} if non-admins are allowed to create subgroups
     */
    public boolean isAllowNonAdminSubGroupCreation() {
        return allowNonAdminSubGroupCreation;
    }

    /**
     * Sets whether non-admin members can create or link subgroups.
     *
     * @param allowNonAdminSubGroupCreation {@code true} to allow non-admin
     *                                      subgroup creation
     */
    public void setAllowNonAdminSubGroupCreation(boolean allowNonAdminSubGroupCreation) {
        this.allowNonAdminSubGroupCreation = allowNonAdminSubGroupCreation;
    }

    /**
     * Returns the {@link ChatPolicy} corresponding to the
     * {@link #isAllowNonAdminSubGroupCreation()} state.
     *
     * @return {@link ChatPolicy#ANYONE} if non-admins are allowed,
     *         {@link ChatPolicy#ADMINS} otherwise
     */
    public ChatPolicy allowNonAdminSubGroupCreationPolicy() {
        return allowNonAdminSubGroupCreation ? ChatPolicy.ANYONE : ChatPolicy.ADMINS;
    }

    /**
     * Returns whether only administrators can add members.
     *
     * @return {@code true} if member addition is restricted to admins
     */
    public boolean isMemberAddModeAdminOnly() {
        return memberAddModeAdminOnly;
    }

    /**
     * Sets whether only administrators can add members.
     *
     * @param memberAddModeAdminOnly {@code true} to restrict to admins
     */
    public void setMemberAddModeAdminOnly(boolean memberAddModeAdminOnly) {
        this.memberAddModeAdminOnly = memberAddModeAdminOnly;
    }

    /**
     * Returns the {@link ChatPolicy} corresponding to the
     * {@link #isMemberAddModeAdminOnly()} state.
     *
     * @return {@link ChatPolicy#ADMINS} if member addition is
     *         restricted, {@link ChatPolicy#ANYONE} otherwise
     */
    public ChatPolicy memberAddModePolicy() {
        return memberAddModeAdminOnly ? ChatPolicy.ADMINS : ChatPolicy.ANYONE;
    }

    /**
     * Returns the instant at which the growth lock expires, if active.
     *
     * @return an {@code Optional} containing the growth lock expiration, or
     *         empty if no growth lock is active
     */
    public Optional<Instant> growthLockExpiration() {
        return Optional.ofNullable(growthLockExpiration);
    }

    /**
     * Sets the instant at which the growth lock expires.
     *
     * @param growthLockExpiration the expiration instant, or {@code null}
     *                            to clear
     */
    public void setGrowthLockExpiration(Instant growthLockExpiration) {
        this.growthLockExpiration = growthLockExpiration;
    }

    /**
     * Returns the type of growth lock applied, if any.
     *
     * @return an {@code Optional} containing the growth lock type, or empty
     *         if no growth lock is active
     */
    public Optional<String> growthLockType() {
        return Optional.ofNullable(growthLockType);
    }

    /**
     * Sets the type of growth lock applied to this community.
     *
     * @param growthLockType the growth lock type, or {@code null} to clear
     */
    public void setGrowthLockType(String growthLockType) {
        this.growthLockType = growthLockType;
    }

    /**
     * Returns whether the report-to-admin feature is enabled.
     *
     * @return {@code true} if report-to-admin is enabled
     */
    public boolean isReportToAdminMode() {
        return reportToAdminMode;
    }

    /**
     * Sets whether the report-to-admin feature is enabled.
     *
     * @param reportToAdminMode {@code true} to enable
     */
    public void setReportToAdminMode(boolean reportToAdminMode) {
        this.reportToAdminMode = reportToAdminMode;
    }

    /**
     * Returns the instant of the last report-to-admin event, if any.
     *
     * @return an {@code Optional} containing the timestamp, or empty if no
     *         event has occurred
     */
    public Optional<Instant> lastReportToAdminTimestamp() {
        return Optional.ofNullable(lastReportToAdminTimestamp);
    }

    /**
     * Sets the instant of the last report-to-admin event.
     *
     * @param lastReportToAdminTimestamp the timestamp, or {@code null} to
     *                                  clear
     */
    public void setLastReportToAdminTimestamp(Instant lastReportToAdminTimestamp) {
        this.lastReportToAdminTimestamp = lastReportToAdminTimestamp;
    }

    /**
     * Returns the server-reported total participant count, if available.
     * This value may differ from the size of the {@link #participants()}
     * set if the full participant list has not been fetched.
     *
     * @return an {@code OptionalInt} containing the count, or empty if not
     *         available
     */
    public OptionalInt size() {
        return size == null ? OptionalInt.empty() : OptionalInt.of(size);
    }

    /**
     * Sets the server-reported participant count.
     *
     * @param size the participant count, or {@code null} to clear
     */
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * Returns whether this is a WhatsApp support group. Support groups
     * have restricted administrative capabilities.
     *
     * @return {@code true} if this is a support group
     */
    public boolean isSupport() {
        return support;
    }

    /**
     * Sets whether this is a support group.
     *
     * @param support {@code true} to mark as support
     */
    public void setSupport(boolean support) {
        this.support = support;
    }

    /**
     * Returns whether this community has been suspended by WhatsApp. A
     * suspended community cannot receive or send messages.
     *
     * @return {@code true} if suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets whether this community is suspended.
     *
     * @param suspended {@code true} to mark as suspended
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Returns whether this community has been permanently terminated. A
     * terminated community cannot be restored, unlike a suspended one.
     *
     * @return {@code true} if terminated
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Sets whether this community is terminated.
     *
     * @param terminated {@code true} to mark as terminated
     */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * Returns whether the community is closed. A closed community requires
     * membership approval for all join requests.
     *
     * @return {@code true} if the community is closed
     */
    public boolean isParentGroupClosed() {
        return isParentGroupClosed;
    }

    /**
     * Sets whether the community is closed.
     *
     * @param parentGroupClosed {@code true} to mark as closed
     */
    public void setParentGroupClosed(boolean parentGroupClosed) {
        this.isParentGroupClosed = parentGroupClosed;
    }

    /**
     * Returns whether this community contains a default announcement
     * subgroup.
     *
     * @return {@code true} if a default announcement subgroup exists
     */
    public boolean isDefaultSubgroup() {
        return defaultSubgroup;
    }

    /**
     * Sets whether this community contains a default announcement subgroup.
     *
     * @param defaultSubgroup {@code true} if a default announcement
     *                        subgroup exists
     */
    public void setDefaultSubgroup(boolean defaultSubgroup) {
        this.defaultSubgroup = defaultSubgroup;
    }

    /**
     * Returns whether this community contains a general chat subgroup.
     *
     * @return {@code true} if a general chat subgroup exists
     */
    public boolean isGeneralSubgroup() {
        return generalSubgroup;
    }

    /**
     * Sets whether this community contains a general chat subgroup.
     *
     * @param generalSubgroup {@code true} if a general chat subgroup exists
     */
    public void setGeneralSubgroup(boolean generalSubgroup) {
        this.generalSubgroup = generalSubgroup;
    }

    /**
     * Returns whether this community contains a hidden subgroup.
     *
     * @return {@code true} if a hidden subgroup exists
     */
    public boolean isHiddenSubgroup() {
        return hiddenSubgroup;
    }

    /**
     * Sets whether this community has a hidden subgroup.
     *
     * @param hiddenSubgroup {@code true} if a hidden subgroup exists
     */
    public void setHiddenSubgroup(boolean hiddenSubgroup) {
        this.hiddenSubgroup = hiddenSubgroup;
    }

    /**
     * Returns whether the group safety check flag is set for this
     * community.
     *
     * @return {@code true} if the safety check is set
     */
    public boolean isGroupSafetyCheck() {
        return groupSafetyCheck;
    }

    /**
     * Sets the group safety check flag.
     *
     * @param groupSafetyCheck {@code true} to set the flag
     */
    public void setGroupSafetyCheck(boolean groupSafetyCheck) {
        this.groupSafetyCheck = groupSafetyCheck;
    }

    /**
     * Returns the JID of the user who added the current user to this
     * community, if known.
     *
     * @return an {@code Optional} containing the adder JID, or empty if
     *         not known
     */
    public Optional<Jid> groupAdder() {
        return Optional.ofNullable(groupAdder);
    }

    /**
     * Sets the JID of the user who added the current user to this community.
     *
     * @param groupAdder the adder JID, or {@code null} to clear
     */
    public void setGroupAdder(Jid groupAdder) {
        this.groupAdder = groupAdder;
    }

    /**
     * Returns whether automatic addition of new community members to the
     * general chat subgroup is disabled.
     *
     * @return {@code true} if auto-add is disabled
     */
    public boolean isGeneralChatAutoAddDisabled() {
        return generalChatAutoAddDisabled;
    }

    /**
     * Sets whether auto-add to general chat is disabled.
     *
     * @param generalChatAutoAddDisabled {@code true} to disable auto-add
     */
    public void setGeneralChatAutoAddDisabled(boolean generalChatAutoAddDisabled) {
        this.generalChatAutoAddDisabled = generalChatAutoAddDisabled;
    }

    /**
     * Returns the instant of the last community poll, if any.
     *
     * @return an {@code Optional} containing the timestamp, or empty if no
     *         poll has occurred
     */
    public Optional<Instant> lastCommunityPollTimestamp() {
        return Optional.ofNullable(lastCommunityPollTimestamp);
    }

    /**
     * Sets the instant of the last community poll.
     *
     * @param lastCommunityPollTimestamp the timestamp, or {@code null} to
     *                                  clear
     */
    public void setLastCommunityPollTimestamp(Instant lastCommunityPollTimestamp) {
        this.lastCommunityPollTimestamp = lastCommunityPollTimestamp;
    }

    /**
     * Returns the instant of the last activity, if available.
     *
     * @return an {@code Optional} containing the timestamp, or empty if
     *         not available
     */
    public Optional<Instant> lastActivityTimestamp() {
        return Optional.ofNullable(lastActivityTimestamp);
    }

    /**
     * Sets the instant of the last activity.
     *
     * @param lastActivityTimestamp the timestamp, or {@code null} to clear
     */
    public void setLastActivityTimestamp(Instant lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }

    /**
     * Returns the instant of the last seen activity, if available.
     *
     * @return an {@code Optional} containing the timestamp, or empty if
     *         not available
     */
    public Optional<Instant> lastSeenActivityTimestamp() {
        return Optional.ofNullable(lastSeenActivityTimestamp);
    }

    /**
     * Sets the instant of the last seen activity.
     *
     * @param lastSeenActivityTimestamp the timestamp, or {@code null} to
     *                                 clear
     */
    public void setLastSeenActivityTimestamp(Instant lastSeenActivityTimestamp) {
        this.lastSeenActivityTimestamp = lastSeenActivityTimestamp;
    }

    /**
     * Returns whether this community has CAPI (Community API) capabilities
     * enabled for business integrations.
     *
     * @return {@code true} if CAPI is available
     */
    public boolean hasCapi() {
        return hasCapi;
    }

    /**
     * Sets whether this community has CAPI capabilities.
     *
     * @param hasCapi {@code true} to enable
     */
    public void setHasCapi(boolean hasCapi) {
        this.hasCapi = hasCapi;
    }

    /**
     * Returns whether this community is a TEE (Trusted Execution
     * Environment) bot group.
     *
     * @return {@code true} if this is a TEE bot group
     */
    public boolean isTeeBotGroup() {
        return isTeeBotGroup;
    }

    /**
     * Sets whether this community is a TEE bot group.
     *
     * @param teeBotGroup {@code true} to mark as TEE bot group
     */
    public void setTeeBotGroup(boolean teeBotGroup) {
        this.isTeeBotGroup = teeBotGroup;
    }

    /**
     * Returns the trigger that caused disappearing message mode to be set,
     * if any.
     *
     * @return an {@code Optional} containing the trigger, or empty if not
     *         set
     */
    public Optional<ChatDisappearingMode.Trigger> disappearingModeTrigger() {
        return Optional.ofNullable(disappearingModeTrigger);
    }

    /**
     * Sets the trigger that caused disappearing message mode.
     *
     * @param disappearingModeTrigger the trigger, or {@code null} to clear
     */
    public void setDisappearingModeTrigger(ChatDisappearingMode.Trigger disappearingModeTrigger) {
        this.disappearingModeTrigger = disappearingModeTrigger;
    }

    /**
     * Returns whether the current user initiated the disappearing message
     * mode.
     *
     * @return {@code true} if initiated by the current user
     */
    public boolean isDisappearingModeInitiatedByMe() {
        return disappearingModeInitiatedByMe;
    }

    /**
     * Sets whether the current user initiated the disappearing message mode.
     *
     * @param disappearingModeInitiatedByMe {@code true} if initiated by
     *                                      current user
     */
    public void setDisappearingModeInitiatedByMe(boolean disappearingModeInitiatedByMe) {
        this.disappearingModeInitiatedByMe = disappearingModeInitiatedByMe;
    }

    /**
     * Returns the number of subgroups, if available.
     *
     * @return an {@code OptionalInt} containing the count, or empty if not
     *         available
     */
    public OptionalInt numSubgroups() {
        return numSubgroups == null ? OptionalInt.empty() : OptionalInt.of(numSubgroups);
    }

    /**
     * Sets the number of subgroups.
     *
     * @param numSubgroups the subgroup count, or {@code null} to clear
     */
    public void setNumSubgroups(Integer numSubgroups) {
        this.numSubgroups = numSubgroups;
    }

    /**
     * Returns whether the limit sharing feature is enabled.
     *
     * @return {@code true} if limit sharing is enabled
     */
    public boolean isLimitSharingEnabled() {
        return limitSharingEnabled;
    }

    /**
     * Sets whether the limit sharing feature is enabled.
     *
     * @param limitSharingEnabled {@code true} to enable
     */
    public void setLimitSharingEnabled(boolean limitSharingEnabled) {
        this.limitSharingEnabled = limitSharingEnabled;
    }

    /**
     * Returns the group evolution version, if available.
     *
     * @return an {@code OptionalInt} containing the version, or empty if
     *         not available
     */
    public OptionalInt evolutionVersion() {
        return evolutionVersion == null ? OptionalInt.empty() : OptionalInt.of(evolutionVersion);
    }

    /**
     * Sets the group evolution version.
     *
     * @param evolutionVersion the version, or {@code null} to clear
     */
    public void setEvolutionVersion(Integer evolutionVersion) {
        this.evolutionVersion = evolutionVersion;
    }

    /**
     * Returns whether participant labels are enabled.
     *
     * @return {@code true} if participant labels are enabled
     */
    public boolean isParticipantLabelEnabled() {
        return participantLabelEnabled;
    }

    /**
     * Sets whether participant labels are enabled.
     *
     * @param participantLabelEnabled {@code true} to enable
     */
    public void setParticipantLabelEnabled(boolean participantLabelEnabled) {
        this.participantLabelEnabled = participantLabelEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpenBotGroup() {
        return isOpenBotGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOpenBotGroup(boolean openBotGroup) {
        this.isOpenBotGroup = openBotGroup;
    }

    /**
     * Returns whether this community metadata is equal to another object.
     * Two {@code CommunityMetadata} instances are considered equal if and
     * only if they have the same {@link #jid()}.
     *
     * @param o the object to compare with
     * @return {@code true} if the given object is a {@code CommunityMetadata}
     *         with the same JID
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof CommunityMetadata that
                && Objects.equals(jid, that.jid);
    }

    /**
     * Returns a hash code based on this community's {@link #jid()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(jid);
    }

    /**
     * Returns a string representation of this community metadata, including
     * the JID and subject.
     *
     * @return a string in the format
     *         {@code CommunityMetadata[jid=..., subject=...]}
     */
    @Override
    public String toString() {
        return "CommunityMetadata[jid=" + jid + ", subject=" + subject + ']';
    }
}
