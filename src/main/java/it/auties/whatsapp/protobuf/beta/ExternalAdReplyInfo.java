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
public class ExternalAdReplyInfo {

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("string")
  private String sourceUrl;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String sourceId;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String sourceType;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String mediaUrl;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String thumbnailUrl;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ExternalAdReplyInfoMediaType")
  private ExternalAdReplyInfoMediaType mediaType;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String body;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
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
