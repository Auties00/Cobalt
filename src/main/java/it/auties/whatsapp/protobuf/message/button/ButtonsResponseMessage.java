package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage that contains a response to a previous {@link ButtonsMessage}.
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
public final class ButtonsResponseMessage extends ContextualMessage implements ButtonMessage {
  /**
   * The jid of the button that was selected
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String buttonId;

  /**
   * The display text of the button that was selected
   */
  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String displayText;

  /**
   * The context info of this message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("context")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info
}
