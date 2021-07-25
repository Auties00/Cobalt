package it.auties.whatsapp4j.protobuf.model.client;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class NoiseCertificate {
  @JsonProperty(value = "2")
  private byte[] signature;

  @JsonProperty(value = "1")
  private byte[] details;
}
