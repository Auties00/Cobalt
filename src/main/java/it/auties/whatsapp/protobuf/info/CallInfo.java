package it.auties.whatsapp.protobuf.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that holds the information related to a Whatsapp call.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class CallInfo implements WhatsappInfo {
  /**
   * The key of this call
   */
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] key;

  /**
   * The source of this call
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String source;

  /**
   * The data of this call
   */
  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] data;

  /**
   * The delay of this call in seconds
   */
  @JsonProperty("4")
  @JsonPropertyDescription("uint32")
  private int delay;
}
