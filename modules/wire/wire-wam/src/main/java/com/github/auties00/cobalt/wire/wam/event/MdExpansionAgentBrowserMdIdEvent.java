package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MultideviceActionType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMdExpansionAgentBrowserMdIdWamEvent")
@WamEvent(id = 3390)
public interface MdExpansionAgentBrowserMdIdEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> agentId();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> browserId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong companionMdId();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isCustomAgentName();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> isNewAgent();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong loginTimestamp();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong logoutTimestamp();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong mdLinkedCount();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<MultideviceActionType> multideviceAction();
}
