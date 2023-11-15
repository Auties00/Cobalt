package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.model.jid.Jid;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This model class represents the metadata of a group
 */
public record GroupMetadata(
        Jid jid,
        String subject,
        Optional<Jid> subjectAuthor,
        Optional<ZonedDateTime> subjectTimestamp,
        Optional<ZonedDateTime> foundationTimestamp,
        Optional<Jid> founder,
        Optional<String> description,
        Optional<String> descriptionId,
        Map<GroupSetting, ChatSettingPolicy> policies,
        List<GroupParticipant> participants,
        Optional<ZonedDateTime> ephemeralExpiration,
        boolean isCommunity,
        boolean isOpenCommunity
) {

}
