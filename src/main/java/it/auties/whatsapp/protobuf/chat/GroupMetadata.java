package it.auties.whatsapp.protobuf.chat;

import it.auties.whatsapp.protobuf.contact.ContactJid;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public record GroupMetadata(ContactJid jid, String subject, ZonedDateTime foundationTimestamp, ContactJid founder,
                            String description, String descriptionId, Map<GroupSetting, GroupPolicy> policies,
                            List<GroupParticipant> participants, ZonedDateTime ephemeralExpiration) {
}
