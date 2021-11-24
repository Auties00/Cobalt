package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HistorySync {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("Pushname")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Pushname> pushnames;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint32")
  private int progress;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint32")
  private int chunkOrder;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("WebMessageInfo")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<WebMessageInfo> statusV3Messages;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Conversation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Conversation> conversations;

  @JsonProperty(value = "1", required = true)
  @JsonPropertyDescription("HistorySyncHistorySyncType")
  private HistorySyncHistorySyncType syncType;

  @Accessors(fluent = true)
  public enum HistorySyncHistorySyncType {
    INITIAL_BOOTSTRAP(0),
    INITIAL_STATUS_V3(1),
    FULL(2),
    RECENT(3),
    PUSH_NAME(4);

    private final @Getter int index;

    HistorySyncHistorySyncType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static HistorySyncHistorySyncType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
