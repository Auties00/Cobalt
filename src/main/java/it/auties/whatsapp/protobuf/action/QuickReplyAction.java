package it.auties.whatsapp.protobuf.action;

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
public class QuickReplyAction {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String shortcut;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String message;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> keywords;

  @JsonProperty("4")
  @JsonPropertyDescription("int32")
  private int count;

  @JsonProperty("5")
  @JsonPropertyDescription("bool")
  private boolean deleted;
}
