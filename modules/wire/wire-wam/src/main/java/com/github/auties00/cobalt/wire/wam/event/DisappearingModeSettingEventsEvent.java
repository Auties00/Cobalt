package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingModeEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.DisappearingModeSettingEventNameType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDisappearingModeSettingEventsWamEvent")
@WamEvent(id = 3446)
public interface DisappearingModeSettingEventsEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<DisappearingModeEntryPointType> disappearingModeEntryPoint();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DisappearingModeSettingEventNameType> disappearingModeSettingEventName();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isAfterRead();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong lastToggleTimestamp();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong newEphemeralityDuration();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong previousEphemeralityDuration();
}
