package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatPolicy;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model describing a batch of edits to apply to a WhatsApp group's
 * metadata. The {@link #group} JID identifies the target; every other
 * field is optional and only triggers a server- or store-side mutation
 * when it carries a value.
 *
 * <p>The same edit packet drives several distinct sites:
 * <ul>
 *   <li><strong>Direct {@code w:g2} / {@code w:profile:picture} edits</strong>
 *       ({@link #subject}, {@link #description}, {@link #picture}) are
 *       translated into a single {@code iq} stanza each and sent to the
 *       relay.</li>
 *   <li><strong>Batched {@code WASmaxGroupsSetPropertyRPC} edits</strong>
 *       ({@link #editInfoPolicy}, {@link #sendMessagePolicy},
 *       {@link #frequentlyForwardedAllowed}, {@link #adminReportsAllowed},
 *       {@link #groupHistoryShared} and
 *       {@link #membershipApprovalRequired}) are batched into a single
 *       {@code w:g2} property IQ.</li>
 *   <li><strong>MEX-dispatched edits</strong> ({@link #limitSharing},
 *       {@link #memberAddPolicy}, {@link #memberLinkPolicy},
 *       {@link #memberShareGroupHistoryPolicy} and
 *       {@link #subGroupCreationPolicy}) flow through the
 *       {@code WAWebMexUpdateGroupPropertyJob} GraphQL endpoint. The
 *       {@link #limitSharing} and {@link #subGroupCreationPolicy} edits
 *       additionally commit a WAM event after the mutation completes.</li>
 *   <li><strong>Disappearing-message edits</strong>
 *       ({@link #ephemeralTimer}, {@link #ephemeralTrigger}) are routed
 *       through the disappearing-message timer path so the in-memory
 *       chat-ephemerality state and the
 *       {@code EphemeralSettingChangeWamEvent} commit also fire.</li>
 *   <li><strong>Local-only edits</strong> ({@link #statusMuted}) are
 *       merged into the in-memory {@link GroupMetadata} row for the
 *       target group without producing a network packet. This branch
 *       backs the {@code WAWebUserStatusMuteSync.applyMutations} sync
 *       path, where the relay has already decided the value and the
 *       client is only reflecting it locally.</li>
 * </ul>
 *
 * <p>Every setting is modelled as a single nullable field whose absence
 * ({@code null}) means "leave untouched". Settings that gate an action
 * behind administrator status use {@link ChatPolicy}: {@link ChatPolicy#ADMINS}
 * restricts the action to admins, {@link ChatPolicy#ANYONE} opens it to all
 * members, and the editor emits the matching positive or negated wire toggle.
 * Plain on/off settings use a nullable {@link Boolean} so a present
 * {@code false} (the "off" toggle) is distinguishable from an absent value.
 * The disappearing-message window uses {@link ChatEphemeralTimer}, where
 * {@link ChatEphemeralTimer#OFF} disables the feature and any other constant
 * enables it for that duration. The two non-trivial scalar properties,
 * {@link #description} and {@link #picture}, use the sealed
 * {@link GroupDescription} and {@link GroupPicture} types to fold the "replace"
 * and "clear" intents into one field each. {@link #subject} stays a nullable
 * {@link String} because WA Web has no clear-subject operation for groups.
 *
 * <p>{@link #statusMuted} is declared as a nullable {@code Boolean} so
 * that a present {@code false} is distinguishable from an absent value.
 * This matches the WA Web behaviour where {@code userStatusMuteAction.muted}
 * being {@code undefined} yields {@code malformedActionValue} rather
 * than silently applying {@code muted=false}.
 */
@ProtobufMessage
public final class GroupMetadataEdit {
    /**
     * JID of the group whose metadata is being edited. Required.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid group;

    /**
     * Policy governing who may edit the group info (subject, description,
     * picture). {@link ChatPolicy#ADMINS} locks editing to admins,
     * {@link ChatPolicy#ANYONE} opens it to all members; {@code null}
     * leaves the setting untouched.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final ChatPolicy editInfoPolicy;

    /**
     * Policy governing who may send messages. {@link ChatPolicy#ADMINS}
     * switches the group into announcement mode, {@link ChatPolicy#ANYONE}
     * lets every member post; {@code null} leaves the setting untouched.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final ChatPolicy sendMessagePolicy;

    /**
     * Whether messages that have been forwarded many times are allowed.
     * A present {@code true} restores the "frequently forwarded" badge,
     * a present {@code false} disables it, and {@code null} leaves the
     * setting untouched.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final Boolean frequentlyForwardedAllowed;

    /**
     * Disappearing-message timer for the group. {@link ChatEphemeralTimer#OFF}
     * disables ephemeral messages while any other constant enables them for
     * that duration; {@code null} leaves the timer untouched.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT32)
    final ChatEphemeralTimer ephemeralTimer;

    /**
     * Trigger context identifying what mechanism initiated the
     * disappearing-message change carried by {@link #ephemeralTimer}, or
     * {@code null} when no trigger is supplied.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    final ChatDisappearingMode.Trigger ephemeralTrigger;

    /**
     * Whether new members must be approved by an admin before joining. A
     * present {@code true} requires approval, a present {@code false}
     * disables it, and {@code null} leaves the join mode untouched.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final Boolean membershipApprovalRequired;

    /**
     * Whether members can report messages to administrators. A present
     * {@code true} enables admin reports, a present {@code false} disables
     * them, and {@code null} leaves the setting untouched.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    final Boolean adminReportsAllowed;

    /**
     * Policy governing who may create sub-groups under this community
     * parent. {@link ChatPolicy#ANYONE} allows non-admins to create
     * sub-groups, {@link ChatPolicy#ADMINS} restricts creation to admins;
     * {@code null} leaves the setting untouched. Dispatched through the
     * {@code WAWebMexUpdateGroupPropertyJob} GraphQL endpoint and followed
     * by a {@code CommunityGroupJourneyEvent} WAM commit.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.ENUM)
    final ChatPolicy subGroupCreationPolicy;

    /**
     * Whether new joiners can see past messages ("group history"). A
     * present {@code true} enables shared history, a present {@code false}
     * disables it, and {@code null} leaves the setting untouched.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    final Boolean groupHistoryShared;

    /**
     * New group subject (display name). When non-{@code null}, the
     * editor emits a {@code <subject>NEW</subject>} body inside a
     * {@code w:g2} {@code iq} of type {@code set}. When {@code null},
     * the subject is left untouched.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String subject;

    /**
     * Description-edit intent. When non-{@code null}, drives a
     * {@code w:g2} {@code <description>} IQ:
     * {@link GroupDescription.Set} wraps a new body, while
     * {@link GroupDescription.Clear} requests removal of the existing
     * body matching WA Web's {@code hasDescriptionDeleteTrue:!0}
     * branch. When {@code null}, the description is left untouched.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    final GroupDescription description;

    /**
     * Picture-edit intent. When non-{@code null}, drives a
     * {@code w:profile:picture} IQ: {@link GroupPicture.Set} carries
     * the new bytes (typically a 256x256 JPEG) and emits a
     * {@code <picture type="image">BYTES</picture>} body, while
     * {@link GroupPicture.Clear} emits the no-body variant matching
     * WA Web's {@code WAWebSendProfilePictureJob(group, null)} removal
     * path. When {@code null}, the picture is left untouched.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BYTES)
    final GroupPicture picture;

    /**
     * Local-only override for the group's
     * {@link GroupMetadata#statusMuted()} flag. When non-{@code null},
     * the editor merges this value into the in-memory metadata row
     * without producing any network packet; the relay has already
     * decided the value (the sync action carrying this edit is itself
     * server-driven). When {@code null}, the flag is left untouched.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    final Boolean statusMuted;

    /**
     * Whether the per-chat "limit sharing" anti-forward feature is
     * enabled. A present {@code true} enables it, a present {@code false}
     * disables it, and {@code null} leaves the setting untouched.
     * Dispatched through {@code WAWebMexUpdateGroupPropertyJob} and
     * followed by a {@code LimitSharingSettingUpdateWamEvent} commit.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    final Boolean limitSharing;

    /**
     * Policy governing who may add new members. {@link ChatPolicy#ADMINS}
     * restricts adding to admins ({@code member_add_mode="ADMIN_ADD"}),
     * {@link ChatPolicy#ANYONE} opens it to all members
     * ({@code "ALL_MEMBER_ADD"}); {@code null} leaves the setting
     * untouched. Dispatched through {@code WAWebMexUpdateGroupPropertyJob}.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.ENUM)
    final ChatPolicy memberAddPolicy;

    /**
     * Policy governing who may share the invite link. {@link ChatPolicy#ADMINS}
     * restricts sharing to admins ({@code member_link_mode="ADMIN_LINK"}),
     * {@link ChatPolicy#ANYONE} opens it to all members
     * ({@code "ALL_MEMBER_LINK"}); {@code null} leaves the setting
     * untouched. Dispatched through {@code WAWebMexUpdateGroupPropertyJob}.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.ENUM)
    final ChatPolicy memberLinkPolicy;

    /**
     * Policy governing who may share the message history with newly added
     * members. {@link ChatPolicy#ADMINS} restricts sharing to admins
     * ({@code member_share_group_history_mode="ADMIN_SHARE"}),
     * {@link ChatPolicy#ANYONE} opens it to all members
     * ({@code "ALL_MEMBER_SHARE"}); {@code null} leaves the setting
     * untouched. Dispatched through {@code WAWebMexUpdateGroupPropertyJob}.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.ENUM)
    final ChatPolicy memberShareGroupHistoryPolicy;

    /**
     * Constructs a new {@code GroupMetadataEdit}.
     *
     * @param group                         the group JID; required
     * @param editInfoPolicy                policy for editing group info, or {@code null}
     * @param sendMessagePolicy             policy for sending messages, or {@code null}
     * @param frequentlyForwardedAllowed    whether frequently-forwarded messages are allowed, or {@code null}
     * @param ephemeralTimer                disappearing-message timer, or {@code null}
     * @param ephemeralTrigger              disappearing-message change trigger, or {@code null}
     * @param membershipApprovalRequired    whether membership approval is required, or {@code null}
     * @param adminReportsAllowed           whether admin reports are allowed, or {@code null}
     * @param subGroupCreationPolicy        policy for sub-group creation, or {@code null}
     * @param groupHistoryShared            whether group history is shared, or {@code null}
     * @param subject                       new group subject, or {@code null} to leave unchanged
     * @param description                   description-edit intent, or {@code null} to leave unchanged
     * @param picture                       picture-edit intent, or {@code null} to leave unchanged
     * @param statusMuted                   new local statusMuted flag, or {@code null} to leave unchanged
     * @param limitSharing                  whether the per-chat sharing limit is enabled, or {@code null}
     * @param memberAddPolicy               policy for adding members, or {@code null}
     * @param memberLinkPolicy              policy for sharing the invite link, or {@code null}
     * @param memberShareGroupHistoryPolicy policy for sharing history with new members, or {@code null}
     * @throws NullPointerException if {@code group} is {@code null}
     */
    GroupMetadataEdit(Jid group, ChatPolicy editInfoPolicy, ChatPolicy sendMessagePolicy,
                      Boolean frequentlyForwardedAllowed, ChatEphemeralTimer ephemeralTimer,
                      ChatDisappearingMode.Trigger ephemeralTrigger, Boolean membershipApprovalRequired,
                      Boolean adminReportsAllowed, ChatPolicy subGroupCreationPolicy,
                      Boolean groupHistoryShared, String subject, GroupDescription description,
                      GroupPicture picture, Boolean statusMuted, Boolean limitSharing,
                      ChatPolicy memberAddPolicy, ChatPolicy memberLinkPolicy,
                      ChatPolicy memberShareGroupHistoryPolicy) {
        this.group = Objects.requireNonNull(group, "group cannot be null");
        this.editInfoPolicy = editInfoPolicy;
        this.sendMessagePolicy = sendMessagePolicy;
        this.frequentlyForwardedAllowed = frequentlyForwardedAllowed;
        this.ephemeralTimer = ephemeralTimer;
        this.ephemeralTrigger = ephemeralTrigger;
        this.membershipApprovalRequired = membershipApprovalRequired;
        this.adminReportsAllowed = adminReportsAllowed;
        this.subGroupCreationPolicy = subGroupCreationPolicy;
        this.groupHistoryShared = groupHistoryShared;
        this.subject = subject;
        this.description = description;
        this.picture = picture;
        this.statusMuted = statusMuted;
        this.limitSharing = limitSharing;
        this.memberAddPolicy = memberAddPolicy;
        this.memberLinkPolicy = memberLinkPolicy;
        this.memberShareGroupHistoryPolicy = memberShareGroupHistoryPolicy;
    }

    /**
     * Returns the group JID.
     *
     * @return the JID, never {@code null}
     */
    public Jid group() {
        return group;
    }

    /**
     * Returns the optional group-info edit policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> editInfoPolicy() {
        return Optional.ofNullable(editInfoPolicy);
    }

    /**
     * Returns the optional send-message policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> sendMessagePolicy() {
        return Optional.ofNullable(sendMessagePolicy);
    }

    /**
     * Returns the optional frequently-forwarded-allowed flag.
     *
     * @return an {@link Optional} carrying {@code true} to allow
     *         frequently-forwarded messages, {@code false} to block them,
     *         or empty when unset
     */
    public Optional<Boolean> frequentlyForwardedAllowed() {
        return Optional.ofNullable(frequentlyForwardedAllowed);
    }

    /**
     * Returns the optional disappearing-message timer.
     *
     * @return an {@link Optional} carrying the timer, or empty when unset
     */
    public Optional<ChatEphemeralTimer> ephemeralTimer() {
        return Optional.ofNullable(ephemeralTimer);
    }

    /**
     * Returns the optional disappearing-message change trigger.
     *
     * @return an {@link Optional} carrying the trigger, or empty when unset
     */
    public Optional<ChatDisappearingMode.Trigger> ephemeralTrigger() {
        return Optional.ofNullable(ephemeralTrigger);
    }

    /**
     * Returns the optional membership-approval-required flag.
     *
     * @return an {@link Optional} carrying {@code true} to require approval,
     *         {@code false} to disable it, or empty when unset
     */
    public Optional<Boolean> membershipApprovalRequired() {
        return Optional.ofNullable(membershipApprovalRequired);
    }

    /**
     * Returns the optional admin-reports-allowed flag.
     *
     * @return an {@link Optional} carrying {@code true} to allow admin
     *         reports, {@code false} to disable them, or empty when unset
     */
    public Optional<Boolean> adminReportsAllowed() {
        return Optional.ofNullable(adminReportsAllowed);
    }

    /**
     * Returns the optional sub-group-creation policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> subGroupCreationPolicy() {
        return Optional.ofNullable(subGroupCreationPolicy);
    }

    /**
     * Returns the optional group-history-shared flag.
     *
     * @return an {@link Optional} carrying {@code true} to share history,
     *         {@code false} to disable it, or empty when unset
     */
    public Optional<Boolean> groupHistoryShared() {
        return Optional.ofNullable(groupHistoryShared);
    }

    /**
     * Returns the optional new subject. When present, the editor must
     * dispatch a {@code w:g2} {@code <subject>} IQ.
     *
     * @return an {@link Optional} carrying the new subject, or empty
     *         when the subject should be left unchanged
     */
    public Optional<String> subject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Returns the optional description-edit intent. When present, the
     * editor must dispatch a {@code w:g2} {@code <description>} IQ
     * keyed on the underlying variant.
     *
     * @return an {@link Optional} carrying the
     *         {@link GroupDescription} intent, or empty when the
     *         description should be left unchanged
     */
    public Optional<GroupDescription> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the optional picture-edit intent. When present, the
     * editor must dispatch a {@code w:profile:picture} IQ keyed on the
     * underlying variant.
     *
     * @return an {@link Optional} carrying the {@link GroupPicture}
     *         intent, or empty when the picture should be left
     *         unchanged
     */
    public Optional<GroupPicture> picture() {
        return Optional.ofNullable(picture);
    }

    /**
     * Returns the optional new status-muted flag. When present, the
     * editor must merge this value into the in-memory
     * {@link GroupMetadata#statusMuted()} field for the target group;
     * no network packet is produced.
     *
     * @return an {@link Optional} carrying the new flag, or empty when
     *         the flag should be left unchanged
     */
    public Optional<Boolean> statusMuted() {
        return Optional.ofNullable(statusMuted);
    }

    /**
     * Returns the optional limit-sharing flag.
     *
     * @return an {@link Optional} carrying {@code true} to enable the
     *         per-chat sharing limit, {@code false} to disable it, or
     *         empty when unset
     */
    public Optional<Boolean> limitSharing() {
        return Optional.ofNullable(limitSharing);
    }

    /**
     * Returns the optional member-add policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> memberAddPolicy() {
        return Optional.ofNullable(memberAddPolicy);
    }

    /**
     * Returns the optional member-link policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> memberLinkPolicy() {
        return Optional.ofNullable(memberLinkPolicy);
    }

    /**
     * Returns the optional member-share-group-history policy.
     *
     * @return an {@link Optional} carrying the policy, or empty when unset
     */
    public Optional<ChatPolicy> memberShareGroupHistoryPolicy() {
        return Optional.ofNullable(memberShareGroupHistoryPolicy);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GroupMetadataEdit) obj;
        return Objects.equals(group, that.group) &&
                editInfoPolicy == that.editInfoPolicy &&
                sendMessagePolicy == that.sendMessagePolicy &&
                Objects.equals(frequentlyForwardedAllowed, that.frequentlyForwardedAllowed) &&
                ephemeralTimer == that.ephemeralTimer &&
                ephemeralTrigger == that.ephemeralTrigger &&
                Objects.equals(membershipApprovalRequired, that.membershipApprovalRequired) &&
                Objects.equals(adminReportsAllowed, that.adminReportsAllowed) &&
                subGroupCreationPolicy == that.subGroupCreationPolicy &&
                Objects.equals(groupHistoryShared, that.groupHistoryShared) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(description, that.description) &&
                Objects.equals(picture, that.picture) &&
                Objects.equals(statusMuted, that.statusMuted) &&
                Objects.equals(limitSharing, that.limitSharing) &&
                memberAddPolicy == that.memberAddPolicy &&
                memberLinkPolicy == that.memberLinkPolicy &&
                memberShareGroupHistoryPolicy == that.memberShareGroupHistoryPolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, editInfoPolicy, sendMessagePolicy, frequentlyForwardedAllowed,
                ephemeralTimer, ephemeralTrigger, membershipApprovalRequired, adminReportsAllowed,
                subGroupCreationPolicy, groupHistoryShared, subject, description, picture, statusMuted,
                limitSharing, memberAddPolicy, memberLinkPolicy, memberShareGroupHistoryPolicy);
    }

    @Override
    public String toString() {
        return "GroupMetadataEdit[" +
                "group=" + group + ", " +
                "editInfoPolicy=" + editInfoPolicy + ", " +
                "sendMessagePolicy=" + sendMessagePolicy + ", " +
                "frequentlyForwardedAllowed=" + frequentlyForwardedAllowed + ", " +
                "ephemeralTimer=" + ephemeralTimer + ", " +
                "ephemeralTrigger=" + ephemeralTrigger + ", " +
                "membershipApprovalRequired=" + membershipApprovalRequired + ", " +
                "adminReportsAllowed=" + adminReportsAllowed + ", " +
                "subGroupCreationPolicy=" + subGroupCreationPolicy + ", " +
                "groupHistoryShared=" + groupHistoryShared + ", " +
                "subject=" + subject + ", " +
                "description=" + description + ", " +
                "picture=" + picture + ", " +
                "statusMuted=" + statusMuted + ", " +
                "limitSharing=" + limitSharing + ", " +
                "memberAddPolicy=" + memberAddPolicy + ", " +
                "memberLinkPolicy=" + memberLinkPolicy + ", " +
                "memberShareGroupHistoryPolicy=" + memberShareGroupHistoryPolicy + ']';
    }
}
