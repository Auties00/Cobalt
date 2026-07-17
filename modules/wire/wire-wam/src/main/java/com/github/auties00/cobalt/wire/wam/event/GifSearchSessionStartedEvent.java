package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.GifSearchProvider;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebGifSearchSessionStartedWamEvent")
@WamEvent(id = 1134)
public interface GifSearchSessionStartedEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<GifSearchProvider> gifSearchProvider();
}
