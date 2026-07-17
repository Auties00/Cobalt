package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatsFolderType;
import com.github.auties00.cobalt.wire.wam.type.ContactType;
import com.github.auties00.cobalt.wire.wam.type.EntryPoint;
import com.github.auties00.cobalt.wire.wam.type.QbmFlag;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebQbmRichOrderStatusInteractionWamEvent")
@WamEvent(id = 6940)
public interface QbmRichOrderStatusInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> actionTypeRichOrderStatus();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ChatsFolderType> chatsFolderType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ContactType> contactType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> decisionId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong deltaTime();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong deltaTimeReceived();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<EntryPoint> entryPoint();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> hsmTagStr();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isBizIntent();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isBroadcastMessage();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isInsubContact();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> isMuted();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> messageIdHmac();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<QbmFlag> qbmFlag();

    @WamProperty(index = 15, type = WamType.BOOLEAN)
    Optional<Boolean> readReceiptsEnabled();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> threadIdHmac();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
