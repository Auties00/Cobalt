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
public class QuickReplyAction {

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bool")
  private boolean deleted;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("int32")
  private int count;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> keywords;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String message;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String shortcut;
}
