package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Collection;

public interface OnWhatsappLinkedDevices extends Listener {
    /**
     * Called when the list of companion devices is updated
     *
     * @param whatsapp an instance to the calling api
     * @param devices  the non-null devices
     */
    @Override
    void onLinkedDevices(Whatsapp whatsapp, Collection<Jid> devices);
}
