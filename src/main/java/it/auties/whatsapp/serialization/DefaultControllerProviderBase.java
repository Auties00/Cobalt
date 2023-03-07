package it.auties.whatsapp.serialization;

import it.auties.whatsapp.model.contact.ContactJid;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

abstract class DefaultControllerProviderBase {
    protected static final String CHAT_PREFIX = "chat_";

    protected final AtomicReference<CompletableFuture<Void>> deserializer;
    protected final Map<ContactJid, Integer> hashCodesMap;

    public DefaultControllerProviderBase() {
        this.deserializer = new AtomicReference<>();
        this.hashCodesMap = new ConcurrentHashMap<>();
    }
}
