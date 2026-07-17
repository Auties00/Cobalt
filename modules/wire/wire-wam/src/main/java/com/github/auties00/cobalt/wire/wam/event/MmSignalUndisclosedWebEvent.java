package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.MmSignalType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMmSignalUndisclosedWebWamEvent")
@WamEvent(id = 7862, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface MmSignalUndisclosedWebEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong mmCarouselCardIndex();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong mmCtaButtonIndex();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> mmSignalData();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<MmSignalType> mmSignalType();
}
