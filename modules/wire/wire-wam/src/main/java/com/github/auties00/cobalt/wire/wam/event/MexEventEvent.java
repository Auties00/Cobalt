package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMexEventWamEvent")
@WamEvent(id = 3782)
public interface MexEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> isMex();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> mexEventData();

    @WamProperty(index = 12, type = WamType.TIMER)
    Optional<Instant> mexEventDurationT();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong mexEventEndTime();

    @WamProperty(index = 13, type = WamType.INTEGER)
    OptionalLong mexEventEnvelopeResponseStatus();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> mexEventOperation();

    @WamProperty(index = 14, type = WamType.INTEGER)
    OptionalLong mexEventPayloadResponseStatus();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mexEventRequestSize();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong mexEventResponseSize();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong mexEventRetries();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong mexEventStartTime();
}
