package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.TsExternalEventSource;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebTsExternalWamEvent")
@WamEvent(id = 4574)
public interface TsExternalEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong relativeTimestampMs();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong tsDuration();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<TsExternalEventSource> tsExternalEventSource();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong tsSessionId();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong tsTimestampMs();

    @WamProperty(index = 6, type = WamType.STRING)
    Optional<String> unifiedSessionId();
}
