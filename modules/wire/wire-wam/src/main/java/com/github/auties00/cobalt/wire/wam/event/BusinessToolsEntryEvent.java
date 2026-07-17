package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsEntryPointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBusinessToolsEntryWamEvent")
@WamEvent(id = 2216)
public interface BusinessToolsEntryEvent extends WamEventSpec {
    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<BusinessToolsEntryPointType> businessToolsEntryPoint();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong businessToolsSequenceNumber();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessToolsSessionId();
}
