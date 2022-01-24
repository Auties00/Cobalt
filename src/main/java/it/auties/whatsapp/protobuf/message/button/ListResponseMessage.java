package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.button.SingleSelectReply;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String title;

  /**
   * The selected option
   */
  @JsonProperty("3")
  @JsonPropertyDescription("reply")
  private SingleSelectReply reply;

  /**
   * The context info of this message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("context")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The description of this message
   */
  @JsonProperty("5")
  @JsonPropertyDescription("string")
  private String description;
}
