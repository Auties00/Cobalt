package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.SingleSelectReply;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a WhatsappMessage that contains a response to a previous {@link ButtonListMessage}.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class ButtonListResponseMessage implements ButtonMessage {
  /**
   * The title of this message
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  /**
   * The selected option
   */
  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = SingleSelectReply.class)
  private SingleSelectReply reply;

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = ContextInfo.class)
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The description of this message
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String description;
}
