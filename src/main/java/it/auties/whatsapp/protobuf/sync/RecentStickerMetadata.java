package it.auties.whatsapp.protobuf.sync;

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
public class RecentStickerMetadata {
  @JsonProperty("7")
  @JsonPropertyDescription("bool")
  private boolean sentByMe;

  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String participant;

  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String chatJid;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String stanzaId;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String mediaKey;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String encFilehash;

  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String directPath;
}
