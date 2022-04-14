package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PaymentBackground {

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("PaymentBackgroundType")
  private PaymentBackgroundType type;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("MediaData")
  private MediaData mediaData;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("fixed32")
  private int subtextArgb;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("fixed32")
  private int textArgb;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("fixed32")
  private int placeholderArgb;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String id;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum PaymentBackgroundType {
    UNKNOWN(0),
    DEFAULT(1);

    @Getter
    private final int index;

    @JsonCreator
    public static PaymentBackgroundType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
