package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

import java.util.Map;

public interface OnWhatsappMetadata extends Listener {
    /**
     * Called when an updated list of properties is received.
     * This method is called both when a connection is established with WhatsappWeb and when new props are available.
     * In the latter case though, this object should be considered as partial and is guaranteed to contain only updated entries.
     *
     * @param metadata the updated list of properties
     */
    @Override
    void onMetadata(Whatsapp whatsapp, Map<String, String> metadata);
}