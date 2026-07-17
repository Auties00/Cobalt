package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PinnedChatsPremiumStatusType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPinnedChatsWamEvent")
@WamEvent(id = 7630)
public interface PinnedChatsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong pinnedChatNumber();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PinnedChatsPremiumStatusType> pinnedChatsPremiumStatus();
}
