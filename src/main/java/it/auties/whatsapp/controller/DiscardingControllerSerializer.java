package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.ImmutableLinkedList;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;

class DiscardingControllerSerializer implements ControllerSerializer {
    private static final DiscardingControllerSerializer SINGLETON = new DiscardingControllerSerializer();

    static ControllerSerializer instance() {
        return SINGLETON;
    }

    @Override
    public LinkedList<UUID> listIds(WhatsappClientType type) {
        return ImmutableLinkedList.empty();
    }

    @Override
    public LinkedList<PhoneNumber> listPhoneNumbers(WhatsappClientType type) {
        return ImmutableLinkedList.empty();
    }

    @Override
    public Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, PhoneNumber phoneNumber, String alias, WhatsappClientType clientType) {
        return Optional.empty();
    }

    @Override
    public void serializeKeys(Keys keys) {
  
    }

    @Override
    public void serializeStore(Store store) {
        
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, PhoneNumber phoneNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, String alias) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, PhoneNumber phoneNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<Store> deserializeStore(WhatsappClientType type, String alias) {
        return Optional.empty();
    }

    @Override
    public void deleteSession(Controller controller) {

    }

    @Override
    public void finishDeserializeStore(Store store) {

    }
}
