package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CadminDemoteOriginType;
import com.github.auties00.cobalt.wire.wam.type.CadminDemoteResultType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebCadminDemoteWamEvent")
@WamEvent(id = 3426)
public interface CadminDemoteEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CadminDemoteOriginType> cadminDemoteOrigin();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CadminDemoteResultType> cadminDemoteResult();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> isLastCadminOrCreator();
}
