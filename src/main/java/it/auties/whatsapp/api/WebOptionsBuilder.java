package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import lombok.NonNull;

import java.util.UUID;

@SuppressWarnings("unused")
public final class WebOptionsBuilder extends OptionsBuilder<WebOptionsBuilder> {
    private Whatsapp whatsapp;

    public WebOptionsBuilder(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType) {
        super(connectionUuid, serializer, connectionType, ClientType.WEB_CLIENT);
    }

    public WebOptionsBuilder(long phoneNumber, ControllerSerializer serializer, ConnectionType connectionType) {
        super(phoneNumber, serializer, connectionType, ClientType.WEB_CLIENT);
    }

    /**
     * Sets how much chat history Whatsapp should send when the QR is first scanned.
     * By default, one year
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder historyLength(@NonNull WebHistoryLength historyLength) {
        store.historyLength(historyLength);
        return this;
    }

    /**
     * Sets the qr handle
     *
     * @return the same instance for chaining
     */
    public WebOptionsBuilder qrHandler(@NonNull QrHandler qrHandler) {
        store.qrHandler(qrHandler);
        return this;
    }


    /**
     * Opens a connection with Whatsapp Web's WebSocket
     *
     * @return a future
     */
    public Whatsapp build() {
        if (whatsapp == null) {
            this.whatsapp = new Whatsapp(store, keys);
        }

        return whatsapp;
    }
}