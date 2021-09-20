package it.auties.whatsapp4j.common.protobuf.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ClientFinish {
  @JsonProperty(value = "1")
  private byte[] staticText;

  @JsonProperty(value = "2")
  private byte[] payload;
}
