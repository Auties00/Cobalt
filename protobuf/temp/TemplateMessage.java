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
public class TemplateMessage {
  @JsonProperty(value = "2")
  @JsonPropertyDescription("HydratedFourRowTemplate")
  private HydratedFourRowTemplate hydratedFourRowTemplate;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("FourRowTemplate")
  private FourRowTemplate fourRowTemplate;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("HydratedFourRowTemplate")
  private HydratedFourRowTemplate hydratedTemplate;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  public Format formatCase() {
    if (fourRowTemplate != null) return Format.FOUR_ROW_TEMPLATE;
    if (hydratedFourRowTemplate != null) return Format.HYDRATED_FOUR_ROW_TEMPLATE;
    return Format.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Format {
    UNKNOWN(0),
    FOUR_ROW_TEMPLATE(1),
    HYDRATED_FOUR_ROW_TEMPLATE(2);

    private final @Getter int index;

    Format(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Format forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Format.UNKNOWN);
    }
  }
}
