package it.auties.whatsapp.model.chat;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * This model class represents the metadata of a group or community
 */
@ProtobufMessage
public final class GroupOrCommunityMetadata {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String subject;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid subjectAuthorJid;

    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    final long subjectTimestampSeconds;

    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final long foundationTimestampSeconds;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final Jid founderJid;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String description;

    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String descriptionId;

    @ProtobufProperty(index = 9, type = ProtobufType.MAP, mapKeyType = ProtobufType.UINT32, mapValueType = ProtobufType.ENUM)
    final Map<Integer, ChatSettingPolicy> settings;

    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    final SequencedSet<ChatParticipant> participants;

    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    final long ephemeralExpirationSeconds;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    final Jid parentCommunityJid;

    @ProtobufProperty(index = 14, type = ProtobufType.BOOL)
    final boolean isCommunity;

    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    final SequencedSet<CommunityLinkedGroup> communityGroups;

    GroupOrCommunityMetadata(Jid jid, String subject, Jid subjectAuthorJid, long subjectTimestampSeconds, long foundationTimestampSeconds, Jid founderJid, String description, String descriptionId, Map<Integer, ChatSettingPolicy> settings, SequencedSet<ChatParticipant> participants, long ephemeralExpirationSeconds, Jid parentCommunityJid, boolean isCommunity, SequencedSet<CommunityLinkedGroup> communityGroups) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.subject = Objects.requireNonNull(subject, "subject cannot be null");
        this.subjectAuthorJid = subjectAuthorJid;
        this.subjectTimestampSeconds = subjectTimestampSeconds;
        this.foundationTimestampSeconds = foundationTimestampSeconds;
        this.founderJid = founderJid;
        this.description = description;
        this.descriptionId = descriptionId;
        this.settings = Objects.requireNonNullElseGet(settings, HashMap::new);
        this.participants = Objects.requireNonNullElseGet(participants, LinkedHashSet::new);
        this.ephemeralExpirationSeconds = ephemeralExpirationSeconds;
        this.parentCommunityJid = parentCommunityJid;
        this.isCommunity = isCommunity;
        this.communityGroups = Objects.requireNonNullElseGet(communityGroups, LinkedHashSet::new);
    }

    public Jid jid() {
        return jid;
    }

    public String subject() {
        return subject;
    }

    public Optional<Jid> subjectAuthorJid() {
        return Optional.ofNullable(subjectAuthorJid);
    }

    public long subjectTimestampSeconds() {
        return subjectTimestampSeconds;
    }

    public Optional<ZonedDateTime> subjectTimestamp() {
        return Clock.parseSeconds(subjectTimestampSeconds);
    }

    public long foundationTimestampSeconds() {
        return foundationTimestampSeconds;
    }

    public Optional<ZonedDateTime> foundationTimestamp() {
        return Clock.parseSeconds(foundationTimestampSeconds);
    }

    public Optional<Jid> founder() {
        return Optional.ofNullable(founderJid);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public Optional<String> descriptionId() {
        return Optional.ofNullable(descriptionId);
    }

    public Optional<ChatSettingPolicy> getPolicy(ChatSetting setting) {
        return Optional.ofNullable(settings.get(setting.index()));
    }

    public Set<ChatParticipant> participants() {
        return Collections.unmodifiableSet(participants);
    }

    public void addParticipant(ChatParticipant participant) {
        participants.add(participant);
    }

    public boolean removeParticipant(ChatParticipant participant) {
        return participants.remove(participant);
    }

    public boolean removeParticipant(Jid jid) {
        return participants.removeIf(participant -> participant.jid().equals(jid));
    }

    public long ephemeralExpirationSeconds() {
        return ephemeralExpirationSeconds;
    }

    public Optional<ZonedDateTime> ephemeralExpiration() {
        return Clock.parseSeconds(ephemeralExpirationSeconds);
    }

    public Optional<Jid> parentCommunityJid() {
        return Optional.ofNullable(parentCommunityJid);
    }

    public boolean isCommunity() {
        return isCommunity;
    }

    public SequencedSet<CommunityLinkedGroup> communityGroups() {
        return Collections.unmodifiableSequencedSet(communityGroups);
    }

    public void addCommunityGroup(CommunityLinkedGroup group) {
        communityGroups.add(group);
    }

    public boolean removeCommunityGroup(CommunityLinkedGroup group) {
        return communityGroups.remove(group);
    }

    public boolean removeCommunityGroup(Jid jid) {
        return communityGroups.removeIf(group -> group.jid().equals(jid));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupOrCommunityMetadata that
                && Objects.equals(jid, that.jid)
                && Objects.equals(subject, that.subject)
                && Objects.equals(subjectAuthorJid, that.subjectAuthorJid)
                && Objects.equals(subjectTimestampSeconds, that.subjectTimestampSeconds)
                && Objects.equals(foundationTimestampSeconds, that.foundationTimestampSeconds)
                && Objects.equals(founderJid, that.founderJid)
                && Objects.equals(description, that.description)
                && Objects.equals(descriptionId, that.descriptionId)
                && Objects.equals(settings, that.settings)
                && Objects.equals(participants, that.participants)
                && Objects.equals(ephemeralExpirationSeconds, that.ephemeralExpirationSeconds)
                && Objects.equals(parentCommunityJid, that.parentCommunityJid)
                && isCommunity == that.isCommunity
                && Objects.equals(communityGroups, that.communityGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid, subject, subjectAuthorJid, subjectTimestampSeconds, foundationTimestampSeconds,
                founderJid, description, descriptionId, settings, participants,
                ephemeralExpirationSeconds, parentCommunityJid, isCommunity, communityGroups);
    }

    @Override
    public String toString() {
        return "ChatMetadata[" +
                "jid=" + jid + ", " +
                "subject=" + subject + ", " +
                "subjectAuthor=" + subjectAuthorJid + ", " +
                "subjectTimestamp=" + subjectTimestampSeconds + ", " +
                "foundationTimestamp=" + foundationTimestampSeconds + ", " +
                "founder=" + founderJid + ", " +
                "description=" + description + ", " +
                "descriptionId=" + descriptionId + ", " +
                "settings=" + settings + ", " +
                "participants=" + participants + ", " +
                "ephemeralExpiration=" + ephemeralExpirationSeconds + ", " +
                "parentCommunityJid=" + parentCommunityJid + ", " +
                "isCommunity=" + isCommunity + ", " +
                "communityGroups=" + communityGroups + ']';
    }
}