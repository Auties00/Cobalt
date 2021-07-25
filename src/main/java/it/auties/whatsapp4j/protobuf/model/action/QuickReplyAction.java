package it.auties.whatsapp4j.protobuf.model.action;

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
  @JsonProperty(value = "5")
  private boolean deleted;

  @JsonProperty(value = "4")
  private int count;

  @JsonProperty(value = "3")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> keywords;

  @JsonProperty(value = "2")
  private String message;

  @JsonProperty(value = "1")
  private String shortcut;
}
