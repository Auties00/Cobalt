package it.auties.whatsapp.model.chat;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PastParticipant")
public class PastParticipant
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, name = "userJid", type = STRING)
  private ContactJid userJid;

  @ProtobufProperty(index = 2, name = "leaveReason", type = MESSAGE)
  private LeaveReason leaveReason;

  @ProtobufProperty(index = 3, name = "leaveTs", type = UINT64)
  private long leaveTimestamp;

  @AllArgsConstructor
  public enum LeaveReason
      implements ProtobufMessage {
    LEFT(0),
    REMOVED(1);

    @Getter
    private final int index;
  }
}