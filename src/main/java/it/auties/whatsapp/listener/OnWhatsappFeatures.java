package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.binary.Socket;

import java.util.List;

public interface OnWhatsappFeatures extends Listener {
    /**
     * Called when {@link Socket} receives new features from Whatsapp.
     *
     * @param features the non-null features that were sent
     */
    void onFeatures(Whatsapp whatsapp, List<String> features);
}