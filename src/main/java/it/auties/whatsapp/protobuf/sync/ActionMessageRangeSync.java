package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class ActionMessageRangeSync {
  @JsonProperty("1")
  @JsonPropertyDescription("int64")
  private long lastMessageTimestamp;

  @JsonProperty("2")
  @JsonPropertyDescription("int64")
  private long lastSystemMessageTimestamp;

  @JsonProperty("3")
  @JsonPropertyDescription("SyncActionMessage")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncActionMessage> messages;
}
