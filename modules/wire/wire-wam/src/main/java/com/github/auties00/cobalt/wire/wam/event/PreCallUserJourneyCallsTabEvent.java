package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CallSizeType;
import com.github.auties00.cobalt.wire.wam.type.CallsTabSource;
import com.github.auties00.cobalt.wire.wam.type.PreCallActionType;
import com.github.auties00.cobalt.wire.wam.type.SubSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPreCallUserJourneyCallsTabWamEvent")
@WamEvent(id = 5680)
public interface PreCallUserJourneyCallsTabEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> appSessionId();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<CallSizeType> callSizeType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CallsTabSource> callsTabSource();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> genaiBots();

    @WamProperty(index = 10, type = WamType.INTEGER)
    OptionalLong itemPosition();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<PreCallActionType> preCallActionType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<SubSurface> subSurface();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> surfaceSessionId();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong userJourneyEventMs();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> userJourneyFunnelId();
}
