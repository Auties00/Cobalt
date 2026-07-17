package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcMediaLoadResultCode;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcMediaLoadWamEvent")
@WamEvent(id = 1202)
public interface WebcMediaLoadEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcMediaLoadResultCode> webcMediaLoadResult();

    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> webcMediaLoadT();
}
