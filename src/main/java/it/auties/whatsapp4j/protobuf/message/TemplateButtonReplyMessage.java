package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage sent in a WhatsappBusiness chat to respond to a {@link TemplateMessage} or {@link HighlyStructuredMessage}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newTemplateButtonReplyMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class TemplateButtonReplyMessage extends ContextualMessage {
  /**
   * The index of the button that was selected from the previous template message
   */
  @JsonProperty(value = "4")
  private int selectedIndex;

  /**
   * The context info of this message
   */
  @JsonProperty(value = "3")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The text of the button that was selected from the previous template message
   */
  @JsonProperty(value = "2")
  private String selectedDisplayText;

  /**
   * The id of the button that was selected from the previous template message
   */
  @JsonProperty(value = "1")
  private String selectedId;
}
