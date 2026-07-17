package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.PrivacyHighlightCategoryEnum;
import com.github.auties00.cobalt.wire.wam.type.PrivacyHighlightSurfaceEnum;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebPrivacyHighlightDailyWamEvent")
@WamEvent(id = 3522)
public interface PrivacyHighlightDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong dialogAppearCount();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong dialogSelectCount();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong narrativeAppearCount();

    @WamProperty(index = 4, type = WamType.ENUM)
    Optional<PrivacyHighlightCategoryEnum> privacyHighlightCategory();

    @WamProperty(index = 5, type = WamType.ENUM)
    Optional<PrivacyHighlightSurfaceEnum> privacyHighlightSurface();
}
