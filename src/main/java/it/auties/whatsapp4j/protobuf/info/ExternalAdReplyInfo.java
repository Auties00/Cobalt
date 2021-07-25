package it.auties.whatsapp4j.protobuf.info;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ExternalAdReplyInfo {
  @JsonProperty(value = "9")
  private String sourceUrl;

  @JsonProperty(value = "8")
  private String sourceId;

  @JsonProperty(value = "7")
  private String sourceType;

  @JsonProperty(value = "6")
  private byte[] thumbnail;

  @JsonProperty(value = "5")
  private String mediaUrl;

  @JsonProperty(value = "4")
  private String thumbnailUrl;

  @JsonProperty(value = "3")
  private ExternalAdReplyInfoMediaType mediaType;

  @JsonProperty(value = "2")
  private String body;

  @JsonProperty(value = "1")
  private String title;

  @Accessors(fluent = true)
  public enum ExternalAdReplyInfoMediaType {
    NONE(0),
    IMAGE(1),
    VIDEO(2);

    private final @Getter int index;

    ExternalAdReplyInfoMediaType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ExternalAdReplyInfoMediaType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
