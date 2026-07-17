package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMexEventV2WamEvent")
@WamEvent(id = 4336)
public interface MexEventV2Event extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> mexEventV2DurationMs();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong mexEventV2EndTime();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> mexEventV2ErrorCodes();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> mexEventV2Errors();

    @WamProperty(index = 12, type = WamType.INTEGER)
    OptionalLong mexEventV2ExperimentFlag();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> mexEventV2HasData();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> mexEventV2IsArgoPayload();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> mexEventV2IsMex();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> mexEventV2OperationName();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> mexEventV2QueryId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong mexEventV2StartTime();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> mexFbUserType();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong traceIdInt();
}
