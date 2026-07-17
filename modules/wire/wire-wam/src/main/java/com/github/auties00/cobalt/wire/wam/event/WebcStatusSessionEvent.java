package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcStatusSessionWamEvent")
@WamEvent(id = 1880)
public interface WebcStatusSessionEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong webcStatusMutedItemCount();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong webcStatusMutedRowCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong webcStatusRecentItemCount();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong webcStatusRecentRowCount();

    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong webcStatusSessionId();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong webcStatusViewedItemCount();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong webcStatusViewedRowCount();
}
