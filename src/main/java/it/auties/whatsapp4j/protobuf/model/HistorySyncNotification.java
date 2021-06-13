package it.auties.whatsapp4j.protobuf.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class HistorySyncNotification {
  @JsonProperty(value = "7")
  private int chunkOrder;

  @JsonProperty(value = "6")
  private HistorySyncType syncType;

  @JsonProperty(value = "5")
  private String directPath;

  @JsonProperty(value = "4")
  private byte[] fileEncSha256;

  @JsonProperty(value = "3")
  private byte[] mediaKey;

  @JsonProperty(value = "2")
  private long fileLength;

  @JsonProperty(value = "1")
  private byte[] fileSha256;

  @Accessors(fluent = true)
  public enum HistorySyncType {
    INITIAL_BOOTSTRAP(0),
    INITIAL_STATUS_V3(1),
    FULL(2),
    RECENT(3);

    private final @Getter int index;

    HistorySyncType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static HistorySyncType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
