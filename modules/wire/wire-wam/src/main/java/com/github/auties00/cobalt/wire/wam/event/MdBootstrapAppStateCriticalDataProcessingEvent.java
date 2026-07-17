package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BootstrapAppStateDataStageCode;
import com.github.auties00.cobalt.wire.wam.type.MdBootstrapPayloadType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdBootstrapAppStateCriticalDataProcessingWamEvent")
@WamEvent(id = 3164)
public interface MdBootstrapAppStateCriticalDataProcessingEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BootstrapAppStateDataStageCode> bootstrapAppStateDataStage();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<MdBootstrapPayloadType> mdBootstrapPayloadType();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> mdRegAttemptId();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> mdSessionId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong mdTimestamp();
}
