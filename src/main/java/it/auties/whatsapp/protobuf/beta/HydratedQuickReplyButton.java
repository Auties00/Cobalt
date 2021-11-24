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
public class HydratedQuickReplyButton {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String id;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String displayText;
}
