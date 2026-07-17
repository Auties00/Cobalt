package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcResourceLoadWamEvent")
@WamEvent(id = 688, betaWeight = 1000, releaseWeight = 2000)
public interface WebcResourceLoadEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> webcResourceCached();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> webcResourceDuration();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> webcResourceName();
}
