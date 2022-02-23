package it.auties.whatsapp.protobuf.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class HydratedTemplateButton {
  @JsonProperty("1")
  @JsonPropertyDescription("HydratedQuickReplyButton")
  private HydratedQuickReplyButton quickReplyButton;

  @JsonProperty("2")
  @JsonPropertyDescription("HydratedURLButton")
  private HydratedURLButton urlButton;

  @JsonProperty("3")
  @JsonPropertyDescription("HydratedCallButton")
  private HydratedCallButton callButton;

  @JsonProperty("4")
  @JsonPropertyDescription("uint32")
  private int index;

  public HydratedButtonType hydratedButtonType() {
    if (quickReplyButton != null) return HydratedButtonType.QUICK_REPLY_BUTTON;
    if (urlButton != null) return HydratedButtonType.URL_BUTTON;
    if (callButton != null) return HydratedButtonType.CALL_BUTTON;
    return HydratedButtonType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HydratedButtonType {
    UNKNOWN(0),
    QUICK_REPLY_BUTTON(1),
    URL_BUTTON(2),
    CALL_BUTTON(3);

    @Getter
    private final int index;

    @JsonCreator
    public static HydratedButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(HydratedButtonType.UNKNOWN);
    }
  }
}
