package it.auties.whatsapp.controller;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.api.TextPreviewSetting;
import it.auties.whatsapp.api.WebHistoryLength;
import it.auties.whatsapp.model.business.BusinessCategory;
import it.auties.whatsapp.model.chat.ChatEphemeralTimer;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.auth.UserAgent.Platform;
import it.auties.whatsapp.model.signal.auth.UserAgent.ReleaseChannel;
import it.auties.whatsapp.model.signal.auth.Version;
import it.auties.whatsapp.util.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class StoreBuilder {
    private UUID uuid;
    private PhoneNumber phoneNumber;
    private String alias;
    private ClientType clientType;
    private CompanionDevice device;
    private ControllerSerializer serializer;
    private URI proxy;
    private String name;
    private boolean business;
    private BusinessCategory businessCategory;
    private TextPreviewSetting textPreviewSetting;
    private WebHistoryLength historyLength;
    private boolean autodetectListeners;
    private boolean automaticPresenceUpdates;
    private ReleaseChannel releaseChannel;
    private boolean checkPatchMacs;
    private String businessAddress;
    private Double businessLongitude;
    private Double businessLatitude;
    private String businessDescription;
    private String businessWebsite;
    private String businessEmail;
    private Version version;

    StoreBuilder() {

    }

    public StoreBuilder uuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public StoreBuilder clientType(ClientType clientType) {
        this.clientType = clientType;
        return this;
    }

    public StoreBuilder device(CompanionDevice device) {
        this.device = device;
        return this;
    }

    public StoreBuilder phoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public StoreBuilder serializer(ControllerSerializer serializer) {
        this.serializer = serializer;
        return this;
    }

    public StoreBuilder alias(String alias) {
        this.alias = alias;
        return this;
    }

    public StoreBuilder proxy(URI proxy) {
        this.proxy = proxy;
        return this;
    }

    public StoreBuilder name(String name) {
        this.name = name;
        return this;
    }

    public StoreBuilder business(boolean business) {
        this.business = business;
        return this;
    }

    public StoreBuilder businessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
        return this;
    }

    public StoreBuilder textPreviewSetting(TextPreviewSetting textPreviewSetting) {
        this.textPreviewSetting = textPreviewSetting;
        return this;
    }

    public StoreBuilder historyLength(WebHistoryLength historyLength) {
        this.historyLength = historyLength;
        return this;
    }

    public StoreBuilder autodetectListeners(boolean autodetectListeners) {
        this.autodetectListeners = autodetectListeners;
        return this;
    }

    public StoreBuilder automaticPresenceUpdates(boolean automaticPresenceUpdates) {
        this.automaticPresenceUpdates = automaticPresenceUpdates;
        return this;
    }

    public StoreBuilder releaseChannel(ReleaseChannel releaseChannel) {
        this.releaseChannel = releaseChannel;
        return this;
    }

    public StoreBuilder checkPatchMacs(boolean checkPatchMacs) {
        this.checkPatchMacs = checkPatchMacs;
        return this;
    }

    public StoreBuilder businessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
        return this;
    }

    public StoreBuilder businessLongitude(Double businessLongitude) {
        this.businessLongitude = businessLongitude;
        return this;
    }

    public StoreBuilder businessLatitude(Double businessLatitude) {
        this.businessLatitude = businessLatitude;
        return this;
    }

    public StoreBuilder businessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
        return this;
    }

    public StoreBuilder businessWebsite(String businessWebsite) {
        this.businessWebsite = businessWebsite;
        return this;
    }

    public StoreBuilder businessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
        return this;
    }

    public StoreBuilder version(Version version) {
        this.version = version;
        return this;
    }

    public Optional<Store> deserialize() {
        var serializer = Objects.requireNonNullElseGet(this.serializer, DefaultControllerSerializer::instance);
        var clientType = Objects.requireNonNull(this.clientType, "Client type is required");
        if(uuid != null) {
            var store = serializer.deserializeStore(clientType, uuid);
            if (store.isPresent()) {
                store.get().setSerializer(serializer);
                serializer.attributeStore(store.get());
                return store;
            }
        }


        if(phoneNumber != null) {
            var store = serializer.deserializeStore(clientType, phoneNumber.number());
            if(store.isPresent()) {
                store.get().setSerializer(serializer);
                serializer.attributeStore(store.get());
                return store;
            }
        }

        if(alias != null) {
            var store = serializer.deserializeStore(clientType, alias);
            if(store.isPresent()) {
                store.get().setSerializer(serializer);
                serializer.attributeStore(store.get());
                return store;
            }
        }

        return Optional.empty();
    }

    public Store build() {
        return deserialize().orElseGet(() -> {
            if(device == null) {
                device = Spec.Whatsapp.DEFAULT_MOBILE_DEVICE;
            }

            var serializer = Objects.requireNonNullElseGet(this.serializer, DefaultControllerSerializer::instance);
            return new Store(
                    Objects.requireNonNull(uuid, "Uuid is required if the StoreBuilder can't find a serialized session"),
                    phoneNumber,
                    serializer,
                    clientType,
                    alias != null ? List.of(alias) : null,
                    proxy,
                    new FutureReference<>(version, () -> MetadataHelper.getVersion(getPlatform(clientType))),
                    false,
                    null,
                    Objects.requireNonNullElse(name, Spec.Whatsapp.DEFAULT_NAME),
                    business,
                    businessAddress,
                    businessLongitude,
                    businessLatitude,
                    businessDescription,
                    businessWebsite,
                    businessEmail,
                    businessCategory,
                    null,
                    new LinkedHashMap<>(),
                    null,
                    null,
                    phoneNumber != null ? phoneNumber.toJid() : null,
                    null,
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    false,
                    false,
                    new ConcurrentHashMap<>(),
                    new ConcurrentHashMap<>(),
                    ConcurrentHashMap.newKeySet(),
                    HexFormat.of().formatHex(BytesHelper.random(1)),
                    Clock.nowSeconds(),
                    null,
                    new CountDownLatch(1),
                    ChatEphemeralTimer.OFF,
                    Objects.requireNonNullElse(textPreviewSetting, TextPreviewSetting.ENABLED_WITH_INFERENCE),
                    Objects.requireNonNullElse(historyLength, WebHistoryLength.STANDARD),
                    autodetectListeners,
                    automaticPresenceUpdates,
                    Objects.requireNonNullElse(releaseChannel, ReleaseChannel.RELEASE),
                    device,
                    null,
                    checkPatchMacs
            );
        });
    }

    private Platform getPlatform(ClientType clientType) {
        return switch (clientType) {
            case WEB -> Platform.WEB;
            case MOBILE -> business ? device.businessPlatform() : device.platform();
        };
    }
}