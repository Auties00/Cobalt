package it.auties.whatsapp.protobuf.signal.auth;

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
public class CompanionData {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] id;

  @JsonProperty("2")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] keyType;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] identifier;

  @JsonProperty("4")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] signatureId;

  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] signaturePublicKey;

  @JsonProperty("6")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] signature;

  @JsonProperty("7")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] buildHash;

  @JsonProperty("8")
  @JsonPropertyDescription("bytes")
  @JsonDeserialize(using = BytesDeserializer.class)
  private byte[] companion;
}
