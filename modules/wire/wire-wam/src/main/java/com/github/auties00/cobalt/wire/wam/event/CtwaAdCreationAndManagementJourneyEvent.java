package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CtwaAdAccountType;
import com.github.auties00.cobalt.wire.wam.type.LwiActionType;
import com.github.auties00.cobalt.wire.wam.type.LwiEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.LwiSubEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.LwiSurface;
import com.github.auties00.cobalt.wire.wam.type.TargetComponent;
import com.github.auties00.cobalt.wire.wam.type.WaAdAccountEligibility;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCtwaAdCreationAndManagementJourneyWamEvent")
@WamEvent(id = 6562)
public interface CtwaAdCreationAndManagementJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CtwaAdAccountType> ctwaAdAccountType();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<LwiActionType> lwiActionType();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<LwiEntryPoint> lwiEntryPoint();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> lwiFlowId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<LwiSubEntryPoint> lwiSubEntryPoint();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<LwiSurface> lwiSurface();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong seqId();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<TargetComponent> targetComponent();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> unifiedSessionId();

    @WamProperty(index = 11, type = WamType.ENUM)
    Optional<WaAdAccountEligibility> waAdAccountEligibility();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> waAdAccountId();
}
