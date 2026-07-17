package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.DefenseModeQuarantineAction;
import com.github.auties00.cobalt.wire.wam.type.JidDomainType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebDefenseModeQuarantineWamEvent")
@WamEvent(id = 7098)
public interface DefenseModeQuarantineEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong defenseModeQuarantineEventCount();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> defenseModeQuarantineIsCapi();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<JidDomainType> jidDomain();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<DefenseModeQuarantineAction> quarantineAction();
}
