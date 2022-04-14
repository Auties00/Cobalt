package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class NoiseCertificate {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] details;
}
