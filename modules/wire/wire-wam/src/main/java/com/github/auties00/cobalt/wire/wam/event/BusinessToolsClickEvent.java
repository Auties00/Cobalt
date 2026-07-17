package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsEntryPointType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsItemType;
import com.github.auties00.cobalt.wire.wam.type.BusinessToolsLinkedAccountType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBusinessToolsClickWamEvent")
@WamEvent(id = 2218)
public interface BusinessToolsClickEvent extends WamEventSpec {
    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<BusinessToolsEntryPointType> businessToolsEntryPoint();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong businessToolsEntryPointPlacement();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<BusinessToolsItemType> businessToolsItem();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong businessToolsSequenceNumber();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> businessToolsSessionId();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<BusinessToolsLinkedAccountType> linkingTarget();
}
