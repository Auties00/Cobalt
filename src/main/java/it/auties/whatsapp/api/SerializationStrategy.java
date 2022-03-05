package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public interface SerializationStrategy {
    void serialize(WhatsappStore store, WhatsappKeys keys);
    Event trigger();

    static SerializationStrategy onClose(){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                store.save(false);
                store.save(false);
            }

            @Override
            public Event trigger() {
                return Event.ON_CLOSE;
            }
        };
    }

    static SerializationStrategy onError(boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                store.save(async);
                store.save(async);
            }

            @Override
            public Event trigger() {
                return Event.ON_ERROR;
            }
        };
    }

    static SerializationStrategy onMessage(boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                store.save(async);
                store.save(async);
            }

            @Override
            public Event trigger() {
                return Event.ON_MESSAGE;
            }
        };
    }

    static SerializationStrategy periodically(long period, boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                    store.save(async);
                    store.save(async);
                }, 0L, period, TimeUnit.SECONDS);
            }

            @Override
            public Event trigger() {
                return Event.OTHER;
            }
        };
    }

    static SerializationStrategy custom(BiConsumer<WhatsappStore, WhatsappKeys> consumer, Event event){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                consumer.accept(store, keys);
            }

            @Override
            public Event trigger() {
                return event;
            }
        };
    }

    enum Event {
        ON_CLOSE,
        ON_MESSAGE,
        ON_ERROR,
        OTHER
    }
}
