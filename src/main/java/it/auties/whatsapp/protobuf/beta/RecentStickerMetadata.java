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
public class RecentStickerMetadata {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("bool")
  private boolean isSentByMe;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String chatJid;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String stanzaId;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String mediaKey;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String encFilehash;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String directPath;
}
