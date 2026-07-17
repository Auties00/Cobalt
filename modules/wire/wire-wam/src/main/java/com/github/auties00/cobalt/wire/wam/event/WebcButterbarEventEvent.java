package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcButterbarActionType;
import com.github.auties00.cobalt.wire.wam.type.WebcButterbarBbType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcButterbarEventWamEvent")
@WamEvent(id = 3932)
public interface WebcButterbarEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebcButterbarActionType> webcButterbarAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcButterbarBbType> webcButterbarType();
}
