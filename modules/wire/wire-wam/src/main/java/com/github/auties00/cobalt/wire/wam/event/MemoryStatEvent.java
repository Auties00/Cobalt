package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebMemoryStatWamEvent")
@WamEvent(id = 1336)
public interface MemoryStatEvent extends WamEventSpec {
    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> appContext();

    @WamProperty(index = 15, type = WamType.INTEGER)
    OptionalLong appContextBitfield();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> hasVerifiedNumber();

    @WamProperty(index = 8, type = WamType.FLOAT)
    OptionalDouble numMessages();

    @WamProperty(index = 3, type = WamType.FLOAT)
    OptionalDouble privateBytes();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> processType();

    @WamProperty(index = 4, type = WamType.FLOAT)
    OptionalDouble sharedBytes();

    @WamProperty(index = 6, type = WamType.FLOAT)
    OptionalDouble uptime();

    @WamProperty(index = 2, type = WamType.FLOAT)
    OptionalDouble workingSetPeakSize();

    @WamProperty(index = 1, type = WamType.FLOAT)
    OptionalDouble workingSetSize();
}
