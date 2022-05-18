package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public interface SerializationStrategy {
    void serialize(WhatsappStore store, WhatsappKeys keys);
    Event trigger();
    default long period() { return 0; }
    default TimeUnit unit(){ return TimeUnit.SECONDS; }

    static SerializationStrategy onClose(){
        return onClose(null);
    }

    static SerializationStrategy onClose(Path path){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                save(store, keys, path, false);
            }

            @Override
            public Event trigger() {
                return Event.ON_CLOSE;
            }
        };
    }

    static SerializationStrategy onError(boolean async){
        return onError(null, async);
    }

    static SerializationStrategy onError(Path path, boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                save(store, keys, path, async);
            }

            @Override
            public Event trigger() {
                return Event.ON_ERROR;
            }
        };
    }

    static SerializationStrategy onMessage(boolean async){
        return onMessage(null, async);
    }

    static SerializationStrategy onMessage(Path path, boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                save(store, keys, path, async);
            }

            @Override
            public Event trigger() {
                return Event.ON_MESSAGE;
            }
        };
    }

    static SerializationStrategy periodically(long period, boolean async){
        return periodically(period, null, async);
    }

    static SerializationStrategy periodically(long period, Path path, boolean async){
        return periodically(period, TimeUnit.SECONDS, path, async);
    }

    static SerializationStrategy periodically(long period, TimeUnit unit, Path path, boolean async){
        return new SerializationStrategy() {
            @Override
            public void serialize(WhatsappStore store, WhatsappKeys keys) {
                save(store, keys, path, async);
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

    private static void save(WhatsappStore store, WhatsappKeys keys, Path path, boolean async) {
        if(path == null) {
            store.save(async);
            keys.save(async);
            return;
        }

        store.save(path, async);
        keys.save(path, async);
    }

    enum Event {
        ON_CLOSE,
        ON_MESSAGE,
        ON_ERROR,
        PERIODICALLY,
        CUSTOM
    }
}
