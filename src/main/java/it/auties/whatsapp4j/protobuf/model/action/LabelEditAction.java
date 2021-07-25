package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class LabelEditAction {
  @JsonProperty(value = "4")
  private boolean deleted;

  @JsonProperty(value = "3")
  private int predefinedId;

  @JsonProperty(value = "2")
  private int color;

  @JsonProperty(value = "1")
  private String name;
}
