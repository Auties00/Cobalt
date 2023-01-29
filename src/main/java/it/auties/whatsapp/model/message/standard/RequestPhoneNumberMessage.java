package it.auties.whatsapp.model.message.standard;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message holding a request for a phone number inside Still needs
 * to be implemented by Whatsapp
 */
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@ProtobufName("RequestPhoneNumberMessage")
public final class RequestPhoneNumberMessage
    extends ContextualMessage {

  /**
   * The context of this message
   */
  @ProtobufProperty(index = 1, name = "contextInfo", type = MESSAGE)
  @Default
  private ContextInfo contextInfo = new ContextInfo();

  @Override
  public MessageType type() {
    return MessageType.REQUEST_PHONE_NUMBER;
  }

  @Override
  public MessageCategory category() {
    return MessageCategory.STANDARD;
  }
}
