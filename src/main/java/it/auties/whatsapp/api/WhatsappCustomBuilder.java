package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.util.Validate;

import java.util.Objects;

public class WhatsappCustomBuilder {
    private Store store;
    private Keys keys;
    private ErrorHandler errorHandler;
    private WebVerificationHandler webVerificationHandler;

    WhatsappCustomBuilder() {

    }

    public WhatsappCustomBuilder store(Store store) {
        this.store = store;
        return this;
    }

    public WhatsappCustomBuilder keys(Keys keys) {
        this.keys = keys;
        return this;
    }

    public WhatsappCustomBuilder errorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public WhatsappCustomBuilder webVerificationSupport(WebVerificationHandler webVerificationHandler) {
        this.webVerificationHandler = webVerificationHandler;
        return this;
    }

    public Whatsapp build() {
        Validate.isTrue(Objects.equals(store.uuid(), keys.uuid()), "UUID mismatch: %s != %s", store.uuid(), keys.uuid());
        var knownInstance = Whatsapp.getInstanceByUuid(store.uuid());
        if (knownInstance.isPresent()) {
            return knownInstance.get();
        }

        var checkedSupport = getWebVerificationMethod(store, keys, webVerificationHandler);
        return new Whatsapp(store, keys, errorHandler, checkedSupport);
    }

    private static WebVerificationHandler getWebVerificationMethod(Store store, Keys keys, WebVerificationHandler webVerificationHandler) {
        if (store.clientType() != ClientType.WEB) {
            return null;
        }

        if (!keys.registered() && webVerificationHandler == null) {
            return QrHandler.toTerminal();
        }

        return webVerificationHandler;
    }
}
