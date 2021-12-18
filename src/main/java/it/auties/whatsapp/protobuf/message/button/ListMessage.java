package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.info.ProductListInfo;
import it.auties.whatsapp.protobuf.button.ButtonSection;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

/**
 * A model class that represents a WhatsappMessage that contains a list of buttons or a list of products.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@Accessors(fluent = true)
public final class ListMessage extends ContextualMessage implements ButtonMessage {
  /**
   * The title of this message
   */
  @JsonProperty(value = "1")
  private String title;

  /**
   * The description of this message
   */
  @JsonProperty(value = "2")
  private String description;

  /**
   * The text of the button of this message
   */
  @JsonProperty(value = "3")
  private String buttonText;

  /**
   * The type of this message
   */
  @JsonProperty(value = "4")
  private Type type;

  /**
   * The button sections of this message
   */
  @JsonProperty(value = "5")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<ButtonSection> sections;

  /**
   * The product info of this message
   */
  @JsonProperty(value = "6")
  private ProductListInfo productListInfo;

  /**
   * The footer text of this message
   */
  @JsonProperty(value = "7")
  private String footerText;

  /**
   * The context info of this message
   */
  @JsonProperty(value = "8")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The constants of this enumerated type describe the various types of {@link ListMessage}
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Type {
    /**
     * Unknown
     */
    UNKNOWN(0),

    /**
     * Only one option can be selected
     */
    SINGLE_SELECT(1),

    /**
     * A list of products
     */
    PRODUCT_LIST(2);

    private final @Getter int index;

    @JsonCreator
    public static Type forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
