package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWamDroppedEventWamEvent")
@WamEvent(id = 4358)
public interface WamDroppedEventEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong droppedEventCode();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong droppedEventCount();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWamsys();
}
