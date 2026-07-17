package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AppLaunchDestinationType;
import com.github.auties00.cobalt.wire.wam.type.AppLaunchType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAppLaunchWamEvent")
@WamEvent(id = 1094)
public interface AppLaunchEvent extends WamEventSpec {
    @WamProperty(index = 22, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 23, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> appLaunchCpuT();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<AppLaunchDestinationType> appLaunchDestination();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> appLaunchMainPreT();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> appLaunchMainRunT();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> appLaunchT();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<AppLaunchType> appLaunchTypeT();

    @WamProperty(index = 17, type = WamType.TIMER)
    Optional<Instant> dbBgThreadReadsDurationT();

    @WamProperty(index = 18, type = WamType.TIMER)
    Optional<Instant> dbBgThreadWritesDurationT();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong dbMainThreadCount();

    @WamProperty(index = 19, type = WamType.TIMER)
    Optional<Instant> dbMainThreadReadsDurationT();

    @WamProperty(index = 20, type = WamType.TIMER)
    Optional<Instant> dbMainThreadWritesDurationT();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong dbReadsCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong dbWritesCount();

    @WamProperty(index = 12, type = WamType.BOOLEAN)
    Optional<Boolean> lowPowerModeEnabled();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> peripheralConnected();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong processIdentifier();

    @WamProperty(index = 24, type = WamType.INTEGER)
    OptionalLong traceIdInt();
}
