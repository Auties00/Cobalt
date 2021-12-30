package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Details {

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] key;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String subject;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint64")
  private long expires;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String issuer;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("uint32")
  private int serial;
}
