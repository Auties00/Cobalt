package it.auties.whatsapp.controller;

import java.util.Objects;

/**
 * A pair of Store and Keys with the same uuid
 */
public record StoreKeysPair(Store store, Keys keys) {
    public StoreKeysPair {
        if (!Objects.equals(store.uuid(), keys.uuid())) {
            throw new IllegalArgumentException("UUID mismatch between store and keys");
        }
    }
}
