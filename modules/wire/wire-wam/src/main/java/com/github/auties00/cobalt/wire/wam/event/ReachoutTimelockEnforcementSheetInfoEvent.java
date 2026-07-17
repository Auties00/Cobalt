package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ReachoutTimelockAction;
import com.github.auties00.cobalt.wire.wam.type.ReachoutTimelockEventSource;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebReachoutTimelockEnforcementSheetInfoWamEvent")
@WamEvent(id = 5582)
public interface ReachoutTimelockEnforcementSheetInfoEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<ReachoutTimelockAction> reachoutTimelockAction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<ReachoutTimelockEventSource> reachoutTimelockEventSource();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> timeSinceEnforcemeentEndAndSheetSeenMs();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> wasSheetSeenForFirstTime();
}
