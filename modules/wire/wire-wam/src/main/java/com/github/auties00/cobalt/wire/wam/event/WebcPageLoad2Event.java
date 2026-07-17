package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcPageLoad2WamEvent")
@WamEvent(id = 5392)
public interface WebcPageLoad2Event extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> webcPageLoadId();
}
