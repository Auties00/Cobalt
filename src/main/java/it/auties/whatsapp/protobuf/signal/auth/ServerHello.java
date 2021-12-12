package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class ServerHello {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] payload;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] staticText;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] ephemeral;
}
