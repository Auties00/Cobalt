package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcProgressiveImageWamEvent")
@WamEvent(id = 2226, releaseWeight = 10)
public interface WebcProgressiveImageEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong webcFirstRenderScans();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> webcFirstRenderT();

    @WamProperty(index = 4, type = WamType.TIMER)
    Optional<Instant> webcFullQualityT();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> webcMidQualityT();
}
