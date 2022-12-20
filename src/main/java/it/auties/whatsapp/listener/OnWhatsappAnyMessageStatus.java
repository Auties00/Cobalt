package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageStatus;

public interface OnWhatsappAnyMessageStatus
        extends Listener {
    /**
     * Called when the status of a message changes inside any type of chat.
     * If {@code chat} is a conversation with {@code contact}, the new read status can be considered valid for the message itself(global status).
     * Otherwise, it should be considered valid only for {@code contact}.
     * If you only need updates regarding conversation, implement {@link Listener#onConversationMessageStatus(Whatsapp, MessageInfo, MessageStatus)}.
     *
     * @param whatsapp an instance to the calling api
     * @param chat     the chat that triggered a status change
     * @param contact  the contact that triggered a status change
     * @param info     the message whose status changed
     * @param status   the new status of the message
     */
    @Override
    void onAnyMessageStatus(Whatsapp whatsapp, Chat chat, Contact contact, MessageInfo info, MessageStatus status);
}
