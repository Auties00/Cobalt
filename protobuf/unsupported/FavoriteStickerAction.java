package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class FavoriteStickerAction {

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bool")
  private boolean isFavorite;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String mediaKey;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String stickerHashWithoutMeta;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String encFilehash;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String handle;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String lastUploadTimestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String directPath;
}
