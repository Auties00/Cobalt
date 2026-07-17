package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.AboutEntrypointType;
import com.github.auties00.cobalt.wire.wam.type.AboutPromptType;
import com.github.auties00.cobalt.wire.wam.type.AboutRequestType;
import com.github.auties00.cobalt.wire.wam.type.PresetType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebAboutCreationWamEvent")
@WamEvent(id = 6818)
public interface AboutCreationEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.INTEGER)
    OptionalLong aboutDuration();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<AboutEntrypointType> aboutEntrypoint();

    @WamProperty(index = 3, type = WamType.INTEGER)
    OptionalLong aboutLength();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> aboutLocale();

    @WamProperty(index = 5, type = WamType.TIMER)
    Optional<Instant> aboutOverallT();

    @WamProperty(index = 6, type = WamType.BOOLEAN)
    Optional<Boolean> aboutPresetSelected();

    @WamProperty(index = 9, type = WamType.ENUM)
    Optional<AboutPromptType> aboutPrompt();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<AboutRequestType> aboutRequestType();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<PresetType> preset();
}
