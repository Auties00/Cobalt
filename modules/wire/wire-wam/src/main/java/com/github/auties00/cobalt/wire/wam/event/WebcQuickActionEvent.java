package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcQuickActionEventType;
import com.github.auties00.cobalt.wire.wam.type.WebcQuickActionId;
import com.github.auties00.cobalt.wire.wam.type.WebcQuickActionSurface;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcQuickActionWamEvent")
@WamEvent(id = 8326)
public interface WebcQuickActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebcQuickActionEventType> webcQuickActionEventType();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcQuickActionId> webcQuickActionId();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> webcQuickActionIsCustomized();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong webcQuickActionNumVisible();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong webcQuickActionSlotPosition();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<WebcQuickActionSurface> webcQuickActionSurface();
}
