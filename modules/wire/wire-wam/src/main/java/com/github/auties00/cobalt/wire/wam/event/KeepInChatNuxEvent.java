package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.KicNuxActionNameType;
import com.github.auties00.cobalt.wire.wam.type.TriggerType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebKeepInChatNuxWamEvent")
@WamEvent(id = 3486)
public interface KeepInChatNuxEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<KicNuxActionNameType> kicNuxActionName();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<TriggerType> trigger();
}
