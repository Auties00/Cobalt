package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AfterReadScreenEntryPointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebArInterstitialScreenImpressionWamEvent")
@WamEvent(id = 7890)
public interface ArInterstitialScreenImpressionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong accountAfterReadDuration();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<AfterReadScreenEntryPointType> afterReadScreenEntryPoint();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong nuxVersion();
}
