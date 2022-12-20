package it.auties.whatsapp.listener;

import it.auties.whatsapp.api.SocketEvent;

public interface OnSocketEvent
        extends Listener {
    /**
     * Called when an event regarding the underlying is fired
     *
     * @param event the event
     */
    @Override
    void onSocketEvent(SocketEvent event);
}
