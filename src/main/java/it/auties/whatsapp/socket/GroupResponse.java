package it.auties.whatsapp.socket;

import it.auties.whatsapp.protobuf.chat.GroupParticipant;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
public class GroupResponse extends Response {
    private String id;
    private String subject;
    private ZonedDateTime subjectTimeStamp;
    private ContactJid founder;
    private ZonedDateTime foundationTimestamp;
    private List<GroupParticipant> participants;

    public GroupResponse(@NonNull Node source) {
        super(source);
        this.id = source.attributes().getString("id");
        this.subject = source.attributes().getString("subject");
        this.subjectTimeStamp = WhatsappUtils.parseWhatsappTime(source.attributes().getLong("s_t"))
                .orElseThrow(() -> new NoSuchElementException("Missing timestamp in group response"));
        this.founder = source.attributes().getJid("creator")
                .orElseThrow(() -> new NoSuchElementException("Missing founder in group response"));
        this.foundationTimestamp = WhatsappUtils.parseWhatsappTime(source.attributes().getLong("creation"))
                .orElseThrow(() -> new NoSuchElementException("Missing timestamp in group response"));
        this.participants = source.findNodes("participant")
                .stream()
                .map(GroupParticipant::of)
                .toList();
    }

}
