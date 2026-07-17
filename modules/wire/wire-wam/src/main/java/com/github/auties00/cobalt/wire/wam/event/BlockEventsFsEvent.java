package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BlockEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.BlockEventActionType;
import com.github.auties00.cobalt.wire.wam.type.CallResultType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebBlockEventsFsWamEvent")
@WamEvent(id = 4288)
public interface BlockEventsFsEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<BlockEntryPoint> blockEntryPoint();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<BlockEventActionType> blockEventActionType();

    @WamProperty(index = 3, type = WamType.BOOLEAN)
    Optional<Boolean> blockEventIsSuspicious();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> blockEventIsUnsub();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> pastCall();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<CallResultType> pastCallResult();
}
