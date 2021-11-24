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
public class DeviceSentMessage {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String phash;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("Message")
  private Message message;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String destinationJid;
}
