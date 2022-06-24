package it.auties.whatsapp.listener;

import it.auties.whatsapp.binary.Socket;

import java.util.List;

public interface OnFeatures extends QrDiscardingListener {
    /**
     * Called when {@link Socket} receives new features from Whatsapp.
     *
     * @param features the non-null features that were sent
     */
    void onFeatures(List<String> features);
}