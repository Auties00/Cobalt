package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.miscellanous.FourRowTemplate;
import it.auties.whatsapp4j.protobuf.miscellanous.HydratedFourRowTemplate;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class TemplateMessage implements Message {
  @JsonProperty(value = "2")
  private HydratedFourRowTemplate hydratedFourRowTemplate;

  @JsonProperty(value = "1")
  private FourRowTemplate fourRowTemplate;

  @JsonProperty(value = "4")
  private HydratedFourRowTemplate hydratedTemplate;

  @JsonProperty(value = "3")
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
