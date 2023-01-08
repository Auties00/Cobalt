package it.auties.whatsapp.model.message.model;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessage;
import it.auties.whatsapp.model.message.button.InteractiveMessage;
import it.auties.whatsapp.model.message.button.InteractiveResponseMessage;
import it.auties.whatsapp.model.message.button.ListMessage;
import it.auties.whatsapp.model.message.button.TemplateMessage;
import it.auties.whatsapp.model.message.standard.ContactMessage;
import it.auties.whatsapp.model.message.standard.ContactsArrayMessage;
import it.auties.whatsapp.model.message.standard.GroupInviteMessage;
import it.auties.whatsapp.model.message.standard.LiveLocationMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.PollCreationMessage;
import it.auties.whatsapp.model.message.standard.ProductMessage;
import it.auties.whatsapp.model.message.standard.RequestPhoneNumberMessage;
import it.auties.whatsapp.model.message.standard.TextMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A model interface that represents a message sent by a contact that provides a context. Classes
 * that implement this interface must provide an accessor named contextInfo to access said
 * property.
 */
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Data
@Accessors(fluent = true)
public sealed abstract class ContextualMessage
    implements Message
    permits ButtonsMessage, InteractiveMessage, InteractiveResponseMessage, ListMessage,
    TemplateMessage, ButtonReplyMessage, MediaMessage,
    it.auties.whatsapp.model.message.payment.PaymentOrderMessage, ContactMessage,
    ContactsArrayMessage, GroupInviteMessage, LiveLocationMessage, LocationMessage,
    PollCreationMessage, ProductMessage, RequestPhoneNumberMessage, TextMessage {

  private static final ContextInfo EMPTY_CONTEXT = new ContextInfo();

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 17, type = MESSAGE, implementation = ContextInfo.class)
  private ContextInfo contextInfo = new ContextInfo();

  public ContextInfo contextInfo() {
    return contextInfo == null ? EMPTY_CONTEXT : contextInfo;
  }
}
