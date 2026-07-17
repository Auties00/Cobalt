package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebTsBitArrayWamEvent")
@WamEvent(id = 4332)
public interface TsBitArrayEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong bitarrayHigh();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong bitarrayLength();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong bitarrayLow();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong cumulativeBits();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong relativeTimestampMs();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong sessionSeq();

    @WamProperty(index = 7, type = WamType.INTEGER)
    OptionalLong tsSessionId();

    @WamProperty(index = 8, type = WamType.INTEGER)
    OptionalLong tsTimestampMs();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
