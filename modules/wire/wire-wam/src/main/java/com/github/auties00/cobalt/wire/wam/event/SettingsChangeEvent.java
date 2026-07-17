package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.SettingType;

import java.util.Optional;

@WhatsAppWebModule(moduleName = "WAWebSettingsChangeWamEvent")
@WamEvent(id = 6396)
public interface SettingsChangeEvent extends WamEventSpec {
    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> currentSettingValue();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> previousSettingValue();

    @WamProperty(index = 3, type = WamType.ENUM)
    Optional<SettingType> settingType();
}
