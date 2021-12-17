package it.auties.whatsapp.protobuf.unknown;

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
public class MediaRetryNotification {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("MediaRetryNotificationResultType")
  private MediaRetryNotificationResultType result;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String stanzaId;

  @Accessors(fluent = true)
  public enum MediaRetryNotificationResultType {
    GENERAL_ERROR(0),
    SUCCESS(1),
    NOT_FOUND(2),
    DECRYPTION_ERROR(3);

    private final @Getter int index;

    MediaRetryNotificationResultType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static MediaRetryNotificationResultType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
