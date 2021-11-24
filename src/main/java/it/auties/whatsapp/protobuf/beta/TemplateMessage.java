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
public class TemplateMessage {

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("HydratedFourRowTemplate")
  private HydratedFourRowTemplate hydratedFourRowTemplate;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("FourRowTemplate")
  private FourRowTemplate fourRowTemplate;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("HydratedFourRowTemplate")
  private HydratedFourRowTemplate hydratedTemplate;

  @JsonProperty(value = "3", required = false)
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
