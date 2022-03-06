package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class CollectionMessage {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("int32")
  private int messageVersion;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String id;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String bizJid;
}
