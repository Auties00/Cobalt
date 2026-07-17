package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebUserActivityWamEvent")
@WamEvent(id = 1384)
public interface UserActivityEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong userActivityBitmapHigh();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong userActivityBitmapLen();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong userActivityBitmapLow();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong userActivitySessionCum();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> userActivitySessionId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong userActivitySessionSeq();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong userActivityStartTime();
}
