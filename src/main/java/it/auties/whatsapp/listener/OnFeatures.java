package it.auties.whatsapp.listener;

import java.util.List;

public interface OnFeatures extends Listener {
    /**
     * Called when the socket receives new features from Whatsapp.
     *
     * @param features the non-null features that were sent
     */
    @Override
    void onFeatures(List<String> features);
}