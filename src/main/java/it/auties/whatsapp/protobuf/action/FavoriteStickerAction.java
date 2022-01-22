package it.auties.whatsapp.protobuf.action;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class FavoriteStickerAction implements Action {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String lastUploadTimestamp;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String handle;

  @JsonProperty("4")
  @JsonPropertyDescription("string")
  private String encFileHash;

  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String stickerHashWithoutMeta;

  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String mediaKey;

  @JsonProperty("7")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty("8")
  @JsonPropertyDescription("bool")
  private boolean isFavourite;
}
