package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.StatusPosterContactType;
import com.github.auties00.cobalt.wire.wam.type.StatusReportInteraction;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebStatusReportingEventsWamEvent")
@WamEvent(id = 3920)
public interface StatusReportingEventsEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<StatusPosterContactType> statusPosterContactType();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<StatusReportInteraction> statusReportInteraction();
}
