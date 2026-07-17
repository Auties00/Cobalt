package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CallStanzaType;
import com.github.auties00.cobalt.wire.wam.type.E2eDeviceType;
import com.github.auties00.cobalt.wire.wam.type.EncryptionTypeCode;
import com.github.auties00.cobalt.wire.wam.type.InvisibleMessageCategoryType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;
import com.github.auties00.cobalt.wire.wam.type.MessageType;
import com.github.auties00.cobalt.wire.wam.type.StanzaType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebOfflineCountTooHighWamEvent")
@WamEvent(id = 2638)
public interface OfflineCountTooHighEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<CallStanzaType> callStanzaType();

    @WamProperty(index = 10, type = WamType.ENUM)
    Optional<E2eDeviceType> e2eSenderType();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<EncryptionTypeCode> encryptionType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<InvisibleMessageCategoryType> invisibleMessageCategory();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MessageType> messageType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> notificationStanzaType();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong offlineCount();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> receiptStanzaType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<StanzaType> stanzaType();
}
