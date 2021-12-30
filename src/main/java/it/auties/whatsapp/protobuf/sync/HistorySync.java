package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HistorySync {
  @JsonProperty(value = "1", required = true)
  @JsonPropertyDescription("HistorySyncHistorySyncType")
  private HistorySyncHistorySyncType syncType;

  @JsonProperty("2")
  @JsonPropertyDescription("Conversation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Chat> conversations;

  @JsonProperty("3")
  @JsonPropertyDescription("WebMessageInfo")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<MessageInfo> statusV3Messages;

  @JsonProperty("5")
  @JsonPropertyDescription("uint32")
  private int chunkOrder;

  @JsonProperty("6")
  @JsonPropertyDescription("uint32")
  private int progress;

  @JsonProperty("7")
  @JsonPropertyDescription("Pushname")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<PushName> pushNames;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HistorySyncHistorySyncType {
    INITIAL_BOOTSTRAP(0),
    INITIAL_STATUS_V3(1),
    FULL(2),
    RECENT(3),
    PUSH_NAME(4);

    @Getter
    private final int index;

    @JsonCreator
    public static HistorySyncHistorySyncType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
