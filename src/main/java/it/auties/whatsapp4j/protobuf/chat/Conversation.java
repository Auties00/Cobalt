package it.auties.whatsapp4j.protobuf.chat;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.model.history.HistorySyncMsg;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Conversation {
  @JsonProperty(value = "15")
  private boolean notSpam;

  @JsonProperty(value = "14")
  private String pHash;

  @JsonProperty(value = "13")
  private String name;

  @JsonProperty(value = "12")
  private long conversationTimestamp;

  @JsonProperty(value = "11")
  private ConversationEndOfHistoryTransferType endOfHistoryTransferType;

  @JsonProperty(value = "10")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "9")
  private int ephemeralExpiration;

  @JsonProperty(value = "8")
  private boolean endOfHistoryTransfer;

  @JsonProperty(value = "7")
  private boolean readOnly;

  @JsonProperty(value = "6")
  private int unreadCount;

  @JsonProperty(value = "5")
  private long lastMsgTimestamp;

  @JsonProperty(value = "4")
  private String oldJid;

  @JsonProperty(value = "3")
  private String newJid;

  @JsonProperty(value = "2")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HistorySyncMsg> messages;

  @JsonProperty(value = "1", required = true)
  private String id;

  @Accessors(fluent = true)
  public enum ConversationEndOfHistoryTransferType {
    COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY(0),
    COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY(1);

    private final @Getter int index;

    ConversationEndOfHistoryTransferType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ConversationEndOfHistoryTransferType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
