package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebNotificationsInfo {
  @JsonProperty(value = "5")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<MessageInfo> notifyMessages;

  @JsonProperty(value = "4")
  private int notifyMessageCount;

  @JsonProperty(value = "3")
  private int unreadChats;

  @JsonProperty(value = "2")
  private long timestamp;
}
