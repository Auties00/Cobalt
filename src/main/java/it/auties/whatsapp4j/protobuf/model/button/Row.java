package it.auties.whatsapp4j.protobuf.model.button;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
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
