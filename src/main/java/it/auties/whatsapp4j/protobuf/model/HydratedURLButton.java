package it.auties.whatsapp4j.protobuf.model;

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
public class HydratedURLButton {
  @JsonProperty(value = "2")
  private String url;

  @JsonProperty(value = "1")
  private String displayText;
}
