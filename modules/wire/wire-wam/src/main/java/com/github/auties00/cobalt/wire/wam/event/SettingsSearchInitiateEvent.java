package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SettingsPageType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSettingsSearchInitiateWamEvent")
@WamEvent(id = 4014)
public interface SettingsSearchInitiateEvent extends WamEventSpec {
    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<SettingsPageType> settingsPageType();
}
