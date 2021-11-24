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
public class SyncActionMessageRange {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("SyncActionMessage")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SyncActionMessage> messages;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int64")
  private long lastSystemMessageTimestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("int64")
  private long lastMessageTimestamp;
}
