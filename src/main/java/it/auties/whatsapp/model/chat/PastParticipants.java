package it.auties.whatsapp.model.chat;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * Class representing a list of past participants in a chat group
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PastParticipants")
public class PastParticipants implements ProtobufMessage {

  /**
   * The JID of the chat group.
   */
  @ProtobufProperty(index = 1, name = "groupJid", type = STRING)
  private ContactJid groupJid;

  /**
   * The list of past participants in the chat group.
   */
  @ProtobufProperty(implementation = PastParticipant.class, index = 2, name = "pastParticipants", repeated = true, type = MESSAGE)
  @Default
  private List<PastParticipant> pastParticipants = new ArrayList<>();

  public static class PastParticipantsBuilder {
    public PastParticipantsBuilder pastParticipants(List<PastParticipant> pastParticipants) {
      if (!this.pastParticipants$set) {
        this.pastParticipants$set = true;
        this.pastParticipants$value = new ArrayList<>();
      }

      this.pastParticipants$value.addAll(pastParticipants);
      return this;
    }
  }
}