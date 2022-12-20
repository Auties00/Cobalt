package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

import java.util.List;

public interface OnWhatsappFeatures
        extends Listener {
    /**
     * Called when the socket receives new features from Whatsapp.
     *
     * @param whatsapp an instance to the calling api
     * @param features the non-null features that were sent
     */
    @Override
    void onFeatures(Whatsapp whatsapp, List<String> features);
}