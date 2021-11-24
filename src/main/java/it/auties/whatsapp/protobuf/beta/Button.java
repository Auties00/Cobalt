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
public class Button {

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("NativeFlowInfo")
  private NativeFlowInfo nativeFlowInfo;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("ButtonType")
  private ButtonType type;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("ButtonText")
  private ButtonText buttonText;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String buttonId;

  @Accessors(fluent = true)
  public enum ButtonType {
    UNKNOWN(0),
    RESPONSE(1),
    NATIVE_FLOW(2);

    private final @Getter int index;

    ButtonType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ButtonType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
