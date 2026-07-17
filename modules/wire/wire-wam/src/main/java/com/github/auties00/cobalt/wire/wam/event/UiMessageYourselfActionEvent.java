package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.UiMessageYourselfActionType;
import com.github.auties00.cobalt.wire.wam.type.UiMessageYourselfFunnelName;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebUiMessageYourselfActionWamEvent")
@WamEvent(id = 3780)
public interface UiMessageYourselfActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> uiMessageYourselfActionSessionId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<UiMessageYourselfActionType> uiMessageYourselfActionType();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<UiMessageYourselfFunnelName> uiMessageYourselfFunnelName();
}
