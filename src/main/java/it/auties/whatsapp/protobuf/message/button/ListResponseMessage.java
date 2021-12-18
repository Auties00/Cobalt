package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.button.SingleSelectReply;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * A model class that represents a WhatsappMessage that contains a response to a previous {@link ListMessage}.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class ListResponseMessage implements ButtonMessage {
  /**
   * The title of this message
   */
  @JsonProperty(value = "1")
  private String title;

  /**
   * The selected option
   */
  @JsonProperty(value = "3")
  private SingleSelectReply reply;

  /**
   * The context info of this message
   */
  @JsonProperty(value = "4")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The description of this message
   */
  @JsonProperty(value = "5")
  private String description;
}
