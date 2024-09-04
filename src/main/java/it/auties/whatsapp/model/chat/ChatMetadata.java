package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.model.jid.Jid;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This model class represents the metadata of a group or community
 */
public record ChatMetadata(
        Jid jid,
        String subject,
        Optional<Jid> subjectAuthor,
        Optional<ZonedDateTime> subjectTimestamp,
        Optional<ZonedDateTime> foundationTimestamp,
        Optional<Jid> founder,
        Optional<String> description,
        Optional<String> descriptionId,
        Map<? super ChatSetting, ChatSettingPolicy> settings,
        List<ChatParticipant> participants,
        List<ChatPastParticipant> pastParticipants,
        Optional<ZonedDateTime> ephemeralExpiration,
        Optional<Jid> parentCommunityJid,
        boolean isCommunity,
        List<CommunityLinkedGroup> communityGroups
) {

}
