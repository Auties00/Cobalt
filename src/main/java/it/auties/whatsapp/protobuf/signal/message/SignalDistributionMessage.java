package it.auties.whatsapp.protobuf.signal.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.auties.whatsapp.util.BytesDeserializer;
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
public final class SignalDistributionMessage implements SignalProtocolMessage {
  /**
   * The id of the sender
   */
  @JsonProperty("1")
  @JsonPropertyDescription("uint32")
  private int id;

  /**
   * The iteration of the message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("uint32")
  private int iteration;

  /**
   * The value key of the message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] chainKey;

  /**
   * The signing key of the message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] signingKey;
}
