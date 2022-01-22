package it.auties.whatsapp.protobuf.action;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class AndroidUnsupportedActions implements Action {
  @JsonProperty("1")
  @JsonPropertyDescription("bool")
  private boolean allowed;
}
