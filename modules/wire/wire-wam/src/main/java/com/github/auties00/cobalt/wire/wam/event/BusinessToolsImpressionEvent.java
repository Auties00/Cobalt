package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsEntryPointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBusinessToolsImpressionWamEvent")
@WamEvent(id = 2220)
public interface BusinessToolsImpressionEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<BusinessToolsEntryPointType> businessToolsEntryPoint();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong businessToolsSequenceNumber();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessToolsSessionId();
}
