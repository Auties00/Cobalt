package it.auties.whatsapp.serialization;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * This interface provides a standardized way to deserialize a session.
 * Implement this interface and <a href="https://www.baeldung.com/java-spi#3-service-provider">register it in your manifest</a>
 */
public non-sealed interface ControllerDeserializerProvider extends ControllerProvider {
    /**
     * Serializes the keys
     *
     * @param id the id of the keys
     * @return a non-null keys
     */
    Optional<Keys> deserializeKeys(int id);

    /**
     * Serializes the store
     *
     * @param id the id of the store
     * @return a non-null store
     */
    Optional<Store> deserializeStore(int id);

    /**
     * Attributes the store asynchronously.
     * This method is optionally used to load asynchronously heavy data such as chats while the socket is connecting.
     * If implemented, cache the returning result because the method may be called multiple times.
     *
     * @param store the non-null store to attribute
     * @return a completable result
     */
    default CompletableFuture<Void> attributeStore(@SuppressWarnings("unused") Store store){
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns whether this serializer should be favoured when compared to other serializers.
     * If more than one serializer with this flag exists and is loaded, an error will be thrown.
     *
     * @return the boolean
     */
    default boolean isBest(){
        return true;
    }
}
