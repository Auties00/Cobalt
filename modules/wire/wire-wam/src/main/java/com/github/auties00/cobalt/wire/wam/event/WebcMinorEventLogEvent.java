package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LogType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcMinorEventLogWamEvent")
@WamEvent(id = 5440, releaseWeight = 100)
public interface WebcMinorEventLogEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> additionalDebugContext();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> logContext();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong logCount();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> logReason();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<LogType> logType();
}
