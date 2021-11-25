package it.auties.whatsapp.protobuf.authentication;

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
  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] id;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] keyType;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] identifier;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] signatureId;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("bytes")
  private byte[] signaturePublicKey;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bytes")
  private byte[] signature;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bytes")
  private byte[] buildHash;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bytes")
  private byte[] companion;
}
