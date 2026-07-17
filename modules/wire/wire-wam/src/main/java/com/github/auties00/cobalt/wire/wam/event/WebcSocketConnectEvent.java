package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcSocketConnectReasonType;

import java.time.Instant;
import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcSocketConnectWamEvent")
@WamEvent(id = 5450)
public interface WebcSocketConnectEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.TIMER)
    Optional<Instant> webcAuthHandshakeDuration();

    @WamProperty(index = 2, type = WamType.TIMER)
    Optional<Instant> webcSocketConnectDuration();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WebcSocketConnectReasonType> webcSocketConnectReason();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> webcSocketHostname();
}
