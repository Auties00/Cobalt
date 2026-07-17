package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.LwiEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.LwiEntryPointImpressionAction;
import com.github.auties00.cobalt.wire.wam.type.LwiSubEntryPoint;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebLwiEntryPointImpressionWamEvent")
@WamEvent(id = 2906)
public interface LwiEntryPointImpressionEvent extends WamEventSpec {
    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong activeItemsCount();

    @WamProperty(index = 9, type = WamType.INTEGER)
    OptionalLong archivedItemsCount();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> businessToolsSessionId();

    @WamProperty(index = 1, type = WamType.STRING)
    Optional<String> catalogSessionId();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong itemsCount();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<LwiEntryPoint> lwiEntryPoint();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<LwiEntryPointImpressionAction> lwiEntryPointImpressionAction();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> lwiExtras();

    @WamProperty(index = 6, type = WamType.ENUM)
    Optional<LwiSubEntryPoint> lwiSubEntryPoint();

    @WamProperty(index = 11, type = WamType.STRING)
    Optional<String> statusId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong statusSessionId();

    @WamProperty(index = 4, type = WamType.BOOLEAN)
    Optional<Boolean> userHasLinkedFbPage();
}
