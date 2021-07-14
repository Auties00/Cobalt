package it.auties.whatsapp4j.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import it.auties.whatsapp4j.protobuf.message.model.BusinessMessage;
import it.auties.whatsapp4j.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp4j.protobuf.model.FourRowTemplate;
import it.auties.whatsapp4j.protobuf.model.HydratedFourRowTemplate;
import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage sent in a WhatsappBusiness chat that provides a list of buttons to choose from.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newTemplateMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class TemplateMessage extends ContextualMessage implements BusinessMessage {
  /**
   * Hydrated four row template.
   * This property is defined only if {@link TemplateMessage#formatCase()} == {@link Format#HYDRATED_FOUR_ROW_TEMPLATE}.
   */
  @JsonProperty(value = "2")
  private HydratedFourRowTemplate hydratedFourRowTemplate;

  /**
   * Four row template.
   * This property is defined only if {@link TemplateMessage#formatCase()} == {@link Format#FOUR_ROW_TEMPLATE}.
   */
  @JsonProperty(value = "1")
  private FourRowTemplate fourRowTemplate;

  /**
   * Hydrated template.
   * This property is defined only if {@link TemplateMessage#formatCase()} == {@link Format#HYDRATED_FOUR_ROW_TEMPLATE}.
   */
  @JsonProperty(value = "4")
  private HydratedFourRowTemplate hydratedTemplate;

  /**
   * The context info of this message
   */
  @JsonProperty(value = "3")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * Returns the type of format of this message
   *
   * @return a non null {@link Format}
   */
  public Format formatCase() {
    if (fourRowTemplate != null) return Format.FOUR_ROW_TEMPLATE;
    if (hydratedFourRowTemplate != null) return Format.HYDRATED_FOUR_ROW_TEMPLATE;
    return Format.UNKNOWN;
  }

  /**
   * The constant of this enumerated type define the various of types of visual formats for a {@link TemplateMessage}
   */
  @Accessors(fluent = true)
  public enum Format {
    /**
     * Unknown format
     */
    UNKNOWN(0),

    /**
     * Four row template
     */
    FOUR_ROW_TEMPLATE(1),

    /**
     * Hydrated four row template
     */
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
