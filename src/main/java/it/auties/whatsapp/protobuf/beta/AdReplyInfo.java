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
public class AdReplyInfo {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("AdReplyInfoMediaType")
  private AdReplyInfoMediaType mediaType;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String advertiserName;

  @Accessors(fluent = true)
  public enum AdReplyInfoMediaType {
    NONE(0),
    IMAGE(1),
    VIDEO(2);

    private final @Getter int index;

    AdReplyInfoMediaType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static AdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
