package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.util.ImmutableLinkedList;

import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

class DiscardingStoreSerializer implements WhatsappStoreSerializer {
    private static final DiscardingStoreSerializer SINGLETON = new DiscardingStoreSerializer();

    static WhatsappStoreSerializer instance() {
        return SINGLETON;
    }

    @Override
    public SequencedCollection<UUID> listIds(WhatsappClientType type) {
        return ImmutableLinkedList.empty();
    }

    @Override
    public SequencedCollection<Long> listPhoneNumbers(WhatsappClientType type) {
        return ImmutableLinkedList.empty();
    }

    @Override
    public Optional<StoreKeysPair> deserializeStoreKeysPair(UUID uuid, Long phoneNumber, WhatsappClientType clientType) {
        return Optional.empty();
    }

    @Override
    public void serializeKeys(Keys keys) {
  
    }

    @Override
    public void serializeStore(WhatsappStore store) {
        
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Keys> deserializeKeys(WhatsappClientType type, Long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public Optional<WhatsappStore> deserializeStore(WhatsappClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<WhatsappStore> deserializeStore(WhatsappClientType type, Long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public void deleteSession(WhatsappClientType type, UUID uuid) {

    }

    @Override
    public void finishDeserializeStore(WhatsappStore store) {

    }
}
