package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.socket.SocketHandler;

public interface OnWhatsappContactPresence extends Listener {
    /**
     * Called when {@link SocketHandler} receives an update regarding the presence of a contact
     *
     * @param whatsapp an instance to the calling api
     * @param chat    the chat that this update regards
     * @param contact the contact that this update regards
     * @param status  the new status of the contact
     */
    @Override
    void onContactPresence(Whatsapp whatsapp, Chat chat, Contact contact, ContactStatus status);
}