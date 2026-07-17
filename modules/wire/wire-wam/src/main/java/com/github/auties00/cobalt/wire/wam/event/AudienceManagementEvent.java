package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AudienceEventSurfaceType;
import com.github.auties00.cobalt.wire.wam.type.AudienceManagementActionType;
import com.github.auties00.cobalt.wire.wam.type.AudiencePredicateTypeEnum;
import com.github.auties00.cobalt.wire.wam.type.AudienceResolutionTriggerType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebAudienceManagementWamEvent")
@WamEvent(id = 7900)
public interface AudienceManagementEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<AudienceEventSurfaceType> audienceEventSurface();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> audienceExtraData();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<AudienceManagementActionType> audienceManagementAction();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<AudiencePredicateTypeEnum> audiencePredicateType();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<AudienceResolutionTriggerType> audienceResolutionTrigger();
}
