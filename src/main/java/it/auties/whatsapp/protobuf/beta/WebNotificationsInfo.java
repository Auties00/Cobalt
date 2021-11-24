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
public class WebNotificationsInfo {

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("WebMessageInfo")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<WebMessageInfo> notifyMessages;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int notifyMessageCount;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int unreadChats;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint64")
  private long timestamp;
}
