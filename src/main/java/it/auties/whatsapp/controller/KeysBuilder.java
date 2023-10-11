package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.util.KeyHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeysBuilder {
    private UUID uuid;
    private PhoneNumber phoneNumber;
    private String alias;
    private ClientType clientType;
    private ControllerSerializer serializer;

    KeysBuilder() {

    }

    public KeysBuilder uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public KeysBuilder phoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public KeysBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    public KeysBuilder clientType(ClientType clientType) {
        this.clientType = clientType;
        return this;
    }

    public KeysBuilder serializer(ControllerSerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    public Optional<Keys> deserialize() {
        var safeSerializer = Objects.requireNonNullElseGet(serializer, DefaultControllerSerializer::instance);
        var clientType = Objects.requireNonNull(this.clientType, "Client type is required");
        if (uuid != null) {
            var keys = safeSerializer.deserializeKeys(clientType, uuid);
            if (keys.isPresent()) {
                keys.get().setSerializer(safeSerializer);
                return keys;
            }
        }

        if (phoneNumber != null) {
            var keys = safeSerializer.deserializeKeys(clientType, phoneNumber.number());
            if (keys.isPresent()) {
                keys.get().setSerializer(safeSerializer);
                return keys;
            }
        }

        if (alias != null) {
            var keys = safeSerializer.deserializeKeys(clientType, alias);
            if (keys.isPresent()) {
                keys.get().setSerializer(safeSerializer);
                return keys;
            }
        }

        return Optional.empty();
    }

    public Keys build() {
        return deserialize().orElseGet(() -> {
            var safeSerializer = Objects.requireNonNullElseGet(serializer, DefaultControllerSerializer::instance);
            var registrationId = KeyHelper.registrationId();
            var identityKeyPair = SignalKeyPair.random();
            var result = new Keys(
                    uuid,
                    phoneNumber,
                    safeSerializer,
                    clientType,
                    alias != null ? List.of(alias) : null,
                    registrationId,
                    SignalKeyPair.random(),
                    SignalKeyPair.random(),
                    identityKeyPair,
                    SignalKeyPair.random(),
                    SignalSignedKeyPair.of(registrationId, identityKeyPair),
                    null,
                    null,
                    new ArrayList<>(),
                    KeyHelper.phoneId(),
                    KeyHelper.deviceId(),
                    KeyHelper.identityId(),
                    null,
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    false,
                    false,
                    false
            );
            safeSerializer.serializeKeys(result, true);
            return result;
        });
    }
}