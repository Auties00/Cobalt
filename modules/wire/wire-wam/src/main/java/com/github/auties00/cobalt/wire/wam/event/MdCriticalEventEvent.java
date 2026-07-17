package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.Collection;
import com.github.auties00.cobalt.wire.wam.type.MdSyncdCriticalEventCode;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebMdCriticalEventWamEvent")
@WamEvent(id = 2746)
public interface MdCriticalEventEvent extends WamEventSpec {
    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<Collection> collection();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<MdSyncdCriticalEventCode> mdCriticalEventCode();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> mdCriticalEventErrorMessage();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> mdCriticalEventStage();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> mutationActionName();
}
