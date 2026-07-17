package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcMenuItemLabel;
import com.github.auties00.cobalt.wire.wam.type.WebcMenuType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebWebcMenuWamEvent")
@WamEvent(id = 2504)
public interface WebcMenuEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcMenuType> webcMenuAction();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<WebcMenuItemLabel> webcMenuItemLabel();
}
