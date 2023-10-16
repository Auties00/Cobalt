package it.auties.whatsapp.util;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.TextPreviewSetting;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.controller.ControllerSerializer;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.auth.UserAgent;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerHelper {
    public static Optional<StoreAndKeysPair> deserialize(UUID uuid, Long phoneNumber, String alias, ClientType clientType, ControllerSerializer serializer) {
        if (uuid != null) {
            var store = serializer.deserializeStore(clientType, uuid);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(serializer);
            serializer.attributeStore(store.get());
            var keys = serializer.deserializeKeys(clientType, uuid);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(serializer);
            return Optional.of(new StoreAndKeysPair(store.get(), keys.get()));
        }

        if (phoneNumber != null) {
            var store = serializer.deserializeStore(clientType, phoneNumber);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(serializer);
            serializer.attributeStore(store.get());
            var keys = serializer.deserializeKeys(clientType, phoneNumber);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(serializer);
            return Optional.of(new StoreAndKeysPair(store.get(), keys.get()));
        }

        if (alias != null) {
            var store = serializer.deserializeStore(clientType, alias);
            if(store.isEmpty()) {
                return Optional.empty();
            }

            store.get().setSerializer(serializer);
            serializer.attributeStore(store.get());
            var keys = serializer.deserializeKeys(clientType,  alias);
            if(keys.isEmpty()) {
                return Optional.empty();
            }

            keys.get().setSerializer(serializer);
            return Optional.of(new StoreAndKeysPair(store.get(), keys.get()));
        }

        return Optional.empty();
    }

    public static StoreAndKeysPair create(UUID uuid, Long phoneNumber, Collection<String> alias, ClientType clientType, ControllerSerializer serializer) {
        var parsedPhoneNumber = PhoneNumber.ofNullable(phoneNumber);
        var store = new Store(
                uuid,
                parsedPhoneNumber.orElse(null),
                serializer,
                clientType,
                alias,
                null,
                null,
                false,
                null,
                Specification.Whatsapp.DEFAULT_NAME,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new LinkedHashMap<>(),
                null,
                null,
                phoneNumber != null ? Jid.of(phoneNumber) : null,
                null,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                false,
                false,
                Clock.nowSeconds(),
                ChatEphemeralTimer.OFF,
                TextPreviewSetting.ENABLED_WITH_INFERENCE,
                WebHistoryLength.standard(),
                true,
                true,
                UserAgent.ReleaseChannel.RELEASE,
                Specification.Whatsapp.DEFAULT_MOBILE_DEVICE,
                null,
                false
        );
        serializer.linkMetadata(store);
        var registrationId = KeyHelper.registrationId();
        var identityKeyPair = SignalKeyPair.random();
        var keys = new Keys(
                uuid,
                parsedPhoneNumber.orElse(null),
                serializer,
                clientType,
                alias,
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
        serializer.serializeKeys(keys, true);
        return new StoreAndKeysPair(store, keys);
    }

    public record StoreAndKeysPair(Store store, Keys keys) {

    }
}
