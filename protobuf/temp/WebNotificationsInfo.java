package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class WebNotificationsInfo {
  @JsonProperty(value = "5")
  @JsonPropertyDescription("WebMessageInfo")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<MessageInfo> notifyMessages;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint32")
  private int notifyMessageCount;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("uint32")
  private int unreadChats;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("uint64")
  private long timestamp;
}
