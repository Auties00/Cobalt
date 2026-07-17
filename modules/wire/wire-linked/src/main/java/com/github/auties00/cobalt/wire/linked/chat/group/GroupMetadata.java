package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.ChatPolicy;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;

/**
 * Represents the metadata of a WhatsApp group chat.
 *
 * <p>A group is a multi-participant chat entity on WhatsApp that carries
 * administrative metadata such as a subject (display name), a founder, an
 * optional description, a participant list, per-group permission settings,
 * ephemeral-message configuration, addressing-mode flags, and an optional link
 * to a parent community.
 *
 * <p>Group metadata captures the identity of the group (its JID and subject),
 * its origin (the founder and foundation timestamp), an optional free-form
 * description with a server-assigned revision identifier, the current list of
 * participants, the group-level administrative settings, the ephemeral message
 * expiration timer, addressing-mode flags, and the open-bot-group toggle.
 *
 * <p>Several group permission settings such as {@code restrict},
 * {@code announce}, {@code memberAddMode}, {@code membershipApprovalMode},
 * and {@code memberLinkMode} are stored internally as boolean fields but
 * exposed through public accessors that return {@link ChatPolicy} values for
 * clarity. A value of {@link ChatPolicy#ADMINS} indicates the action is
 * restricted to administrators, while {@link ChatPolicy#ANYONE} indicates all
 * members are allowed.
 *
 * <p>Groups linked to a community can be identified through the subgroup
 * flags: {@link #isDefaultSubgroup()} returns {@code true} for the Community
 * Announcement Group, {@link #isGeneralSubgroup()} returns {@code true} for
 * the general chat, and {@link #parentCommunityJid()} is present for any
 * linked subgroup.
 *
 * <p>Instances of this class are mutable. All fields can be changed after
 * construction through the setter methods. The participant collection
 * additionally exposes dedicated add, remove, and clear operations.
 *
 * @see ChatMetadata
 * @see ChatPolicy
 */
