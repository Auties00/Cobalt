package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.miscellanous.HistorySyncNotification;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class ProtocolMessage implements Message {
  @JsonProperty(value = "6")
  private HistorySyncNotification historySyncNotification;

  @JsonProperty(value = "5")
  private long ephemeralSettingTimestamp;

  @JsonProperty(value = "4")
  private int ephemeralExpiration;

  @JsonProperty(value = "2")
  private ProtocolMessageType type;

  @JsonProperty(value = "1")
  private MessageKey key;

  @Accessors(fluent = true)
  public enum ProtocolMessageType {
    REVOKE(0),
    EPHEMERAL_SETTING(3),
    EPHEMERAL_SYNC_RESPONSE(4),
    HISTORY_SYNC_NOTIFICATION(5);

    private final @Getter int index;

    ProtocolMessageType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ProtocolMessageType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
