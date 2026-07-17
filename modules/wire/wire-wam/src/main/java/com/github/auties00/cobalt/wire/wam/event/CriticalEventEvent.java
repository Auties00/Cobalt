package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CrashApplicationState;
import com.github.auties00.cobalt.wire.wam.type.CrashlogType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCriticalEventWamEvent")
@WamEvent(id = 1684)
public interface CriticalEventEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> context();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<CrashApplicationState> crashApplicationState();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<CrashlogType> crashlogType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> debug();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> extraDebug();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> mobileBuildId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> name();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong storageAvailSizeWithCache();
}
