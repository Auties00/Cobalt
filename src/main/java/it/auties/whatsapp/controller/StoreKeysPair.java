package it.auties.whatsapp.controller;

import it.auties.whatsapp.util.Validate;

import java.util.Objects;

/**
 * A pair of Store and Keys with the same uuid
 */
public record StoreKeysPair(Store store, Keys keys) {
    public StoreKeysPair {
        Validate.isTrue(Objects.equals(store.uuid(), keys.uuid()), "UUID mismatch between store and keys");
    }
}
