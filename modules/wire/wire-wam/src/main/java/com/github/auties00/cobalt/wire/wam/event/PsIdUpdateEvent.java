package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsIdUpdateWamEvent")
@WamEvent(id = 2862)
public interface PsIdUpdateEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isFromWamsys();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<PsIdAction> psIdAction();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong psIdKey();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong psIdRotationFrequence();
}
