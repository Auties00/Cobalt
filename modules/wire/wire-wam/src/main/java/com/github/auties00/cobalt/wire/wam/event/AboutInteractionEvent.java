package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AboutConsumptionSurfaceType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebAboutInteractionWamEvent")
@WamEvent(id = 7084)
public interface AboutInteractionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<AboutConsumptionSurfaceType> aboutConsumptionSurface();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> aboutLocale();
}
