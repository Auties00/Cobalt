package it.auties.whatsapp4j.protobuf.model.key;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class KeyId {
  @JsonProperty(value = "1")
  private byte[] id;
}
