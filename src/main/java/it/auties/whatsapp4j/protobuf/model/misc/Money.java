package it.auties.whatsapp4j.protobuf.model.misc;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Money {
  @JsonProperty(value = "3")
  private String currencyCode;

  @JsonProperty(value = "2")
  private int offset;

  @JsonProperty(value = "1")
  private long value;
}
