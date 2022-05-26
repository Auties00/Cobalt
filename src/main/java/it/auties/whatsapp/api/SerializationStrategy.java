package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * This interface provides a medium to serialize a pair of {@link WhatsappStore} and {@link WhatsappKeys} using custom logic.
 * This logic is fired when a particular event, described by {@link Event}, is fired.
 *
 * @apiNote Calling async operations in a onClose block may result in undefined behaviour
 */
public interface SerializationStrategy {
    /**
     * Serialize the credentials
     *
     * @param store the non-null store
     * @param keys  the non-null keys
     */
    void serialize(WhatsappStore store, WhatsappKeys keys);

    /**
     * The event used as a trigger for this strategy's logic
     *
     * @return a non-null event
     */
    Event trigger();

    /**
     * The time that should elapse between each call to this strategy's logic.
     * Only relevant if linked to a {@link Event#PERIODICALLY} trigger.
     *
     * @return a long
     */
    default long period() {
        return 0;
    }

    /**
     * The time unit used for the time that should elapse between each call to this strategy's logic.
     * Only relevant if linked to a {@link Event#PERIODICALLY} trigger.
     * By default, seconds are used.
     *
     * @return a non-null time unit
     */
    default TimeUnit unit() {
        return TimeUnit.SECONDS;
    }

    /**
     * Constructs the default serialization strategy and links it to a close trigger
     *
     * @return a non-null strategy
     */
    static SerializationStrategy onClose() {
        return onClose(null);
    }

    /**
     * Constructs the default serialization strategy using a specific file and links it to a close trigger
     *
     * @param path the path to the file used as memory, null if the default should be used
     * @return a non-null strategy
     */
    static SerializationStrategy onClose(Path path) {
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

    /**
     * Constructs the default serialization strategy and links it to an error trigger
     *
     * @param async whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy onError(boolean async) {
        return onError(null, async);
    }

    /**
     * Constructs the default serialization strategy using a specific file and links it to an error trigger
     *
     * @param path  the path to the file used as memory, null if the default should be used
     * @param async whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy onError(Path path, boolean async) {
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

    /**
     * Constructs the default serialization strategy and links it to a Whatsapp message, not node, trigger
     *
     * @param async whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy onMessage(boolean async) {
        return onMessage(null, async);
    }

    /**
     * Constructs the default serialization strategy using a specific file and links it to a Whatsapp message, not node, trigger
     *
     * @param path  the path to the file used as memory, null if the default should be used
     * @param async whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy onMessage(Path path, boolean async) {
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

    /**
     * Constructs the default serialization strategy and links it to a periodic trigger called every {@code period} seconds
     *
     * @param period the time that should elapse between each call
     * @param async  whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy periodically(long period, boolean async) {
        return periodically(period, null, async);
    }

    /**
     * Constructs the default serialization strategy using a specific file and links it to a periodic trigger called every {@code period} seconds
     *
     * @param period the time that should elapse between each call
     * @param path   the path to the file used as memory, null if the default should be used
     * @param async  whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy periodically(long period, Path path, boolean async) {
        return periodically(period, TimeUnit.SECONDS, path, async);
    }

    /**
     * Constructs the default serialization strategy using a specific file and links it to a periodic trigger called every {@code period} {@code unit}
     *
     * @param period the time that should elapse between each call
     * @param unit   the time uint to use for the period parameter
     * @param path   the path to the file used as memory, null if the default should be used
     * @param async  whether the serialization operation should be called asynchronously
     * @return a non-null strategy
     */
    static SerializationStrategy periodically(long period, @NonNull TimeUnit unit, Path path, boolean async) {
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

    /**
     * Constructs a custom serialization strategy from a consumer and a trigger event
     *
     * @param consumer the non-null consumer for the store and keys
     * @param event    the non-null event to use as a trigger
     * @return a non-null strategy
     */
    static SerializationStrategy custom(@NonNull BiConsumer<WhatsappStore, WhatsappKeys> consumer, @NonNull Event event) {
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

    /**
     * The constants of this enumerated type describe the various events that can be used a trigger for a {@link SerializationStrategy}
     */
    enum Event {
        /**
         * Called when the socket is closed
         *
         * @apiNote async operations may not be completed
         */
        ON_CLOSE,

        /**
         * Called when the API receives a message from a contact
         */
        ON_MESSAGE,

        /**
         * Called when an unexpected error is thrown, can be used as a safety mechanism
         */
        ON_ERROR,

        /**
         * Called periodically
         */
        PERIODICALLY,

        /**
         * Other events
         */
        CUSTOM
    }

    private static void save(WhatsappStore store, WhatsappKeys keys, Path path, boolean async) {
        Objects.requireNonNull(store, "Cannot serialize: null store");
        Objects.requireNonNull(keys, "Cannot serialize: null keys");
        if (path == null) {
            store.save(async);
            keys.save(async);
            return;
        }

        store.save(path, async);
        keys.save(path, async);
    }
}
