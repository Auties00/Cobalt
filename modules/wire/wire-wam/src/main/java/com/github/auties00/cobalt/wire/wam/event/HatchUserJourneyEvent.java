package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BotEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.HatchActionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebHatchUserJourneyWamEvent")
@WamEvent(id = 7806)
public interface HatchUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> aiSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BotEntryPointType> botEntryPoint();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<HatchActionType> hatchActionType();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
