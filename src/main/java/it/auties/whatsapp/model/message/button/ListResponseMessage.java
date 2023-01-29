package it.auties.whatsapp.model.message.button;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.button.SingleSelectReplyButton;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonReplyMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message that contains a response to a previous
 * {@link ListMessage}
 */
@AllArgsConstructor
@Data
@SuperBuilder
@Jacksonized
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public final class ListResponseMessage extends ButtonReplyMessage {
  /**
   * The title of this message
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String title;

  /**
   * The selected option
   */
  @ProtobufProperty(index = 3, type = MESSAGE, implementation = SingleSelectReplyButton.class)
  private SingleSelectReplyButton reply;

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 4, type = MESSAGE, implementation = ContextInfo.class)
  @Builder.Default
  private ContextInfo contextInfo = new ContextInfo();

  /**
   * The description of this message
   */
  @ProtobufProperty(index = 5, type = STRING)
  private String description;

  @ProtobufProperty(index = 2, name = "listType", type = ProtobufType.MESSAGE)
  private ListMessage.Type listType;

  @Override
  public MessageType type() {
    return MessageType.LIST_RESPONSE;
  }
}