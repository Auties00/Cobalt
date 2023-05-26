package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public final class WebOptionsBuilder extends OptionsBuilder<WebOptionsBuilder> {
    private Whatsapp whatsapp;

    WebOptionsBuilder(Store store, Keys keys) {
        super(store, keys);
    }

    static Optional<WebOptionsBuilder> of(UUID connectionUuid, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(connectionUuid, serializer, connectionType, ClientType.WEB);
        var required = connectionType == ConnectionType.KNOWN;
        var store = Store.of(uuid, null, ClientType.WEB, serializer, required);
        if(required && store.isEmpty()){
            return Optional.empty();
        }

        var keys = Keys.of(uuid, null, ClientType.WEB, serializer, required);
        if(required && keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new WebOptionsBuilder(store.get(), keys.get()));
    }

    static Optional<WebOptionsBuilder> of(long phoneNumber, ControllerSerializer serializer, ConnectionType connectionType){
        var uuid = getCorrectUuid(null, serializer, connectionType, ClientType.WEB);
        var required = connectionType == ConnectionType.KNOWN;
        var store = Store.of(uuid, phoneNumber, ClientType.WEB, serializer, required);
        if(required && store.isEmpty()){
            return Optional.empty();
        }

        var keys = Keys.of(uuid, phoneNumber, ClientType.WEB, serializer, required);
        if(required && keys.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(new WebOptionsBuilder(store.get(), keys.get()));
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