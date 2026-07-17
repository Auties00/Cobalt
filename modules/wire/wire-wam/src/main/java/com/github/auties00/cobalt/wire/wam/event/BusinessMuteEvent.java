package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebBusinessMuteWamEvent")
@WamEvent(id = 1376)
public interface BusinessMuteEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> muteT();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> muteeId();
}
