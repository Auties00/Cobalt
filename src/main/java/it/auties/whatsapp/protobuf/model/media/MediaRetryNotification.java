package it.auties.whatsapp.protobuf.model.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  private MediaRetryNotificationResultType result;

  @JsonProperty(value = "2")
  private String directPath;

  @JsonProperty(value = "1")
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
