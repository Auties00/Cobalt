package it.auties.whatsapp.protobuf.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class DeleteMessageForMeAction {
  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean deleteMedia;

  @JsonProperty("2")
  @JsonPropertyDescription("int64")
  private long messageTimestamp;
}
