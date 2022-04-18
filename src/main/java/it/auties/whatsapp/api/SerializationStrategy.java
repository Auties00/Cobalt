package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public interface SerializationStrategy {
    void serialize(WhatsappStore store, WhatsappKeys keys);
    Event trigger();
    default long period() { return 0; }
    default TimeUnit unit(){ return TimeUnit.SECONDS; }

    static SerializationStrategy onClose(){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                store.save(false);
                keys.save(false);
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
                keys.save(async);
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
                keys.save(async);
            }

            @Override
            public Event trigger() {
                return Event.ON_MESSAGE;
            }
        };
    }

    static SerializationStrategy periodically(long period, boolean async){
        return periodically(period, TimeUnit.SECONDS, async);
    }

    static SerializationStrategy periodically(long period, TimeUnit unit, boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                store.save(async);
                keys.save(async);
            }

            @Override
            public Event trigger() {
                return Event.PERIODICALLY;
            }

            @Override
            public long period() {
                return period;
            }

            @Override
            public TimeUnit unit() {
                return unit;
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
        PERIODICALLY,
        CUSTOM
    }
}
