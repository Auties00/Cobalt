package it.auties.whatsapp.protobuf.unknown;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.sync.HistorySyncMsg;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Conversation {
  @JsonProperty(value = "15")
  @JsonPropertyDescription("bool")
  private boolean notSpam;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("string")
  private String pHash;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("string")
  private String name;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("uint64")
  private long conversationTimestamp;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("ConversationEndOfHistoryTransferType")
  private ConversationEndOfHistoryTransferType endOfHistoryTransferType;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("int64")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("uint32")
  private int ephemeralExpiration;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bool")
  private boolean endOfHistoryTransfer;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bool")
  private boolean readOnly;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("uint32")
  private int unreadCount;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint64")
  private long lastMsgTimestamp;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String oldJid;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String newJid;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("HistorySyncMsg")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<HistorySyncMsg> messages;

  @JsonProperty(value = "1", required = true)
  @JsonPropertyDescription("string")
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
