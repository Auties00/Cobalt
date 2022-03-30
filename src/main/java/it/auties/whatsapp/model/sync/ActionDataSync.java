package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class ActionDataSync {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] index;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncActionValue")
  private ActionValueSync value;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] padding;

  @JsonProperty("4")
  @JsonPropertyDescription("int32")
  private int version;

  public MessageSync messageIndex(){
    var jsonIndex = new String(index, StandardCharsets.UTF_8);
    return MessageSync.ofJson(jsonIndex);
  }
}
