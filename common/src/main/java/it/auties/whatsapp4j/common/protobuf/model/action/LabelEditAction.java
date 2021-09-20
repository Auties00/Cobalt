package it.auties.whatsapp4j.common.protobuf.model.action;

import com.fasterxml.jackson.annotation.JsonProperty;
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
