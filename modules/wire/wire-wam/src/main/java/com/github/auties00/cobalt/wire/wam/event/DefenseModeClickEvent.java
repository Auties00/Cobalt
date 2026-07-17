package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeClickControlName;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeClickDesiredState;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebDefenseModeClickWamEvent")
@WamEvent(id = 7096)
public interface DefenseModeClickEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DefenseModeClickControlName> controlName();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> defenseModeClickAccepted();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<DefenseModeClickDesiredState> desiredState();
}
