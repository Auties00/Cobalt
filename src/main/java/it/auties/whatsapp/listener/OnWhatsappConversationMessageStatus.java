package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;

public interface OnWhatsappConversationMessageStatus
    extends Listener {

  /**
   * Called when the status of a message changes inside a conversation. This means that the status
   * change can be considered global as the only other participant is the contact. If you need
   * updates regarding any chat, implement
   * {@link Listener#onAnyMessageStatus(Chat, Contact, MessageInfo, MessageStatus)}
   *
   * @param whatsapp an instance to the calling api
   * @param info     the message whose status changed
   * @param status   the new status of the message
   */
  @Override
  void onConversationMessageStatus(Whatsapp whatsapp, MessageInfo info, MessageStatus status);
}