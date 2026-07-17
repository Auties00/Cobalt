package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.WebcWhatsNewActionType;
import com.github.auties00.cobalt.wire.wam.type.WebcWhatsNewSurfaceType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebWebcWhatsNewImpressionWamEvent")
@WamEvent(id = 8200)
public interface WebcWhatsNewImpressionEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<WebcWhatsNewActionType> webcWhatsNewAction();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<WebcWhatsNewSurfaceType> webcWhatsNewSurface();

    @WamProperty(index = 3, type = WamType.TIMER)
    Optional<Instant> webcWhatsNewTimeSpent();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong webcWhatsNewVariant();
}
