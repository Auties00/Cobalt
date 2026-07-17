package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcMediaAnalyzedWamEvent")
@WamEvent(id = 912)
public interface WebcMediaAnalyzedEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> webcMediaAnalyzeT();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> webcMediaExtensions();

    @WamProperty(index = 1, type = WamType.BOOLEAN)
    Optional<Boolean> webcMediaSupported();
}
