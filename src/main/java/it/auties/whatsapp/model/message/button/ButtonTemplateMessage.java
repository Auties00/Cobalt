package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.FourRowTemplate;
import it.auties.whatsapp.model.button.HydratedFourRowTemplate;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents a WhatsappMessage sent in a WhatsappBusiness chat that provides a list of buttons to choose from.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newTemplateMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class ButtonTemplateMessage extends ContextualMessage implements ButtonMessage {
  /**
   * Four row template.
   * This property is defined only if {@link ButtonTemplateMessage#type()} == {@link Format#FOUR_ROW_TEMPLATE}.
   */
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = FourRowTemplate.class)
  private FourRowTemplate fourRowTemplate;

  /**
   * Hydrated four row template.
   * This property is defined only if {@link ButtonTemplateMessage#type()} == {@link Format#HYDRATED_FOUR_ROW_TEMPLATE}.
   */
  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = HydratedFourRowTemplate.class)
  private HydratedFourRowTemplate hydratedFourRowTemplate;

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ContextInfo.class)
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * Hydrated template.
   * This property is defined only if {@link ButtonTemplateMessage#type()} == {@link Format#HYDRATED_FOUR_ROW_TEMPLATE}.
   */
  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = HydratedFourRowTemplate.class)
  private HydratedFourRowTemplate hydratedTemplate;

  /**
   * Returns the type of format of this message
   *
   * @return a non-null {@link Format}
   */
  public Format type() {
    if (fourRowTemplate != null) return Format.FOUR_ROW_TEMPLATE;
    if (hydratedFourRowTemplate != null) return Format.HYDRATED_FOUR_ROW_TEMPLATE;
    return Format.UNKNOWN;
  }

  /**
   * The constant of this enumerated type define the various of types of visual formats for a {@link ButtonTemplateMessage}
   */
  @AllArgsConstructor
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

    @Getter
    private final int index;

    @JsonCreator
    public static Format forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Format.UNKNOWN);
    }
  }
}
