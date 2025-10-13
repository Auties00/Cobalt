package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.api.WhatsappClientType;

import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;

/**
 * Private implementation for a serializer that discards all data.
 */
final class DiscardingStoreSerializer implements WhatsappStoreSerializer {
    static final DiscardingStoreSerializer INSTANCE = new DiscardingStoreSerializer();

    private DiscardingStoreSerializer() {

    }

    @Override
    public SequencedCollection<UUID> listIds(WhatsappClientType type) {
        return List.of();
    }

    @Override
    public SequencedCollection<Long> listPhoneNumbers(WhatsappClientType type) {
        return List.of();
    }

    @Override
    public void serialize(WhatsappStore store) {

    }

    @Override
    public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<WhatsappStore> startDeserialize(WhatsappClientType type, Long phoneNumber) {
        return Optional.empty();
    }

    @Override
    public void deleteSession(WhatsappClientType type, UUID uuid) {

    }

    @Override
    public void finishDeserialize(WhatsappStore store) {

    }
}
