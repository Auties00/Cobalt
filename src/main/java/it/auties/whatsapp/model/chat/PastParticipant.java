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

/**
 * Class representing a past participant in a chat.
 *
 * @author [Author Name]
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PastParticipant")
public class PastParticipant implements ProtobufMessage {

  /**
   * The user JID of the past participant.
   */
  @ProtobufProperty(index = 1, name = "userJid", type = STRING)
  private ContactJid userJid;

  /**
   * The reason for the past participant leaving the chat.
   */
  @ProtobufProperty(index = 2, name = "leaveReason", type = MESSAGE)
  private LeaveReason leaveReason;

  /**
   * The timestamp of when the past participant left the chat.
   */
  @ProtobufProperty(index = 3, name = "leaveTs", type = UINT64)
  private long leaveTimestamp;

  /**
   * Enum representing the reason for a past participant leaving the chat.
   */
  @AllArgsConstructor
  public enum LeaveReason implements ProtobufMessage {
    /**
     * The past participant left the chat voluntarily.
     */
    LEFT(0),
    /**
     * The past participant was removed from the chat.
     */
    REMOVED(1);

    /**
     * Getter for the index of the leave reason.
     */
    @Getter
    private final int index;
  }
}