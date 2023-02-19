package it.auties.whatsapp.serialization;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;

/**
 * This interface provides a standardized way to serialize a session. Implement this interface and
 * <a href="https://www.baeldung.com/java-spi#3-service-provider">register it in your manifest</a>
 */
public non-sealed interface ControllerSerializerProvider extends ControllerProvider {
    /**
     * Serializes the keys
     *
     * @param keys  the non-null keys to serialize
     * @param async whether the operation should be executed asynchronously
     */
    void serializeKeys(Keys keys, boolean async);

    /**
     * Serializes the store
     *
     * @param store the non-null store to serialize
     * @param async whether the operation should be executed asynchronously
     */
    void serializeStore(Store store, boolean async);
}