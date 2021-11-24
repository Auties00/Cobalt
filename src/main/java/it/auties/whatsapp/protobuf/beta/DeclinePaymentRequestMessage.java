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
public class DeclinePaymentRequestMessage {

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("MessageKey")
  private MessageKey key;
}
