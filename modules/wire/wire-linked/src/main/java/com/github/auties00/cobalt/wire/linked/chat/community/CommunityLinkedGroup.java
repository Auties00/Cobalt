package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a subgroup that is linked to a WhatsApp community.
 *
 * <p>Every WhatsApp community organizes conversations into subgroups.
 * Each subgroup has its own JID, display name, description, and participant
 * count. This class captures the metadata of a single linked subgroup as
 * reported by the server, including its relationship to the parent community
 * and special roles such as being the default announcement subgroup or the
 * general chat subgroup.
 *
 * <p>Communities always contain at least two special subgroups:
 * <ul>
 *   <li>The <b>default announcement subgroup</b>, where only community
 *       administrators can post messages. Identified by
 *       {@link #isDefaultSubgroup()} returning {@code true}.</li>
 *   <li>The <b>general chat subgroup</b>, which acts as the main open
 *       discussion space. Identified by {@link #isGeneralSubgroup()}
 *       returning {@code true}.</li>
 * </ul>
 * Additional subgroups may be created by administrators (or by non-admin
 * members if the community permits it).
 *
 * <p>Instances of this class are mutable. All fields can be changed after
 * construction through the setter methods.
 *
 * @see CommunityMetadata#communityGroups()
 */
@ProtobufMessage(name = "CommunityLinkedGroup")
public final class CommunityLinkedGroup {
    /**
     * The JID that uniquely identifies this linked subgroup within WhatsApp.
     * Every subgroup has its own distinct JID, separate from the parent
     * community JID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid jid;

    /**
     * The number of participants currently in this linked subgroup, or
     * {@code null} if the server did not include participant count
     * information. This value is a server-reported approximation and may
     * not reflect real-time membership changes.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
    Integer participantCount;

    /**
     * The display name (subject) of this linked subgroup, or {@code null}
     * if the subject is not available. The subject is set by the subgroup
     * or community administrator and is visible to all community members.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String subject;

    /**
     * The instant at which the subject was last changed, or {@code null}
     * if the timestamp is not available.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant subjectTimestamp;

    /**
     * The JID of the parent community this subgroup belongs to, or
     * {@code null} if not available. This provides a back-reference from
     * the subgroup to its owning community.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    Jid parentGroupJid;

    /**
     * Whether this subgroup is the default announcement subgroup of the
     * parent community. The default announcement subgroup is a special
     * subgroup where only community administrators can send messages. Every
     * community has exactly one default announcement subgroup.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    boolean defaultSubgroup;

    /**
     * Whether this subgroup is the general chat subgroup of the parent
     * community. The general chat subgroup is the main open discussion
     * space where all community members can participate. Every community
     * has exactly one general chat subgroup.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean generalSubgroup;

    /**
     * The description text of this linked subgroup, or {@code null} if no
     * description has been set. The description is typically a short
     * paragraph explaining the purpose or topic of the subgroup.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String description;

    /**
     * The instant at which this linked subgroup was created, or
     * {@code null} if the creation timestamp is not available.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant creationTimestamp;

    /**
     * The JID of the user who originally created this subgroup, or
     * {@code null} if the creator is not known.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    Jid ownerJid;

    /**
     * Whether admin approval is required for new members to join this
     * subgroup. When {@code true}, join requests must be approved by a
     * subgroup administrator before the requesting user becomes a member.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    boolean membershipApprovalMode;

    /**
     * Whether this subgroup is hidden from the community's public subgroup
     * list. Hidden subgroups are not visible to community members browsing
     * the list of available subgroups.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    boolean hiddenSubgroup;

    /**
     * Whether this subgroup has been suspended by WhatsApp. A suspended
     * subgroup cannot receive or send messages until it is restored. This
     * typically happens as a result of policy enforcement actions.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    boolean suspended;

    /**
     * Constructs a new {@code CommunityLinkedGroup} with the specified values.
     *
     * @param jid                    the JID of the linked subgroup
     * @param participantCount       the number of participants, or
     *                               {@code null} if not available
     * @param subject                the display name (subject), or
     *                               {@code null} if not available
     * @param subjectTimestamp       the instant at which the subject was
     *                               last changed, or {@code null}
     * @param parentGroupJid         the JID of the parent community, or
     *                               {@code null}
     * @param defaultSubgroup        whether this is the default
     *                               announcement subgroup
     * @param generalSubgroup        whether this is the general chat
     *                               subgroup
     * @param description            the description text, or {@code null}
     * @param creationTimestamp      the instant at which this subgroup was
     *                               created, or {@code null}
     * @param ownerJid               the JID of the subgroup creator, or
     *                               {@code null}
     * @param membershipApprovalMode whether admin approval is required to
     *                               join
     * @param hiddenSubgroup         whether this subgroup is hidden from
     *                               the community subgroup list
     * @param suspended              whether this subgroup is suspended
     */
    CommunityLinkedGroup(
            Jid jid,
            Integer participantCount,
            String subject,
            Instant subjectTimestamp,
            Jid parentGroupJid,
            boolean defaultSubgroup,
            boolean generalSubgroup,
            String description,
            Instant creationTimestamp,
            Jid ownerJid,
            boolean membershipApprovalMode,
            boolean hiddenSubgroup,
            boolean suspended
    ) {
        this.jid = jid;
        this.participantCount = participantCount;
        this.subject = subject;
        this.subjectTimestamp = subjectTimestamp;
        this.parentGroupJid = parentGroupJid;
        this.defaultSubgroup = defaultSubgroup;
        this.generalSubgroup = generalSubgroup;
        this.description = description;
        this.creationTimestamp = creationTimestamp;
        this.ownerJid = ownerJid;
        this.membershipApprovalMode = membershipApprovalMode;
        this.hiddenSubgroup = hiddenSubgroup;
        this.suspended = suspended;
    }

    /**
     * Returns the JID that uniquely identifies this linked subgroup.
     *
     * @return the subgroup JID, or {@code null} if not set
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Sets the JID that identifies this linked subgroup.
     *
     * @param jid the subgroup JID
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * Returns the number of participants currently in this linked subgroup,
     * if available. The count may be absent when the server did not include
     * participant count information in the metadata response.
     *
     * @return an {@code OptionalInt} containing the participant count, or
     *         empty if not available
     */
    public OptionalInt participantCount() {
        return participantCount == null
                ? OptionalInt.empty()
                : OptionalInt.of(participantCount);
    }

    /**
     * Sets the number of participants in this linked subgroup.
     *
     * @param participantCount the participant count, or {@code null} to
     *                         clear the value
     */
    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }

    /**
     * Returns the display name (subject) of this linked subgroup, if
     * available.
     *
     * @return an {@code Optional} containing the subject, or empty if not
     *         available
     */
    public Optional<String> subject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Sets the display name (subject) of this linked subgroup.
     *
     * @param subject the subject text, or {@code null} to clear
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns the instant at which the subject was last changed, if known.
     *
     * @return an {@code Optional} containing the subject timestamp, or
     *         empty if not available
     */
    public Optional<Instant> subjectTimestamp() {
        return Optional.ofNullable(subjectTimestamp);
    }

    /**
     * Sets the instant at which the subject was last changed.
     *
     * @param subjectTimestamp the subject change timestamp, or {@code null}
     *                        to clear
     */
    public void setSubjectTimestamp(Instant subjectTimestamp) {
        this.subjectTimestamp = subjectTimestamp;
    }

    /**
     * Returns the JID of the parent community this subgroup belongs to,
     * if available. This provides a back-reference from the subgroup to
     * its owning {@link CommunityMetadata}.
     *
     * @return an {@code Optional} containing the parent community JID, or
     *         empty if not available
     */
    public Optional<Jid> parentGroupJid() {
        return Optional.ofNullable(parentGroupJid);
    }

    /**
     * Sets the JID of the parent community this subgroup belongs to.
     *
     * @param parentGroupJid the parent community JID, or {@code null} to
     *                       clear
     */
    public void setParentGroupJid(Jid parentGroupJid) {
        this.parentGroupJid = parentGroupJid;
    }

    /**
     * Returns whether this subgroup is the default announcement subgroup
     * of the parent community. The default announcement subgroup is a
     * read-only group where only administrators can post messages.
     *
     * @return {@code true} if this is the default announcement subgroup
     */
    public boolean isDefaultSubgroup() {
        return defaultSubgroup;
    }

    /**
     * Sets whether this subgroup is the default announcement subgroup.
     *
     * @param defaultSubgroup {@code true} to mark as the default
     *                        announcement subgroup
     */
    public void setDefaultSubgroup(boolean defaultSubgroup) {
        this.defaultSubgroup = defaultSubgroup;
    }

    /**
     * Returns whether this subgroup is the general chat subgroup of the
     * parent community. The general chat subgroup is the main open
     * discussion space where all community members can participate.
     *
     * @return {@code true} if this is the general chat subgroup
     */
    public boolean isGeneralSubgroup() {
        return generalSubgroup;
    }

    /**
     * Sets whether this subgroup is the general chat subgroup.
     *
     * @param generalSubgroup {@code true} to mark as general subgroup
     */
    public void setGeneralSubgroup(boolean generalSubgroup) {
        this.generalSubgroup = generalSubgroup;
    }

    /**
     * Returns the description text of this linked subgroup, if one has been
     * set.
     *
     * @return an {@code Optional} containing the description, or empty if
     *         not set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets the description text of this linked subgroup.
     *
     * @param description the description text, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the instant at which this subgroup was created, if known.
     *
     * @return an {@code Optional} containing the creation timestamp, or
     *         empty if not available
     */
    public Optional<Instant> creationTimestamp() {
        return Optional.ofNullable(creationTimestamp);
    }

    /**
     * Sets the instant at which this subgroup was created.
     *
     * @param creationTimestamp the creation timestamp, or {@code null} to
     *                         clear
     */
    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    /**
     * Returns the JID of the user who originally created this subgroup, if
     * known.
     *
     * @return an {@code Optional} containing the owner JID, or empty if
     *         not known
     */
    public Optional<Jid> ownerJid() {
        return Optional.ofNullable(ownerJid);
    }

    /**
     * Sets the JID of the user who originally created this subgroup.
     *
     * @param ownerJid the creator JID, or {@code null} to clear
     */
    public void setOwnerJid(Jid ownerJid) {
        this.ownerJid = ownerJid;
    }

    /**
     * Returns whether admin approval is required for new members to join
     * this subgroup.
     *
     * @return {@code true} if membership approval mode is enabled
     */
    public boolean isMembershipApprovalMode() {
        return membershipApprovalMode;
    }

    /**
     * Sets whether admin approval is required for new members to join
     * this subgroup.
     *
     * @param membershipApprovalMode {@code true} to enable membership
     *                               approval mode
     */
    public void setMembershipApprovalMode(boolean membershipApprovalMode) {
        this.membershipApprovalMode = membershipApprovalMode;
    }

    /**
     * Returns whether this subgroup is hidden from the community's public
     * subgroup list. Hidden subgroups are not visible to community members
     * browsing the list of available subgroups.
     *
     * @return {@code true} if this subgroup is hidden
     */
    public boolean isHiddenSubgroup() {
        return hiddenSubgroup;
    }

    /**
     * Sets whether this subgroup is hidden from the community's public
     * subgroup list.
     *
     * @param hiddenSubgroup {@code true} to mark as hidden
     */
    public void setHiddenSubgroup(boolean hiddenSubgroup) {
        this.hiddenSubgroup = hiddenSubgroup;
    }

    /**
     * Returns whether this subgroup has been suspended by WhatsApp. A
     * suspended subgroup cannot receive or send messages until restored.
     *
     * @return {@code true} if this subgroup is suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets whether this subgroup has been suspended.
     *
     * @param suspended {@code true} to mark as suspended
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
