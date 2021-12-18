package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model class that represents a WhatsappMessage that contains a response to a previous {@link StructuredButtonMessage}.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(builderMethodName = "newTemplateButtonReplyMessage", buildMethodName = "create")
@Accessors(fluent = true)
public final class TemplateButtonReplyMessage extends ContextualMessage implements ButtonMessage {
  /**
   * The id of the button that was selected from the previous template message
   */
  @JsonProperty(value = "1")
  private String id;

  /**
   * The text of the button that was selected from the previous template message
   */
  @JsonProperty(value = "2")
  private String displayText;

  /**
   * The context info of this message
   */
  @JsonProperty(value = "3")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The index of the button that was selected from the previous template message
   */
  @JsonProperty(value = "4")
  private int index;
}
