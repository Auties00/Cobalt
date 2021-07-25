package it.auties.whatsapp4j.protobuf.model.recent;

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
  @JsonProperty(value = "7")
  private boolean isSentByMe;

  @JsonProperty(value = "6")
  private String participant;

  @JsonProperty(value = "5")
  private String chatJid;

  @JsonProperty(value = "4")
  private String stanzaId;

  @JsonProperty(value = "3")
  private String mediaKey;

  @JsonProperty(value = "2")
  private String encFilehash;

  @JsonProperty(value = "1")
  private String directPath;
}
