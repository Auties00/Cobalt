package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;

import java.util.Collection;

public interface OnWhatsappCompanionDevices extends Listener {
    /**
     * Called when the list of companion devices is updated
     *
     * @param whatsapp an instance to the calling api
     * @param devices  the non-null devices
     */
    @Override
    void onCompanionDevices(Whatsapp whatsapp, Collection<ContactJid> devices);
}
