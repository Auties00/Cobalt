package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.FmxEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.FmxEventType;
import com.github.auties00.cobalt.wire.wam.type.HighlightGroupType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPsFmxActionWamEvent")
@WamEvent(id = 7054, channel = WamChannel.PRIVATE, privateStatsId = 113760892)
public interface PsFmxActionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong commonGroupNum();

    @WamProperty(index = 2, type = WamType.BOOLEAN)
    Optional<Boolean> countryShown();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<FmxEntryPoint> fmxEntryPoint();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<FmxEventType> fmxEvent();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<HighlightGroupType> highlightGroupType();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> isSenderSmb();

    @WamProperty(index = 8, type = WamType.BOOLEAN)
    Optional<Boolean> isSuspiciousFmx();

    @WamProperty(index = 9, type = WamType.BOOLEAN)
    Optional<Boolean> newAccountShown();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> notAContactShown();
}
