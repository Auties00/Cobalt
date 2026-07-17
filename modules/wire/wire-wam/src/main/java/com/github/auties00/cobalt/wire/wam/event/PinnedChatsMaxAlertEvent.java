package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PremiumStatusType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebPinnedChatsMaxAlertWamEvent")
@WamEvent(id = 7606)
public interface PinnedChatsMaxAlertEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> addToListSelected();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PremiumStatusType> premiumStatus();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> subscribeSelected();
}
