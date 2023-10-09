package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.model.jid.Jid;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This model class represents the metadata of a group
 */
public record GroupMetadata(
        @NonNull
        Jid jid,
        @NonNull
        String subject,
        Optional<Jid> subjectAuthor,
        Optional<ZonedDateTime> subjectTimestamp,
        Optional<ZonedDateTime> foundationTimestamp,
        Optional<Jid> founder,
        Optional<String> description,
        Optional<String> descriptionId,
        @NonNull
        Map<GroupSetting, GroupSettingPolicy> policies,
        @NonNull
        List<GroupParticipant> participants,
        Optional<ZonedDateTime> ephemeralExpiration,
        boolean isCommunity,
        boolean isOpenCommunity
) {

}
