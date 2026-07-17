package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ChatGatedReason;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebGatedChatOpenedWamEvent")
@WamEvent(id = 3150)
public interface GatedChatOpenedEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<ChatGatedReason> chatGatedReason();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> selfInitiated();
}
