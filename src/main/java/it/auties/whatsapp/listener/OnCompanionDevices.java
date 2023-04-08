package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.contact.ContactJid;

import java.util.Collection;

public interface OnCompanionDevices extends Listener {
    /**
     * Called when the list of companion devices is updated
     *
     * @param devices  the non-null devices
     */
    @Override
    void onCompanionDevices(Collection<ContactJid> devices);
}
