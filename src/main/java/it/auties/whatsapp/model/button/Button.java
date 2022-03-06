package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.model.info.NativeFlowInfo;
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
public class Button {
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String buttonId;

  @JsonProperty("2")
  @JsonPropertyDescription("ButtonText")
  private ButtonText buttonText;

  @JsonProperty("3")
  @JsonPropertyDescription("ButtonType")
  private ButtonType type;

  @JsonProperty("4")
  @JsonPropertyDescription("NativeFlowInfo")
  private NativeFlowInfo nativeFlowInfo;

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum ButtonType {
    UNKNOWN(0),
    RESPONSE(1),
    NATIVE_FLOW(2);

    @Getter
    private final int index;

    @JsonCreator
    public static ButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
