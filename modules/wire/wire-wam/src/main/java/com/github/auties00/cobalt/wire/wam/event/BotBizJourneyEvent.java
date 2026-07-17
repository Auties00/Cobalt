package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BotBizActionType;
import com.github.auties00.cobalt.wire.wam.type.BotBizEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.BotBizType;
import com.github.auties00.cobalt.wire.wam.type.BotType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebBotBizJourneyWamEvent")
@WamEvent(id = 4868)
public interface BotBizJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<BotBizActionType> botBizActionType();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<BotBizEntryPoint> botBizEntryPoint();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<BotBizType> botBizType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<BotType> botType();
}
