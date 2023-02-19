package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.Whatsapp;

public interface OnWhatsappHistorySyncProgress extends Listener {
    /**
     * Called when the socket receives the sync percentage for the full or recent chunk of messages.
     * This method is only called when the QR is first scanned and history is being synced.
     *
     * @param whatsapp   an instance to the calling api
     * @param percentage the percentage synced up to now
     * @param recent     whether the sync is about the recent messages or older messages
     */
    @Override
    void onHistorySyncProgress(Whatsapp whatsapp, int percentage, boolean recent);
}