@ProtobufMessage
public final class GroupMetadata implements ChatMetadata {
    /**
     * The JID that uniquely identifies this group. Required, never
     * {@code null} after construction.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The subject (display name) of this group. Set by a group administrator
     * and visible to all participants. Required, never {@code null} after
     * construction.
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
     * The instant at which the subject was last changed, or {@code null} if
     * the timestamp is not available.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant subjectTimestamp;

    /**
     * The instant at which this group was created, or {@code null} if the
     * timestamp is not available.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant foundationTimestamp;

    /**
     * The JID of the user who originally created this group (the founder or
     * super-admin), or {@code null} if the founder is not known.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    Jid founderJid;

    /**
     * The free-form description text of this group, or {@code null} if no
     * description has been set.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String description;

    /**
     * The server-assigned identifier for the current description revision,
     * or {@code null} if no description identifier is available. Used by
     * the server to detect conflicting concurrent edits to the description.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String descriptionId;

    /**
     * The instant at which the description was last changed, or {@code null}
     * if the timestamp is not available.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant descriptionTimestamp;

    /**
     * The ordered set of participants currently in this group. Insertion
     * order is preserved. If no participants have been added, an empty
     * mutable set is used.
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
     * The timer for automatic message disappearance in this group. When set
     * to a non-{@link ChatEphemeralTimer#OFF} value, messages sent to this
     * group are automatically deleted after the specified duration. A
     * {@code null} or {@link ChatEphemeralTimer#OFF} value disables
     * ephemeral messaging.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT32)
    ChatEphemeralTimer ephemeralExpiration;

    /**
     * Whether metadata editing is restricted to administrators only. When
     * {@code true}, only administrators can change the group's subject,
     * description, and profile picture. Exposed through {@link #restrict()}
     * as a {@link ChatPolicy}.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    boolean restrict;

    /**
     * Whether the group is in announcement mode. When {@code true}, only
     * administrators can send messages. Exposed through {@link #announce()}
     * as a {@link ChatPolicy}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    boolean announce;

    /**
     * The JID of the parent community this group is linked to, or
     * {@code null} if the group is standalone.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    Jid parentCommunityJid;

    /**
     * Whether this group uses LID (Linked Identity) addressing mode instead
     * of traditional phone-number-based addressing. LID addressing hides
     * phone numbers between participants who do not share them as contacts.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    boolean isLidAddressingMode;

    /**
     * Whether this group operates in incognito mode.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.BOOL)
    boolean isIncognito;

    /**
     * Whether frequently forwarded messages (messages forwarded many times)
     * are blocked from being sent to this group.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BOOL)
    boolean noFrequentlyForwarded;

    /**
     * Whether administrator approval is required for new members to join
     * this group. When {@code true}, prospective members must have their
     * join request approved by an administrator before joining. Exposed
     * through {@link #membershipApprovalMode()} as a {@link ChatPolicy}.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    boolean membershipApprovalMode;

    /**
     * Whether invite link usage is restricted to administrators. When
     * {@code true}, only administrators can share and use invite links to
     * the group. Exposed through {@link #memberLinkMode()} as a
     * {@link ChatPolicy}.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.BOOL)
    boolean memberLinkModeAdminOnly;

    /**
     * Whether only administrators can add new members to this group. When
     * {@code true}, adding members is restricted to administrators. Exposed
     * through {@link #memberAddMode()} as a {@link ChatPolicy}.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    boolean memberAddModeAdminOnly;

    /**
     * The instant at which an active growth lock expires, or {@code null}
     * if no growth lock is active. Growth locks temporarily prevent rapid
     * growth of a group (for example, after anti-spam measures trigger).
     */
    @ProtobufProperty(index = 22, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant growthLockExpiration;

    /**
     * The type of growth lock applied to this group (for example
     * {@code "invite"}), or {@code null} if no growth lock is active.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.STRING)
    String growthLockType;

    /**
     * Whether the "report to admin" feature is enabled for this group.
     * When enabled, members can report specific messages to the group's
     * administrators.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.BOOL)
    boolean reportToAdminMode;

    /**
     * The instant at which the most recent report-to-admin event occurred,
     * or {@code null} if no such event has occurred.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastReportToAdminTimestamp;

    /**
     * The server-reported participant count, or {@code null} if not
     * available. Note that this may differ from the size of
     * {@link #participants()} when the local participant list is not fully
     * synchronized with the server.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.UINT32)
    Integer size;

    /**
     * Whether this is a WhatsApp support group used for customer support
     * interactions.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.BOOL)
    boolean support;

    /**
     * Whether this group has been suspended by WhatsApp. A suspended group
     * cannot be interacted with until it is restored.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.BOOL)
    boolean suspended;

    /**
     * Whether this group has been terminated. A terminated group is
     * permanently closed and cannot be restored.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    boolean terminated;

    /**
     * Whether this group is the default subgroup (Community Announcement
     * Group) of a community. The announcement group is the main group in a
     * community where administrators can broadcast messages to all
     * community members.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    boolean defaultSubgroup;

    /**
     * Whether this group is the general chat subgroup of a community. The
     * general chat is an open discussion group that community members can
     * join automatically.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.BOOL)
    boolean generalSubgroup;

    /**
     * Whether this group is a hidden subgroup within a community. Hidden
     * subgroups are not visible in the community's list of subgroups.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.BOOL)
    boolean hiddenSubgroup;

    /**
     * Whether the group safety check flag is set. Safety checks flag groups
     * that may require additional moderation attention.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.BOOL)
    boolean groupSafetyCheck;

    /**
     * The JID of the user who added the current user to this group, or
     * {@code null} if not known. Used to display the "added by" information
     * in the group info screen.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.STRING)
    Jid groupAdder;

    /**
     * Whether automatic addition to the general chat of the parent community
     * is disabled for this group.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    boolean generalChatAutoAddDisabled;

    /**
     * The instant of the last activity (any message or event) in this
     * group, or {@code null} if not available.
     */
    @ProtobufProperty(index = 36, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastActivityTimestamp;

    /**
     * The instant at which the current user last viewed activity in this
     * group, or {@code null} if not available.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant lastSeenActivityTimestamp;

    /**
     * Whether this group has CAPI (Community API) capabilities enabled.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.BOOL)
    boolean hasCapi;

    /**
     * Whether this group is a TEE (Trusted Execution Environment) bot
     * group. TEE bot groups use a secure execution environment for bot
     * interactions.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.BOOL)
    boolean isTeeBotGroup;

    /**
     * The trigger that caused the disappearing message mode to be set for
     * this group, or {@code null} if not set. The trigger indicates whether
     * the timer was set manually, by account default, or by some other
     * means.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.ENUM)
    ChatDisappearingMode.Trigger disappearingModeTrigger;

    /**
     * Whether the current user initiated the disappearing message mode
     * change for this group.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    boolean disappearingModeInitiatedByMe;

    /**
     * Whether the limit sharing feature is enabled for this group. Limit
     * sharing restricts certain message sharing behaviors to reduce spam.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.BOOL)
    boolean limitSharingEnabled;

    /**
     * The group evolution version, or {@code null} if not available. Used
     * to track structural migrations of the group.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.UINT32)
    Integer evolutionVersion;

    /**
     * Whether participant labels (member tags) are enabled for this group.
     * When enabled, administrators can assign {@link GroupParticipantLabel}
     * tags to individual participants.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.BOOL)
    boolean participantLabelEnabled;

    /**
     * Whether the status updates posted by this group are muted for the
     * current user. When muted, the group's status updates are hidden from
     * the status tab, but messages in the group itself remain unaffected.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.BOOL)
    boolean statusMuted;

    /**
     * Whether this group has the open Meta AI bot feature enabled. This
     * field is not serialized as a protobuf property and is instead
     * populated at runtime from the group query response when AI bot
     * participants are detected.
     */
    boolean isOpenBotGroup;

    /**
     * Constructs a new {@code GroupMetadata} with the specified values.
     *
     * <p>The {@code jid} and {@code subject} parameters are required and
     * must not be {@code null}. All other parameters accept {@code null} to
     * indicate an absent or unknown value. The {@code participants}
     * parameter, if {@code null}, is replaced with an empty mutable
     * collection so that callers can safely add elements after construction.
     *
     * @param jid                          the non-{@code null} group JID
     * @param subject                      the non-{@code null} subject
     * @param subjectAuthorJid             the author of the last subject
     *                                     change, or {@code null}
     * @param subjectTimestamp              the subject change instant, or
     *                                     {@code null}
     * @param foundationTimestamp           the group creation instant, or
     *                                     {@code null}
     * @param founderJid                   the founder JID, or {@code null}
     * @param description                  the description text, or
     *                                     {@code null}
     * @param descriptionId                the server-assigned description
     *                                     revision identifier, or
     *                                     {@code null}
     * @param descriptionTimestamp         the description change instant, or
     *                                     {@code null}
     * @param participants                 the participant set, or
     *                                     {@code null}
     * @param descriptionAuthorJid         the description author JID, or
     *                                     {@code null}
     * @param ephemeralExpiration          the ephemeral timer, or
     *                                     {@code null} to disable
     * @param restrict                     whether metadata editing is
     *                                     restricted to admins
     * @param announce                     whether announcement mode is on
     * @param parentCommunityJid           the parent community JID, or
     *                                     {@code null}
     * @param isLidAddressingMode          whether LID addressing is enabled
     * @param isIncognito                  whether incognito mode is enabled
     * @param noFrequentlyForwarded        whether forwarded messages are
     *                                     blocked
     * @param membershipApprovalMode       whether admin approval is required
     * @param memberLinkModeAdminOnly      whether invite links are admin-only
     * @param memberAddModeAdminOnly       whether only admins can add members
     * @param growthLockExpiration         the growth lock expiration instant,
     *                                     or {@code null}
     * @param growthLockType               the growth lock type, or
     *                                     {@code null}
     * @param reportToAdminMode            whether report-to-admin is enabled
     * @param lastReportToAdminTimestamp   the last report-to-admin instant,
     *                                     or {@code null}
     * @param size                         the server-reported participant
     *                                     count, or {@code null}
     * @param support                      whether this is a support group
     * @param suspended                    whether the group is suspended
     * @param terminated                   whether the group is terminated
     * @param defaultSubgroup              whether this is a default subgroup
     * @param generalSubgroup              whether this is a general subgroup
     * @param hiddenSubgroup               whether this is a hidden subgroup
     * @param groupSafetyCheck             whether the safety check flag is
     *                                     set
     * @param groupAdder                   the JID of who added the user, or
     *                                     {@code null}
     * @param generalChatAutoAddDisabled   whether auto-add to general chat
     *                                     is disabled
     * @param lastActivityTimestamp        the last activity instant, or
     *                                     {@code null}
     * @param lastSeenActivityTimestamp    the last seen activity instant, or
     *                                     {@code null}
     * @param hasCapi                      whether CAPI is available
     * @param isTeeBotGroup                whether this is a TEE bot group
     * @param disappearingModeTrigger      the disappearing mode trigger, or
     *                                     {@code null}
     * @param disappearingModeInitiatedByMe whether the user initiated
     *                                     disappearing mode
     * @param limitSharingEnabled          whether limit sharing is enabled
     * @param evolutionVersion             the evolution version, or
     *                                     {@code null}
     * @param participantLabelEnabled      whether participant labels are
     *                                     enabled
     * @param statusMuted                  whether this group's status updates
     *                                     are muted for the current user
     */
    GroupMetadata(
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
            Jid parentCommunityJid,
            boolean isLidAddressingMode,
            boolean isIncognito,
            boolean noFrequentlyForwarded,
            boolean membershipApprovalMode,
            boolean memberLinkModeAdminOnly,
            boolean memberAddModeAdminOnly,
            Instant growthLockExpiration,
            String growthLockType,
            boolean reportToAdminMode,
            Instant lastReportToAdminTimestamp,
            Integer size,
            boolean support,
            boolean suspended,
            boolean terminated,
            boolean defaultSubgroup,
            boolean generalSubgroup,
            boolean hiddenSubgroup,
            boolean groupSafetyCheck,
            Jid groupAdder,
            boolean generalChatAutoAddDisabled,
            Instant lastActivityTimestamp,
            Instant lastSeenActivityTimestamp,
            boolean hasCapi,
            boolean isTeeBotGroup,
            ChatDisappearingMode.Trigger disappearingModeTrigger,
            boolean disappearingModeInitiatedByMe,
            boolean limitSharingEnabled,
            Integer evolutionVersion,
            boolean participantLabelEnabled,
            boolean statusMuted
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
        this.parentCommunityJid = parentCommunityJid;
        this.isLidAddressingMode = isLidAddressingMode;
        this.isIncognito = isIncognito;
        this.noFrequentlyForwarded = noFrequentlyForwarded;
        this.membershipApprovalMode = membershipApprovalMode;
        this.memberLinkModeAdminOnly = memberLinkModeAdminOnly;
        this.memberAddModeAdminOnly = memberAddModeAdminOnly;
        this.growthLockExpiration = growthLockExpiration;
        this.growthLockType = growthLockType;
        this.reportToAdminMode = reportToAdminMode;
        this.lastReportToAdminTimestamp = lastReportToAdminTimestamp;
        this.size = size;
        this.support = support;
        this.suspended = suspended;
        this.terminated = terminated;
        this.defaultSubgroup = defaultSubgroup;
        this.generalSubgroup = generalSubgroup;
        this.hiddenSubgroup = hiddenSubgroup;
        this.groupSafetyCheck = groupSafetyCheck;
        this.groupAdder = groupAdder;
        this.generalChatAutoAddDisabled = generalChatAutoAddDisabled;
        this.lastActivityTimestamp = lastActivityTimestamp;
        this.lastSeenActivityTimestamp = lastSeenActivityTimestamp;
        this.hasCapi = hasCapi;
        this.isTeeBotGroup = isTeeBotGroup;
        this.disappearingModeTrigger = disappearingModeTrigger;
        this.disappearingModeInitiatedByMe = disappearingModeInitiatedByMe;
        this.limitSharingEnabled = limitSharingEnabled;
        this.evolutionVersion = evolutionVersion;
        this.participantLabelEnabled = participantLabelEnabled;
        this.statusMuted = statusMuted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Jid jid() {
        return jid;
    }

    /**
     * Sets the JID that identifies this group.
     *
     * @param jid the group JID
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
     * Sets the subject (display name) of this group.
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
     * Sets the instant at which this group was created.
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
     * Sets the JID of the user who originally created this group.
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
     * Sets the free-form description text of this group.
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
     * Sets the ephemeral message timer for this group.
     *
     * @param ephemeralExpiration the ephemeral timer to set, or {@code null}
     *        to disable ephemeral messaging
     */
    @Override
    public void setEphemeralExpiration(ChatEphemeralTimer ephemeralExpiration) {
        this.ephemeralExpiration = ephemeralExpiration;
    }

    /**
     * Returns the policy that determines who can edit the group's metadata
     * (subject, description, and profile picture).
     *
     * <p>A return value of {@link ChatPolicy#ADMINS} indicates that only
     * administrators can edit group information. A return value of
     * {@link ChatPolicy#ANYONE} indicates that all participants can edit it.
     *
     * @return the non-{@code null} policy for metadata editing
     */
    public ChatPolicy restrict() {
        return ChatPolicy.of(restrict);
    }

    /**
     * Sets the policy that determines who can edit the group's metadata.
     *
     * @param policy the non-{@code null} policy to set
     */
    public void setRestrict(ChatPolicy policy) {
        this.restrict = policy == ChatPolicy.ADMINS;
    }

    /**
     * Returns the policy that determines who can send messages in this
     * group.
     *
     * <p>A return value of {@link ChatPolicy#ADMINS} indicates that the
     * group is in announcement mode and only administrators can send
     * messages. A return value of {@link ChatPolicy#ANYONE} indicates that
     * all participants can send messages.
     *
     * @return the non-{@code null} policy for sending messages
     */
    public ChatPolicy announce() {
        return ChatPolicy.of(announce);
    }

    /**
     * Sets the policy that determines who can send messages in this group.
     *
     * @param policy the non-{@code null} policy to set
     */
    public void setAnnounce(ChatPolicy policy) {
        this.announce = policy == ChatPolicy.ADMINS;
    }

    /**
     * Returns the JID of the parent community this group is linked to, if
     * any.
     *
     * @return an {@code Optional} containing the parent community JID, or
     *         empty if this group is standalone
     */
    public Optional<Jid> parentCommunityJid() {
        return Optional.ofNullable(parentCommunityJid);
    }

    /**
     * Sets the JID of the parent community this group is linked to.
     *
     * @param parentCommunityJid the parent community JID, or {@code null}
     *                           to clear
     */
    public void setParentCommunityJid(Jid parentCommunityJid) {
        this.parentCommunityJid = parentCommunityJid;
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
     * Sets whether this group operates in incognito mode.
     *
     * @param incognito {@code true} to enable incognito mode, {@code false}
     *                  to disable it
     */
    public void setIncognito(boolean incognito) {
        this.isIncognito = incognito;
    }

    /**
     * Returns whether frequently forwarded messages are blocked.
     *
     * @return {@code true} if forwarded messages are blocked
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
     * Returns the policy that determines whether admin approval is required
     * for new members to join this group.
     *
     * <p>A return value of {@link ChatPolicy#ADMINS} indicates that new
     * members must be approved by an administrator before joining. A return
     * value of {@link ChatPolicy#ANYONE} indicates that members can join
     * freely.
     *
     * @return the non-{@code null} membership approval policy
     */
    public ChatPolicy membershipApprovalMode() {
        return ChatPolicy.of(membershipApprovalMode);
    }

    /**
     * Sets the policy that determines whether admin approval is required for
     * new members.
     *
     * @param policy the non-{@code null} policy to set
     */
    public void setMembershipApprovalMode(ChatPolicy policy) {
        this.membershipApprovalMode = policy == ChatPolicy.ADMINS;
    }

    /**
     * Returns the policy that determines who can share this group's invite
     * link.
     *
     * <p>A return value of {@link ChatPolicy#ADMINS} indicates that only
     * administrators can use invite links. A return value of
     * {@link ChatPolicy#ANYONE} indicates that all members can share the
     * link.
     *
     * @return the non-{@code null} invite link policy
     */
    public ChatPolicy memberLinkMode() {
        return ChatPolicy.of(memberLinkModeAdminOnly);
    }

    /**
     * Sets the policy that determines who can share this group's invite
     * link.
     *
     * @param policy the non-{@code null} policy to set
     */
    public void setMemberLinkMode(ChatPolicy policy) {
        this.memberLinkModeAdminOnly = policy == ChatPolicy.ADMINS;
    }

    /**
     * Returns the policy that determines who can add new members to this
     * group.
     *
     * <p>A return value of {@link ChatPolicy#ADMINS} indicates that only
     * administrators can add members. A return value of
     * {@link ChatPolicy#ANYONE} indicates that all participants can add new
     * members.
     *
     * @return the non-{@code null} member addition policy
     */
    public ChatPolicy memberAddMode() {
        return ChatPolicy.of(memberAddModeAdminOnly);
    }

    /**
     * Sets the policy that determines who can add new members to this group.
     *
     * @param policy the non-{@code null} policy to set
     */
    public void setMemberAddMode(ChatPolicy policy) {
        this.memberAddModeAdminOnly = policy == ChatPolicy.ADMINS;
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
     * Sets the type of growth lock applied to this group.
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
     * Returns the server-reported participant count, if available.
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
     * Returns whether this is a support group.
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
     * Returns whether this group is suspended.
     *
     * @return {@code true} if suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets whether this group is suspended.
     *
     * @param suspended {@code true} to mark as suspended
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Returns whether this group is terminated.
     *
     * @return {@code true} if terminated
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Sets whether this group is terminated.
     *
     * @param terminated {@code true} to mark as terminated
     */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * Returns whether this group is the default subgroup (Community
     * Announcement Group) of a community.
     *
     * @return {@code true} if this group is a default subgroup
     */
    public boolean isDefaultSubgroup() {
        return defaultSubgroup;
    }

    /**
     * Sets whether this group is the default subgroup of a community.
     *
     * @param defaultSubgroup {@code true} if this is a default subgroup
     */
    public void setDefaultSubgroup(boolean defaultSubgroup) {
        this.defaultSubgroup = defaultSubgroup;
    }

    /**
     * Returns whether this group is the general chat subgroup of a
     * community.
     *
     * @return {@code true} if this is a general subgroup
     */
    public boolean isGeneralSubgroup() {
        return generalSubgroup;
    }

    /**
     * Sets whether this group is the general chat subgroup of a community.
     *
     * @param generalSubgroup {@code true} if this is a general subgroup
     */
    public void setGeneralSubgroup(boolean generalSubgroup) {
        this.generalSubgroup = generalSubgroup;
    }

    /**
     * Returns whether this group is a hidden subgroup within a community.
     *
     * @return {@code true} if this is a hidden subgroup
     */
    public boolean isHiddenSubgroup() {
        return hiddenSubgroup;
    }

    /**
     * Sets whether this group is a hidden subgroup within a community.
     *
     * @param hiddenSubgroup {@code true} if this is a hidden subgroup
     */
    public void setHiddenSubgroup(boolean hiddenSubgroup) {
        this.hiddenSubgroup = hiddenSubgroup;
    }

    /**
     * Returns whether the group safety check flag is set.
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
     * Returns the JID of the user who added the current user to this group,
     * if known.
     *
     * @return an {@code Optional} containing the adder JID, or empty if not
     *         known
     */
    public Optional<Jid> groupAdder() {
        return Optional.ofNullable(groupAdder);
    }

    /**
     * Sets the JID of the user who added the current user to this group.
     *
     * @param groupAdder the adder JID, or {@code null} to clear
     */
    public void setGroupAdder(Jid groupAdder) {
        this.groupAdder = groupAdder;
    }

    /**
     * Returns whether auto-add to general chat is disabled.
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
     * Returns the instant of the last activity, if available.
     *
     * @return an {@code Optional} containing the timestamp, or empty if not
     *         available
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
     * @return an {@code Optional} containing the timestamp, or empty if not
     *         available
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
     * Returns whether this group has CAPI capabilities.
     *
     * @return {@code true} if CAPI is available
     */
    public boolean hasCapi() {
        return hasCapi;
    }

    /**
     * Sets whether this group has CAPI capabilities.
     *
     * @param hasCapi {@code true} to enable
     */
    public void setHasCapi(boolean hasCapi) {
        this.hasCapi = hasCapi;
    }

    /**
     * Returns whether this group is a TEE bot group.
     *
     * @return {@code true} if this is a TEE bot group
     */
    public boolean isTeeBotGroup() {
        return isTeeBotGroup;
    }

    /**
     * Sets whether this group is a TEE bot group.
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
     * Returns whether the status updates posted by this group are muted for
     * the current user.
     *
     * <p>When muted, the group's status updates are hidden from the status
     * tab, but messages in the group itself remain unaffected.
     *
     * @return {@code true} if status updates from this group are muted,
     *         {@code false} otherwise
     */
    public boolean statusMuted() {
        return statusMuted;
    }

    /**
     * Sets whether the status updates posted by this group are muted for the
     * current user.
     *
     * @param statusMuted {@code true} to mute status updates,
     *                    {@code false} to unmute
     * @return this {@code GroupMetadata} instance for method chaining
     */
    public GroupMetadata setStatusMuted(boolean statusMuted) {
        this.statusMuted = statusMuted;
        return this;
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
     * Returns whether this group metadata is equal to the given object.
     *
     * <p>Two {@code GroupMetadata} instances are considered equal if they
     * have the same {@link #jid()}. Other fields are not considered in the
     * comparison.
     *
     * @param o the object to compare against
     * @return {@code true} if the given object is a {@code GroupMetadata}
     *         with the same JID, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof GroupMetadata that
                && Objects.equals(jid, that.jid);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}, derived
     * from the group's JID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(jid);
    }

    /**
     * Returns a string representation of this group metadata containing the
     * JID and subject.
     *
     * @return a human-readable string representation
     */
    @Override
    public String toString() {
        return "GroupMetadata[jid=" + jid + ", subject=" + subject + ']';
    }
}
