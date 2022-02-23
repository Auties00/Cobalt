package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class LocalizedName {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String verifiedName;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String lc;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String lg;
}
