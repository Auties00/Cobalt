package it.auties.whatsapp.protobuf.model;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class SenderKeyStateStructure {
  @JsonProperty(value = "1")
  @JsonPropertyDescription("uint32")
  private int senderKeyId;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SenderChainKey")
  private SenderChainKey senderChainKey;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("SenderSigningKey")
  private SenderSigningKey senderSigningKey;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("SenderMessageKey")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<SenderMessageKey> senderMessageKeys;
}
