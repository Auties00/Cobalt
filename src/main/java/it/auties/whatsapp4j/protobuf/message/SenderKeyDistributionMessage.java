package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyDistributionMessage implements Message {
  @JsonProperty(value = "2")
  private byte[] axolotlSenderKeyDistributionMessage;

  @JsonProperty(value = "1")
  private String groupId;
}
