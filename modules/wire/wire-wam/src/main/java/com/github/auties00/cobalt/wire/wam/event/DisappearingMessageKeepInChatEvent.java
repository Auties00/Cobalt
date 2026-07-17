package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.KicActionNameType;
import com.github.auties00.cobalt.wire.wam.type.KicActorType;
import com.github.auties00.cobalt.wire.wam.type.KicEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.MediaType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDisappearingMessageKeepInChatWamEvent")
@WamEvent(id = 3482)
public interface DisappearingMessageKeepInChatEvent extends WamEventSpec {
    @WamProperty(index = 16, type = WamType.BOOLEAN)
    Optional<Boolean> canEditDmSettings();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> isAGroup();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isAdmin();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong keptCount();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong keptDelta();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<KicActionNameType> kicActionName();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<KicActorType> kicActor();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<KicEntryPointType> kicEntryPoint();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<MediaType> mediaType();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> messageExpiredOnUnkeep();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong messageExpiryTimer();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong messagesInFolder();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong messagesSelected();

    @WamProperty(index = 13, type = WamType.STRING)
    Optional<String> threadId();
}
