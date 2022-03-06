package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public class ButtonTemplate {
  @JsonProperty("1")
  @JsonPropertyDescription("QuickReplyButton")
  private QuickReplyButton quickReplyButton;

  @JsonProperty("2")
  @JsonPropertyDescription("URLButton")
  private URLButton urlButton;

  @JsonProperty("3")
  @JsonPropertyDescription("CallButton")
  private CallButton callButton;

  @JsonProperty("4")
  @JsonPropertyDescription("uint32")
  private int index;

  public ButtonType buttonType() {
    if (quickReplyButton != null) return ButtonType.QUICK_REPLY_BUTTON;
    if (urlButton != null) return ButtonType.URL_BUTTON;
    if (callButton != null) return ButtonType.CALL_BUTTON;
    return ButtonType.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ButtonType {
    UNKNOWN(0),
    QUICK_REPLY_BUTTON(1),
    URL_BUTTON(2),
    CALL_BUTTON(3);

    @Getter
    private final int index;

    @JsonCreator
    public static ButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(ButtonType.UNKNOWN);
    }
  }
}
