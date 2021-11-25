package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ExternalAdReplyInfo {
  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String sourceUrl;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String sourceId;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String sourceType;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnail;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String mediaUrl;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String thumbnailUrl;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ExternalAdReplyInfoMediaType")
  private ExternalAdReplyInfoMediaType mediaType;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String body;

  @JsonProperty(value = "1")
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
