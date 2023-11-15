package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.util.Validate;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class WhatsappCustomBuilder {
    private Store store;
    private Keys keys;
    private ErrorHandler errorHandler;
    private WebVerificationSupport webVerificationSupport;
    private ExecutorService socketExecutor;

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

    public WhatsappCustomBuilder webVerificationSupport(WebVerificationSupport webVerificationSupport) {
        this.webVerificationSupport = webVerificationSupport;
        return this;
    }

    public WhatsappCustomBuilder socketExecutor(ExecutorService socketExecutor) {
        this.socketExecutor = socketExecutor;
        return this;
    }

    public Whatsapp build() {
        Validate.isTrue(Objects.equals(store.uuid(), keys.uuid()), "UUID mismatch: %s != %s", store.uuid(), keys.uuid());
        var knownInstance = Whatsapp.getInstanceByUuid(store.uuid());
        if (knownInstance.isPresent()) {
            return knownInstance.get();
        }

        var checkedSupport = getWebVerificationMethod(store, keys, webVerificationSupport);
        return new Whatsapp(store, keys, errorHandler, checkedSupport, socketExecutor);
    }

    private static WebVerificationSupport getWebVerificationMethod(Store store, Keys keys, WebVerificationSupport webVerificationSupport) {
        if (store.clientType() != ClientType.WEB) {
            return null;
        }

        if (!keys.registered() && webVerificationSupport == null) {
            return QrHandler.toTerminal();
        }

        return webVerificationSupport;
    }
}
