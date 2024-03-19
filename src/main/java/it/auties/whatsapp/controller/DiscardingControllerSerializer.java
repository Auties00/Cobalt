package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
class DiscardingControllerSerializer implements ControllerSerializer {
    private static final DiscardingControllerSerializer SINGLETON = new DiscardingControllerSerializer();
    private static final LinkedList<UUID> EMPTY_IDS = new LinkedList<>();
    private static final LinkedList<PhoneNumber> EMPTY_PHONE_NUMBERS = new LinkedList<>();

    public static ControllerSerializer singleton() {
        return SINGLETON;
    }

    @Override
    public LinkedList<UUID> listIds(ClientType type) {
        return EMPTY_IDS;
    }

    @Override
    public LinkedList<PhoneNumber> listPhoneNumbers(ClientType type) {
        return EMPTY_PHONE_NUMBERS;
    }

    @Override
    public Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, Long phoneNumber, String alias, ClientType clientType) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Void> serializeKeys(Keys keys, boolean async) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> serializeStore(Store store, boolean async) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Optional<Keys> deserializeKeys(ClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Keys> deserializeKeys(ClientType type, long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<Keys> deserializeKeys(ClientType type, String alias) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(ClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(ClientType type, long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(ClientType type, String alias) {
        return Optional.empty();
    }

    @Override
    public void deleteSession(Controller<?> controller) {

    }
}
