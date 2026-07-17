package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcPwaActionType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcPwaEventWamEvent")
@WamEvent(id = 4116)
public interface WebcPwaEventEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcPwaActionType> webcPwaAction();
}
