package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.media.AttachmentProvider;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class HistorySyncNotification implements AttachmentProvider {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty("2")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] key;

  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty("6")
  @JsonPropertyDescription("HistorySyncNotificationHistorySyncType")
  private HistorySyncNotificationHistorySyncType syncType;

  @JsonProperty("7")
  @JsonPropertyDescription("uint32")
  private int chunkOrder;

  @JsonProperty("8")
  @JsonPropertyDescription("string")
  private String originalMessageId;

  @Override
  public String url() {
    return null;
  }

  @Override
  public String name() {
    return "md-msg-hist";
  }

  @Override
  public String keyName() {
    return "WhatsApp History Keys";
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HistorySyncNotificationHistorySyncType {
    INITIAL_BOOTSTRAP(0),
    INITIAL_STATUS_V3(1),
    FULL(2),
    RECENT(3),
    PUSH_NAME(4);

    @Getter
    private final int index;

    @JsonCreator
    public static HistorySyncNotificationHistorySyncType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
