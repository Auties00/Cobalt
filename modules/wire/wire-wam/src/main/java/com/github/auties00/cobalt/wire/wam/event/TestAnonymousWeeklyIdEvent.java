package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebTestAnonymousWeeklyIdWamEvent")
@WamEvent(id = 2956, channel = WamChannel.PRIVATE, privateStatsId = 42196056)
public interface TestAnonymousWeeklyIdEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> psTestBooleanField();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> psTestStringField();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong psTimeSinceLastEventInMin();
}
