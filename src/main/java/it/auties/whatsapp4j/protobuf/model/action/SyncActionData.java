package it.auties.whatsapp4j.protobuf.model.action;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SyncActionData {
  @JsonProperty(value = "4")
  private int version;

  @JsonProperty(value = "3")
  private byte[] padding;

  @JsonProperty(value = "2")
  private SyncActionValue value;

  @JsonProperty(value = "1")
  private byte[] index;
}
