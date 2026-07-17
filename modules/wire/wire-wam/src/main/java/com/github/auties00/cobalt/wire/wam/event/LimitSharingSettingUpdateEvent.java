package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.OpusAction;
import com.github.auties00.cobalt.wire.wam.type.ToggleUpdateAction;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebLimitSharingSettingUpdateWamEvent")
@WamEvent(id = 6390)
public interface LimitSharingSettingUpdateEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<OpusAction> opusAction();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> threadId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ToggleUpdateAction> toggleUpdateAction();
}
