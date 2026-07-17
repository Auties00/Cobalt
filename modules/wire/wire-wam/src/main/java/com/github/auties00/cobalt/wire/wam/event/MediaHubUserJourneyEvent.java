package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.ActionCode;
import com.github.auties00.cobalt.wire.wam.type.EntryPointType;
import com.github.auties00.cobalt.wire.wam.type.SurfaceCode;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMediaHubUserJourneyWamEvent")
@WamEvent(id = 7090)
public interface MediaHubUserJourneyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> customFields();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<ActionCode> mediaHubAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<EntryPointType> mediaHubEntryPoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong mediaHubSequenceNumber();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> mediaHubSessionId();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<SurfaceCode> mediaHubSurface();

    @WamProperty(index = 7, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
