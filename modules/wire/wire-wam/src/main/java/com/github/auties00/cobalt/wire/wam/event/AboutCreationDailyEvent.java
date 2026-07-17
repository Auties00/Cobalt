package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AboutEntrypointType;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAboutCreationDailyWamEvent")
@WamEvent(id = 6820)
public interface AboutCreationDailyEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong aboutCreationStarted();

    @WamProperty(index = 2, type = WamType.INTEGER)
    OptionalLong aboutCreationVisit();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<AboutEntrypointType> aboutEntrypoint();

    @WamProperty(index = 4, type = WamType.INTEGER)
    OptionalLong aboutFailureCount();

    @WamProperty(index = 5, type = WamType.STRING)
    Optional<String> aboutLocale();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong aboutSuccessCount();
}
