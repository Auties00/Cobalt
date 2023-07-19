package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.call.Call;

public interface OnWhatsappCall extends Listener {
    /**
     * Called when a phone call arrives
     *
     * @param whatsapp an instance to the calling api
     * @param call the non-null phone call
     */
    @Override
    void onCall(Whatsapp whatsapp, Call call);
}
