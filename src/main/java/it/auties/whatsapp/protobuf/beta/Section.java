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
public class Section {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Row")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Row> rows;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String title;
}
