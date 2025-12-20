package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.client.WhatsAppClientType;

import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

final class DiscardingStoreSerializer implements WhatsappStoreSerializer {
    static final DiscardingStoreSerializer INSTANCE = new DiscardingStoreSerializer();

    private DiscardingStoreSerializer() {

    }

    @Override
    public SequencedCollection<UUID> listIds(WhatsAppClientType type) {
        return List.of();
    }

    @Override
    public SequencedCollection<Long> listPhoneNumbers(WhatsAppClientType type) {
        return List.of();
    }

    @Override
    public void serialize(WhatsAppStore store) {

    }

    @Override
    public Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<WhatsAppStore> startDeserialize(WhatsAppClientType type, Long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public void deleteSession(WhatsAppClientType type, UUID uuid) {

    }

    @Override
    public void finishDeserialize(WhatsAppStore store) {

    }
}
