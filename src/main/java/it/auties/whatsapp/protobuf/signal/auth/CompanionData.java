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
public class CompanionData {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] id;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  private byte[] keyType;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] identifier;

  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  private byte[] signatureId;

  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  private byte[] signaturePublicKey;

  @JsonProperty("6")
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty("7")
  @JsonPropertyDescription("bytes")
  private byte[] buildHash;

  @JsonProperty("8")
  @JsonPropertyDescription("bytes")
  private byte[] companion;
}
