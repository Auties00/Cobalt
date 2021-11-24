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
public class LabelEditAction {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bool")
  private boolean deleted;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int32")
  private int predefinedId;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int32")
  private int color;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String name;
}
