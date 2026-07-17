package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.EphemeralSettingEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.PreciseSizeBucket;
import com.github.auties00.cobalt.wire.wam.type.PreviousEphemeralityType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebEphemeralSettingChangeWamEvent")
@WamEvent(id = 2370)
public interface EphemeralSettingChangeEvent extends WamEventSpec {
    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong afterReadDuration();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong chatEphemeralityDuration();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EphemeralSettingEntryPointType> ephemeralSettingEntryPoint();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PreciseSizeBucket> ephemeralSettingGroupSize();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong errorCode();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isAfterRead();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> isSuccess();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong previousEphemeralityDuration();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<PreviousEphemeralityType> previousEphemeralityType();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> threadId();
}
