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
public class CompanionRegData {

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] companionProps;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] buildHash;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eSkeySig;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eSkeyVal;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eSkeyId;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eIdent;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eKeytype;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] eRegid;
}
