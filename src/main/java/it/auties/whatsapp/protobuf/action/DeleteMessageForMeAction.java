package it.auties.whatsapp.protobuf.action;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class DeleteMessageForMeAction implements Action {
  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean deleteMedia;

  @JsonProperty("2")
  @JsonPropertyDescription("int64")
  private long messageTimestamp;

  @Override
  public String indexName() {
    return "deleteMessageForMe";
  }
}
