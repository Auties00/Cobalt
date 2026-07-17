package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GroupCreateEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebGroupCreateWamEvent")
@WamEvent(id = 594)
public interface GroupCreateEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong ephemeralityDuration();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GroupCreateEntryPoint> groupCreateEntryPoint();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> hasGroupName();
}
