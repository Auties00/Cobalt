package it.auties.whatsapp.protobuf.model.button;

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
public class Row {
  @JsonProperty(value = "3")
  private String rowId;

  @JsonProperty(value = "2")
  private String description;

  @JsonProperty(value = "1")
  private String title;
}
