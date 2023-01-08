package it.auties.whatsapp.model.chat;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.contact.ContactJid;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PastParticipants")
public class PastParticipants
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, name = "groupJid", type = ProtobufType.STRING)
  private ContactJid groupJid;

  @ProtobufProperty(implementation = PastParticipant.class, index = 2, name = "pastParticipants", repeated = true, type = ProtobufType.MESSAGE)
  private List<PastParticipant> pastParticipants;

  public static class PastParticipantsBuilder {

    public PastParticipantsBuilder pastParticipants(List<PastParticipant> pastParticipants) {
      if (this.pastParticipants == null) {
        this.pastParticipants = new ArrayList<>();
      }
      this.pastParticipants.addAll(pastParticipants);
      return this;
    }
  }
}