package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactStatus;

public interface OnWhatsappContactPresence extends Listener {
    /**
     * Called when {@link Socket} receives an update regarding the presence of a contact
     *
     * @param chat    the chat that this update regards
     * @param contact the contact that this update regards
     * @param status  the new status of the contact
     */
    void onContactPresence(Whatsapp whatsapp, Chat chat, Contact contact, ContactStatus status);
}